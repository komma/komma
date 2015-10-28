MODULES=$(grep -l -r --include 'pom.xml' -e '<packaging>eclipse-plugin</packaging>' | sed "s/^\(.*\)\/pom\.xml/<module>\1<\/module>/" | paste -sd " ")

sed -e "s+<modules></modules>+<modules>$MODULES</modules>+" releng/updateVersion.pom.xml > updateVersion.pom.xml.tmp

ARTIFACTS=$(grep -l -r --include 'pom.xml' -e '<packaging>eclipse-plugin</packaging>' | xargs cat | grep "<artifactId>" | sed "s/^.*<artifactId>\(.*\)<.*$/\1/" | sort -u | grep -e ^net.enilink -e ^komma | grep -e komma-parent -v | paste -sd ",")
echo ${ARTIFACTS}

mvn -Dtycho.mode=maven -f updateVersion.pom.xml.tmp org.eclipse.tycho:tycho-versions-plugin:0.23.0:set-version -DnewVersion=1.4.0.qualifier -Dartifacts=${ARTIFACTS}

mvn -Dtycho.mode=maven org.apache.maven.plugins:maven-release-plugin:2.5.3:update-versions -DdevelopmentVersion=1.4.0-SNAPSHOT

pushd releng/komma-updatesite
mvn -Dtycho.mode=maven org.apache.maven.plugins:maven-release-plugin:2.5.3:update-versions -DdevelopmentVersion=1.4.0-SNAPSHOT
popd

#rm updateVersion.pom.xml.tmp
