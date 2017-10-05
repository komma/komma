#!/bin/bash
# Usage: pushToBintray.sh pathToP2Repo username apikey owner repo package version
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

TAG=$(git describe --tags --exact-match 2>/dev/null)
TAG=${TAG#v}

if [ ! -z "$REPOSITORY_PATH" ]; then
  pushd "$REPOSITORY_PATH"
else
  pushd .
fi

if [ ! -z "$TAG" ]; then
# use current tag as version
  BINTRAY_PCK_VERSION=$TAG
elif [ -e "version.txt" ]; then
# read version from file
  BINTRAY_PCK_VERSION=$(<version.txt)
fi

FILES=(content.jar artifacts.jar plugins/* features/* binary/*)

BINTRAY_PATH="$BINTRAY_OWNER/$BINTRAY_REPO/$BINTRAY_PCK_NAME/$BINTRAY_PCK_VERSION"
BINTRAY_OPTS="bt_package=$BINTRAY_PCK_NAME;bt_version=$BINTRAY_PCK_VERSION;publish=0"

# create version
curl -X POST -u ${BINTRAY_USER}:${BINTRAY_API_KEY} "${API}/packages/$BINTRAY_OWNER/$BINTRAY_REPO/$BINTRAY_PCK_NAME/versions" -d "{ \"name\": \"$BINTRAY_PCK_VERSION\" }" -H "Content-Type: application/json"

# upload files
for f in ${FILES[@]}
do
if [ -f $f ]; then
  echo "Processing file: $f"
  curl -X PUT -T $f --retry 3 -u ${BINTRAY_USER}:${BINTRAY_API_KEY} "${API}/content/$BINTRAY_PATH/$f;$BINTRAY_OPTS"
fi
done

echo "Publishing the new version"
curl -X POST --retry 3 -u ${BINTRAY_USER}:${BINTRAY_API_KEY} "${API}/content/$BINTRAY_OWNER/$BINTRAY_REPO/$BINTRAY_PCK_NAME/$BINTRAY_PCK_VERSION/publish" -d "{ \"discard\": \"false\" }" -H "Content-Type: application/json"

popd
}

publish_p2_repository "$@"
