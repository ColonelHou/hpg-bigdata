#!/bin/bash

#gcc -O3 -std=gnu99 ./converters/bam2ga.c jni/org_opencb_hpg_bigdata_core_NativeSupport.c -o $olib -shared -fPIC -I third-party/avro-c-1.7.7/src/ -I $JAVA_HOME/include -I $JAVA_HOME/include/linux -I $JAVA_HOME/include/darwin -I third-party/ -I third-party/htslib/ -L third-party/avro-c-1.7.7/build/src/ -L third-party/htslib/ -lhts -lavro -lpthread
gcc -O3 -std=gnu99 hpg-aligner.c index.c mapper.c -o hpg-aligner

olib="libhpgaligner.so"
gcc -O3 -std=gnu99 hpg-aligner.c index.c mapper.c -o $olib -shared -fPIC
