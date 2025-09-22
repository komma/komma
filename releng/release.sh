#!/bin/bash
PUSHCHANGES="false"
while [[ $# > 1 ]]
do
key="$1"

case $key in
    -r|--releaseVersion)
    RELEASEVERSION="$2"
    shift # past argument
    ;;
    -d|--developmentVersion)
    DEVELOPMENTVERSION="$2"
    shift # past argument
    ;;
    -p|--pushChanges)
    PUSHCHANGES="true"
    ;;
    *)
    # unknown option
    ;;
esac
shift # past argument or value
done

mvn versions:set -DnewVersion=$RELEASEVERSION -DgroupId='*' -DartifactId='*' -DgenerateBackupPoms=false -P versions

git commit -a -m "prepare release $RELEASEVERSION"
git tag -a v$RELEASEVERSION -m "KOMMA release $RELEASEVERSION"

mvn versions:set -DnewVersion=$DEVELOPMENTVERSION -DgroupId='*' -DartifactId='*' -DgenerateBackupPoms=false -P versions

git commit -a -m "prepare for next development iteration"
