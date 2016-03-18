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

mvn -Dtycho.mode=maven tycho-versions:set-version -DnewVersion=$RELEASEVERSION

git commit -a -m "prepare release $RELEASEVERSION"
git tag v$RELEASEVERSION

mvn -Dtycho.mode=maven tycho-versions:set-version -DnewVersion=$DEVELOPMENTVERSION

git commit -a -m "prepare for next development iteration"
