#! /bin/bash

echo "Building CodewebsIndexDaemon"

DEST=../bin/
LIB=../../../ext/commons-daemon-1.0.15/commons-daemon-1.0.15.jar:../../../ext/rabbitmq/rabbitmq-client.jar:../bin:../../../ext/commons-math3-3.2/commons-math3-3.2.jar
#SRC="../src/daemons/CodewebsIndexDaemon.java ../src/util/FileSystem.java ../src/org/json/*.java"
SRC="../src"
find ../src -name *.java > sourceslist.txt
MANIFEST=../src/daemons/codewebsindexdaemon.mf
#javac -d $DEST -classpath $LIB $SRC
javac -d $DEST -classpath $LIB @sourceslist.txt

cd ../jar
if [ -d "tmp" ]; then
    rm -rf tmp
fi
mkdir tmp

cp -rf ../bin/daemons tmp
cp -rf ../bin/models tmp
cp -rf ../bin/util tmp
cp -rf ../bin/org tmp
cp -rf ../bin/minions tmp
cd tmp
jar -cmf ../../src/daemons/codewebsindexdaemon.mf ../CodewebsIndexDaemon.jar *


