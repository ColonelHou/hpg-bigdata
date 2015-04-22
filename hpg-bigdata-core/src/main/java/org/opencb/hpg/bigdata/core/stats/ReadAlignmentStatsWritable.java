package org.opencb.hpg.bigdata.core.stats;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;

import org.apache.hadoop.io.Writable;

public class ReadAlignmentStatsWritable extends ReadAlignmentStats implements Writable {
	
	public ReadAlignmentStatsWritable()  {
		super();
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(numMapped);
		out.writeInt(numUnmapped);
		out.writeInt(numPaired);
		out.writeInt(numMappedFirst);
		out.writeInt(numMappedSecond);
		
		out.writeInt(NM);

		out.writeInt(numHardC);
		out.writeInt(numSoftC);
		out.writeInt(numIn);
		out.writeInt(numDel);
		out.writeInt(numPad);
		out.writeInt(numSkip);

		out.writeInt(accMappingQuality);
		out.writeInt(mappingQualityMap.size());
		for(int key:mappingQualityMap.keySet()) {
			out.writeInt(key);
			out.writeInt(mappingQualityMap.get(key));		
		}
		
		out.writeInt(accInsert);
		out.writeInt(insertMap.size());
		for(int key:insertMap.keySet()) {
			out.writeInt(key);
			out.writeInt(insertMap.get(key));		
		}

		ReadStatsWritable aux = new ReadStatsWritable(readStats);
		aux.write(out);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		int size;
		
		numMapped = in.readInt();
		numUnmapped = in.readInt();
		numPaired = in.readInt();
		numMappedFirst = in.readInt();
		numMappedSecond = in.readInt();

		NM = in.readInt();

		numHardC = in.readInt();
		numSoftC = in.readInt();
		numIn = in.readInt();
		numDel = in.readInt();
		numPad = in.readInt();
		numSkip = in.readInt();

		accMappingQuality =  in.readInt();
		size = in.readInt();
		mappingQualityMap = new HashMap<Integer, Integer>(size);
		for (int i = 0; i < size; i++) {
			mappingQualityMap.put(in.readInt(), in.readInt());
		}
		
		accInsert =  in.readInt();
		size = in.readInt();
		insertMap = new HashMap<Integer, Integer>(size);
		for (int i = 0; i < size; i++) {
			insertMap.put(in.readInt(), in.readInt());
		}

		ReadStatsWritable aux = new ReadStatsWritable();
		aux.readFields(in);
		readStats.set(aux);
	}
}
