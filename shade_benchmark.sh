#!/bin/sh

APP=quarkus-jmh
VERSION=1.0.0-SNAPSHOT
RUNNER_JAR=${APP}-${VERSION}-runner.jar
JAR=${APP}-${VERSION}.jar

cd ./target/
rm -rf scratch
mkdir scratch
cp ${RUNNER_JAR}  ./scratch/
cd scratch/
jar xvf ${RUNNER_JAR}
rm ${RUNNER_JAR}
rm META-INF/MANIFEST.MF 
jar uf ../${JAR} ./*
cd ..
