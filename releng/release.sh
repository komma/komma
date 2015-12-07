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

mvn -Dtycho.mode=maven org.eclipse.tycho:tycho-versions-plugin:0.23.0:set-version -DnewVersion=$RELEASEVERSION

git commit -a -m "prepare release $RELEASEVERSION"
git tag v$RELEASEVERSION
pushd libraries
git commit -a -m "prepare release $RELEASEVERSION"
git tag v$RELEASEVERSION
popd

mvn -Dtycho.mode=maven org.eclipse.tycho:tycho-versions-plugin:0.23.0:set-version -DnewVersion=$DEVELOPMENTVERSION

git commit -a -m "prepare for next development iteration"
pushd libraries
git commit -a -m "prepare for next development iteration"
popd
