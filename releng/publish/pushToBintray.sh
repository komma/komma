#!/bin/bash
#Sample Usage: pushToBintray.sh pathToP2Repo username apikey owner repo package version
API=https://api.bintray.com

function publish_p2_repository() {
if [ ! -z "$1" ]; then REPOSITORY_PATH=$1; fi
if [ ! -z "$2" ]; then BINTRAY_USER=$2; fi
if [ ! -z "$3" ]; then BINTRAY_API_KEY=$3; fi
if [ ! -z "$4" ]; then BINTRAY_OWNER=$4; fi
if [ ! -z "$5" ]; then BINTRAY_REPO=$5; fi
if [ ! -z "$6" ]; then BINTRAY_PCK_NAME=$6; fi
if [ ! -z "$7" ]; then BINTRAY_PCK_VERSION=$7; fi

echo "REPOSITORY_PATH=$REPOSITORY_PATH"
echo "BINTRAY_USER=$BINTRAY_USER"
echo "BINTRAY_OWNER=$BINTRAY_OWNER"
echo "BINTRAY_REPO=$BINTRAY_REPO"
echo "BINTRAY_PCK_NAME=$BINTRAY_PCK_NAME"
echo "BINTRAY_PCK_VERSION=$BINTRAY_PCK_VERSION"

if [ ! -z "$REPOSITORY_PATH" ]; then
  pushd "$REPOSITORY_PATH"
else
  pushd .
fi

FILES=*
BINARYDIR=binary/*
PLUGINDIR=plugins/*
FEATUREDIR=features/*

BINTRAY_PATH="$BINTRAY_OWNER/$BINTRAY_REPO/$BINTRAY_PCK_VERSION"
BINTRAY_OPTS="bt_package=$BINTRAY_PCK_NAME;bt_version=$BINTRAY_PCK_VERSION;publish=0"

curl -X POST -u ${BINTRAY_USER}:${BINTRAY_API_KEY} "${API}/packages/$BINTRAY_OWNER/$BINTRAY_REPO/$BINTRAY_PCK_NAME/versions" -d "{ \"name\": \"$BINTRAY_PCK_VERSION\" }" -H "Content-Type: application/json"

for f in $FILES
do
if [ ! -d $f ]; then
  if [[ "$f" == *content.jar ]] || [[ "$f" == *artifacts.jar ]] 
  then
    echo "Processing p2 metadata file: $f"
    curl -X PUT -T $f -u ${BINTRAY_USER}:${BINTRAY_API_KEY} "${API}/content/$BINTRAY_PATH/$f;$BINTRAY_OPTS"
  fi
  echo ""
fi
done

echo "Processing features dir $FEATUREDIR ..."	
for f in $FEATUREDIR
do
  echo "Processing feature file: $f"
  curl -X PUT -T $f -u ${BINTRAY_USER}:${BINTRAY_API_KEY} "${API}/content/$BINTRAY_PATH/$f;$BINTRAY_OPTS"
  echo ""
done

echo "Processing plugin dir $PLUGINDIR ..."

for f in $PLUGINDIR
do
  # take action on each file. $f store current file name
  echo "Processing plugin file: $f"
  curl -X PUT -T $f -u ${BINTRAY_USER}:${BINTRAY_API_KEY} "${API}/content/$BINTRAY_PATH/$f;$BINTRAY_OPTS"
  echo ""
done

if [ -d $BINARYDIR ]
then
  echo "Processing binary dir $BINARYDIR ..."
  for f in $BINARYDIR
  do
    # take action on each file. $f store current file name
    echo "Processing binary file: $f"
    curl -X PUT -T $f -u ${BINTRAY_USER}:${BINTRAY_API_KEY} "${API}/content/$BINTRAY_PATH/$f;$BINTRAY_OPTS"
    echo ""
  done
fi

echo "Publishing the new version"
curl -X POST -u ${BINTRAY_USER}:${BINTRAY_API_KEY} "${API}/content/$BINTRAY_OWNER/$BINTRAY_REPO/$BINTRAY_PCK_NAME/$BINTRAY_PCK_VERSION/publish" -d "{ \"discard\": \"false\" }" -H "Content-Type: application/json"

popd
}

publish_p2_repository "$@"
