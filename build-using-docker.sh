#!/bin/bash
if [ $# -eq 0 ]
then
   echo "usage: ./build-using-docker.sh <java version>"
   echo "e.g. ./build-using-docker.sh 8 "
   echo "e.g. ./build-using-docker.sh 11 "
   echo "e.g. ./build-using-docker.sh 14 "
   exit;
fi
mydate=`date -u +"%Y-%m-%dT%H:%M:%S.000Z"`

version="${1}"

if [ $# -eq 2 ]
then
    skiptests=${2}
else
    skiptests=true
fi

docker run -it --rm --name my-maven-project -v "$PWD":/usr/src/app   -v "$HOME"/.m2:/root/.m2 -w /usr/src/app maven:3.6-jdk-${version} mvn clean install -DskipTests=${skiptests}
