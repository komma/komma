NEWVERSION=$1

if [ -z "$NEWVERSION" ]; then exit 1; fi

VERSION_OPT=-DreleaseVersion=$NEWVERSION
if [ "$NEWVERSION" == *-SNAPSHOT ]; then
  VERSION_OPT=-DdevelopmentVersion=$NEWVERSION
fi
echo $VERSION_OPT

# Update versions with Tycho (MANIFEST.MF, plugin.xml, feature.xml, ...)
MODULES=$(grep -l -r --include 'pom.xml' -e '<packaging>eclipse-plugin</packaging>' | sed "s/^\(.*\)\/pom\.xml/<module>\1<\/module>/" | paste -sd " ")
sed -e "s+<modules></modules>+<modules>$MODULES</modules>+" releng/updateVersion.pom.xml > updateVersion.pom.xml.tmp

ARTIFACTS=$(grep -l -r --include 'pom.xml' -e '<packaging>eclipse-plugin</packaging>' | xargs cat | grep "<artifactId>" | sed "s/^.*<artifactId>\(.*\)<.*$/\1/" | sort -u | grep -e ^net.enilink -e ^komma | grep -e komma-parent -v | paste -sd ",")
echo "Updating Maven and OSGi descriptors for: " ${ARTIFACTS}

#mvn -Dtycho.mode=maven -f updateVersion.pom.xml.tmp org.eclipse.tycho:tycho-versions-plugin:0.23.0:set-version -DnewVersion=$NEWVERSION -Dartifacts=${ARTIFACTS}

# Update versions with Maven - parent projects are not updated
sed -e "s+<modules></modules>+<modules><module>releng/komma-updatesite</module></modules>+" releng/updateVersion.pom.xml > updateVersion.pom.xml.tmp
mvn -Dtycho.mode=maven -f updateVersion.pom.xml.tmp org.codehaus.mojo:versions-maven-plugin:2.2:set -DnewVersion=$NEWVERSION

# delete temporary aggregator project
rm updateVersion.pom.xml.tmp

# Undo changes in libraries since these modules have fixed version numbers
git -C libraries reset --hard

for f in $(find libraries -name 'pom.xml')
do
  sed -i -e 's+-SNAPSHOT</version>+</version>+' "$f"
done
