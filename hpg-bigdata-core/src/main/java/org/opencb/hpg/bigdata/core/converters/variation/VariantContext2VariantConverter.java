/*
 * Copyright 2015 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 *
 */
package org.opencb.hpg.bigdata.core.converters.variation;

import htsjdk.tribble.AbstractFeatureReader;
import htsjdk.tribble.FeatureReader;
import htsjdk.tribble.TribbleException;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.*;
import htsjdk.variant.vcf.VCFConstants;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLine;
import org.apache.avro.file.CodecFactory;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.ga4gh.models.*;
import org.opencb.hpg.bigdata.core.converters.Converter;
import org.opencb.hpg.bigdata.core.converters.FullVcfCodec;
import org.opencb.hpg.bigdata.core.io.avro.AvroWriter;
import org.opencb.hpg.bigdata.core.utils.AvroUtils;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Chr20 of 1000 genomes -> 855,196 rows  = 30 header, 855,166 rows.
 *
 * avro.snappy:  3GB ->  12.0 GB  (58 min)
 * avro.deflate: 3GB ->   7.2 GB  (82 min)
 *
 *
 * @author mh719
 *
 */
public class VariantContext2VariantConverter implements Converter<VariantContext, Variant> {

    private final AtomicReference<VariantConverterContext> ctx = new AtomicReference<>();
    public static final String VCF_FILTER_COLUMN = "FILTER";
    public static final String VCF_QUAL_COLUMN = "QUAL";
    private String variantSetId = "";

    public static void main(String[] args) {
        convert(args[0], args[1]);
    }

    public static void convert(String input, String output) {
        StopWatch sw = new StopWatch();
        sw.start();
        File file = new File(input);

        long cnt = 0;

        File outFile = new File(output);
        try (OutputStream os = new FileOutputStream(outFile);
             OutputStream callOs = new FileOutputStream(new File(output + ".Call"));
             OutputStream vsOs = new FileOutputStream(new File(output + ".variantSet"))) {

            CodecFactory codec = AvroUtils.getCodec("deflate");
//          CompressionUtils.getCodec("snappy");
            AvroWriter<Variant> writer = new AvroWriter<>(Variant.getClassSchema(), codec, os);
            AvroWriter<CallSet> callWriter = new AvroWriter<>(CallSet.getClassSchema(), codec, callOs);
            AvroWriter<VariantSet> vsWriter = new AvroWriter<>(VariantSet.getClassSchema(), codec, vsOs);

            Converter<String, CallSet> gtConverter = new Genotype2CallSet();
            Converter<VCFHeaderLine, VariantSetMetadata> infoConverter =
                    new VcfHeaderLine2VariantSetMetadataConverter();
            VariantContext2VariantConverter varConverter = new VariantContext2VariantConverter();
            try (FeatureReader<VariantContext> freader =
                         AbstractFeatureReader.getFeatureReader(file.getAbsolutePath(), new FullVcfCodec(), false)) {
                VCFHeader header = (VCFHeader) freader.getHeader();

                int gtSize = header.getGenotypeSamples().size();
                VariantConverterContext ctx = new VariantConverterContext();

                VariantSet vs = new VariantSet();
                vs.setId(file.getName());
                vs.setDatasetId(file.getName());
                vs.setReferenceSetId("test");

                List<String> genotypeSamples = header.getGenotypeSamples();
                for (int gtPos = 0; gtPos < gtSize; ++gtPos) {
                    CallSet cs = gtConverter.forward(genotypeSamples.get(gtPos));
                    cs.getVariantSetIds().add(vs.getId());
                    ctx.getCallSetMap().put(cs.getName(), cs);
                    callWriter.write(cs);
                }

                vs.setMetadata(new LinkedList<>());
                convertHeaders(infoConverter, header, vs);

                vsWriter.write(vs);
                varConverter.setContext(ctx);
                for (VariantContext c : freader.iterator()) {
                    if (cnt % 1000 == 0) {
                        System.err.println(String.format("Processing variant %s ...", cnt++));
                    }
                    cnt++;
                    Variant v = varConverter.forward(c);
                    writer.write(v);
                }
            } catch (TribbleException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            writer.close();
            vsWriter.close();
            callWriter.close();
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        sw.stop();
        System.err.println(sw.toString());
    }

    /**
     * Converts INFO, FORMAT, FILTER.
     * @param headerConverter
     * @param header
     * @param vs
     */
    private static void convertHeaders(Converter<VCFHeaderLine, VariantSetMetadata> headerConverter,
                                       VCFHeader header, VariantSet vs) {
        Collection[] coll = new Collection[] {
                header.getInfoHeaderLines(),
                header.getFilterLines(),
                header.getFormatHeaderLines(),
//              header.getContigLines(),
                // TODO other formats
        };
        for (Collection<? extends VCFHeaderLine> c : coll) {
            if (null != c) {
                for (VCFHeaderLine hl : c) {
                    vs.getMetadata().add(headerConverter.forward(hl));
                }
            }
        }
    }

    public static void print(Variant v) {
        System.out.println(String.format("%s:%s-%s %s %s %s", v.getReferenceName(), v.getStart(), v.getEnd(),
                v.getId(), v.getReferenceBases(), v.getAlternateBases()));
    }

    @Deprecated
    public VariantConverterContext getContext() {
        return ctx.get();
    }

    @Deprecated
    public void setContext(VariantConverterContext ctx) {
        this.ctx.set(ctx);
    }

    public String getVariantSetId() {
        return variantSetId;
    }

    public VariantContext2VariantConverter setVariantSetId(String variantSetId) {
        this.variantSetId = variantSetId;
        return this;
    }

    /*
     *
     * VariantSetMetadata represents VCF header information.
     *
     * `Variant` and `CallSet` both belong to a `VariantSet`.
     * `VariantSet` belongs to a `Dataset`.
     * The variant set is equivalent to a VCF file.
     *
     * A `CallSet` is a collection of variant calls for a particular sample. It belongs to a `VariantSet`.
     * This is equivalent to one column in VCF.
     * A `Call` represents the determination of genotype with respect to a particular `Variant`.
     * An `AlleleCall` represents the determination of the copy number of a particular `Allele`,
     * possibly within a certain `Variant`.
     * A `Variant` represents a change in DNA sequence relative to some reference. For example,
     * a variant could represent a SNP or an insertion.
     * Variants belong to a `VariantSet`. This is equivalent to a row in VCF.
     *
     * SUMMARY:
     *  VCF File -> VariantSet
     *  Header   -> VariantSetMetadata (separate converter)
     *  Row -> Variant
     *
     * Whole Column (sample) -> CallSet
     * One Column entry(sample) -> Call
     *
     */

    @Override
    public Variant forward(VariantContext variantContext) {
        Variant gaVariant = new Variant();

        gaVariant.setVariantSetId(variantSetId);

        gaVariant.setId(variantContext.getContig()
                + ":" + translateStartPosition(variantContext)
                + ":" + variantContext.getReference().getBaseString()
                + ":" + StringUtils.join(variantContext.getAlternateAlleles(), ","));
        // Assumed to be related to the avro record
        long currTime = System.currentTimeMillis();
        gaVariant.setCreated(currTime);
        gaVariant.setUpdated(currTime);

        /* VCF spec (1-Based):
         *  -> POS - position: The reference position, with the 1st base having position 1.
         *  -> TODO: Telomeres are indicated by using positions 0 or N+1
         * GA4GH spec
         *  -> (0-Based)
         */
        gaVariant.setReferenceName(variantContext.getContig());
        gaVariant.setStart(translateStartPosition(variantContext));

        /* 0-based, exclusive end position [start,end) */
        /* 1-based, inclusive end position [start,end] */
        gaVariant.setEnd((long) variantContext.getEnd());
        List<String> nameList = Collections.emptyList();

        if (StringUtils.isNotBlank(variantContext.getID())) {
            nameList = Arrays.asList(variantContext.getID().split(VCFConstants.ID_FIELD_SEPARATOR));
        }

        gaVariant.setNames(nameList);

        /* Classic mode */
        // Reference
        Allele refAllele = variantContext.getReference();
        gaVariant.setReferenceBases(refAllele.getBaseString());

        assert Long.compare(gaVariant.getEnd() - gaVariant.getStart(), gaVariant.getReferenceBases().length()) == 0;

        // Alt
        List<Allele> altAllele = variantContext.getAlternateAlleles();
        List<String> altList = new ArrayList<>(altAllele.size());
        for (Allele allele : altAllele) {
            altList.add(allele.getBaseString());
        }
        gaVariant.setAlternateBases(altList);


        /* Graph mode */
        // NOT supported
        // gaVariant.setAlleleIds(value);

        // INFO
        Map<String, List<String>> info = convertInfo(variantContext);
        gaVariant.setInfo(info);

        // QUAL
        info.put(VCF_QUAL_COLUMN, Collections.singletonList(Double.toString(variantContext.getPhredScaledQual())));

        //  FILTER
        Set<String> filter = variantContext.getFilters();
        if (filter.isEmpty()) {
            info.put(VCF_FILTER_COLUMN, Collections.singletonList(VCFConstants.PASSES_FILTERS_v4));
        } else {
            info.put(VCF_FILTER_COLUMN, new ArrayList<>(filter));
        }

        // Genotypes (Call's)

        gaVariant.setCalls(convertCalls(variantContext));

        return gaVariant;
    }

    /**
     * Convert HTS Genotypes to GA4GH {@link Call}.
     *
     * @param context {@link VariantContext}
     * @return {@link List} of {@link Call}
     */
    @SuppressWarnings("deprecation")
    private List<Call> convertCalls(VariantContext context) {
        GenotypesContext gtc = context.getGenotypes();

        // Create Allele mapping
        Map<String, Integer> alleleMap = new HashMap<>();
        int i = 0;
        alleleMap.put(context.getReference().getBaseString(), i++);
        for (Allele a : context.getAlternateAlleles()) {
            alleleMap.put(a.getBaseString(), i++);
        }

        int size = gtc.size();
        List<Call> callList = new ArrayList<>(size);

        for (Genotype gt : gtc) {
            String name = gt.getSampleName();
//            CallSet cs = getContext().getCallSetMap().get(name);
            Call c = new Call();

            c.setCallSetId(name);
//            c.setCallSetId(cs.getId());
//            c.setCallSetName(cs.getName());  // TODO is this really needed 2x
            c.setCallSetName(StringUtils.EMPTY);

            List<Integer> cgt = new ArrayList<>(gt.getPloidy());
            boolean unknownGT = false;
            // TODO issue with ./. -> propose ignore call
            for (Allele a : gt.getAlleles()) {
                String baseString = a.getBaseString();
                Integer e = alleleMap.get(baseString);
                if (null == e) {
                    unknownGT = true;
                    break;
                }
                cgt.add(e);
            }
            if (unknownGT) {
                continue; // ignore Call -> Genotype not known (e.g. "./.")
            }
            c.setGenotype(cgt);
            c.setPhaseset(Boolean.valueOf(gt.isPhased()).toString()); // TODO decide what's the correct string

            Map<String, List<String>> infoMap = new HashMap<>();
            if (gt.hasAD()) {
                int[] ad = gt.getAD();
                List<String> adl = new ArrayList<>(ad.length);
                for (int val : ad) {
                    adl.add(Integer.toString(val));
                }
                infoMap.put(VCFConstants.GENOTYPE_ALLELE_DEPTHS, adl);
            }

            if (gt.hasDP()) {
                infoMap.put(VCFConstants.DEPTH_KEY, Collections.singletonList(Integer.toString(gt.getDP())));
            }

            if (gt.hasGQ()) {
                infoMap.put(VCFConstants.GENOTYPE_QUALITY_KEY,
                        Collections.singletonList(Integer.toString(gt.getGQ())));
            }

            // Add Filter information to the genotype data
//            infoMap.put(VFC_FILTER_COLUMN, filters);

            Map<String, Object> extAttr = gt.getExtendedAttributes();
            List<Double> lhList = Collections.emptyList();
            if (extAttr != null) {
                for (Entry<String, Object> extEntry : extAttr.entrySet()) {
                    Object value = extEntry.getValue();
                    // requires FullVCFCodec implementation to add GL as extra field instead of converting
                    if (StringUtils.equals(extEntry.getKey(), VCFConstants.GENOTYPE_LIKELIHOODS_KEY)) {
                        if (null != value) {
                            GenotypeLikelihoods glField = GenotypeLikelihoods.fromGLField((String) value);
                            double[] glv = glField.getAsVector();
                            lhList = new ArrayList<>(glv.length);
                            for (double d : glv) {
                                lhList.add(d);
                            }
                        }
                    } else {
                        if (value instanceof List) {
                            // TODO Check if this works
                            List vlist = (List) value;
                            List<String> lst = new ArrayList<>(vlist.size());
                            for (Object o : vlist) {
                                lst.add(o.toString());
                            }
                            infoMap.put(extEntry.getKey(), lst);
                        } else {
                            infoMap.put(extEntry.getKey(), Collections.singletonList(value.toString()));
                        }
                    }
                }
            }
            c.setGenotypeLikelihood(lhList);

            c.setInfo(infoMap);
            callList.add(c);
        }
        return callList;
    }

    /**
     * Convert into from HTS to GA4GH.
     * @param c {@link VariantContext}
     * @return Map of info information
     */
    private Map<String, List<String>> convertInfo(VariantContext c) {
        Map<String, List<String>> infoMap = new HashMap<>();
        Map<String, Object> attributes = c.getAttributes();
        for (Entry<String, Object> a : attributes.entrySet()) {
            String key = a.getKey();
            Object value = a.getValue();
            final List<String> valList;
            if (value == null) {
                // don't do anything
                valList = Collections.emptyList();
            } else if (value instanceof List) {
                List l = (List) value;
                valList = new ArrayList<>(l.size());
                for (Object o : l) {
                    valList.add(o.toString());
                }
            } else {
                String o = value.toString();
                valList = Arrays.asList(o);
            }
            infoMap.put(key, valList);
        }
        return infoMap;
    }

    /**
     * Translate from 1-based to 0-based.
     * @param c VariantContext object
     * @return 0-based start position
     */
    private Long translateStartPosition(VariantContext c) {
        int val = c.getStart();
        if (val == 0) {
            return 0L; // TODO: test if this is correct for Telomeres
        }
        return Long.valueOf(val - 1);
    }

    @Override
    public VariantContext backward(Variant obj) {
        throw new NotImplementedException("");
    }

}
