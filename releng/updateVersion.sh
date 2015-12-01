NEWVERSION=$1

for f in $(find libraries -name 'pom.xml')
do
  sed -i -e 's+-SNAPSHOT</version>+</version>+' "$f"
done

if [ -z "$NEWVERSION" ]; then exit 1; fi

# Update versions with Tycho (MANIFEST.MF, plugin.xml, feature.xml, ...)
MODULES=$(grep -l -r --include 'pom.xml' -e '<packaging>eclipse-plugin</packaging>' | sed "s/^\(.*\)\/pom\.xml/<module>\1<\/module>/" | paste -sd " ")
sed -e "s+<modules></modules>+<modules>$MODULES</modules>+" releng/updateVersion.pom.xml > updateVersion.pom.xml.tmp

ARTIFACTS=$(grep -l -r --include 'pom.xml' -e '<packaging>eclipse-plugin</packaging>' | xargs cat | grep "<artifactId>" | sed "s/^.*<artifactId>\(.*\)<.*$/\1/" | sort -u | grep -e ^net.enilink -e ^komma | grep -e komma-parent -v | paste -sd ",")
echo "Updating Maven and OSGi descriptors for: " ${ARTIFACTS}

mvn -Dtycho.mode=maven -f updateVersion.pom.xml.tmp org.eclipse.tycho:tycho-versions-plugin:0.23.0:set-version -DnewVersion=$NEWVERSION -Dartifacts=${ARTIFACTS}

# Update versions with Maven - parent projects are not updated
sed -e "s+<modules></modules>+<modules><module>releng/komma-updatesite</module></modules>+" releng/updateVersion.pom.xml > updateVersion.pom.xml.tmp
mvn -Dtycho.mode=maven -f updateVersion.pom.xml.tmp org.apache.maven.plugins:maven-release-plugin:2.5.3:update-versions -DautoVersionSubmodules=true -DdevelopmentVersion=$NEWVERSION

# delete temporary aggregator project
rm updateVersion.pom.xml.tmp

# Undo changes in libraries since these modules have fixed version numbers
git -C libraries reset --hard


