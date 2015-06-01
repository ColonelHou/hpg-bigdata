#!/bin/bash

PLATFORM=`uname -s`

echo
echo "Building library avro-c-1.7.7"
cd third-party/avro-c-1.7.7/
if [ ! -d "build" ]; then
  mkdir build
fi
cd build
cmake .. -DCMAKE_BUILD_TYPE=RelWithDebInfo
make

cd ../../..
echo
echo "Building library htslib"
cd third-party/htslib
make

cd ../..
echo
echo "Building library samtools"
cd third-party/samtools
make HTSDIR=../htslib

cd ../..

olib="libhpgbigdata.so"
if [[ "Darwin" == "$PLATFORM" ]]; then
  olib="libhpgbigdata.dylib"
fi

echo 
echo "Building the dynamic library $olib"

gcc -O3 -std=gnu99 ./converters/bam2ga.c jni/org_opencb_hpg_bigdata_core_NativeConverter.c -o $olib -shared -fPIC -I third-party/avro-c-1.7.7/src/ -I jni/ -I $JAVA_HOME/include -I $JAVA_HOME/include/linux -I $JAVA_HOME/include/darwin -I third-party/ -I third-party/htslib/ -L third-party/avro-c-1.7.7/build/src/ -L third-party/htslib/ -lhts -lavro -lpthread

olib="libhpgaligner.so"
if [[ "Darwin" == "$PLATFORM" ]]; then
  olib="libhpgaligner.dylib"
fi

echo
echo "Building the dynamic library $olib"

gcc -O3 -std=gnu99 jni/org_opencb_hpg_bigdata_core_NativeAligner.c ./third-party/hpg-aligner/index.c ./third-party/hpg-aligner/mapper.c -o $olib -shared -fPIC -I jni/ -I third-party/hpg-aligner -I $JAVA_HOME/include -I $JAVA_HOME/include/linux -I $JAVA_HOME/include/darwin
