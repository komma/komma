MODULES=$(grep -L -r --include 'pom.xml' '<module>' | sed "s/^\(.*\)\/pom\.xml/<module>\1<\/module>/" | paste -sd " ")

sed -e "s+<modules></modules>+<modules>$MODULES</modules>+" releng/updateVersion.pom.xml > updateVersion.pom.xml.tmp

ARTIFACTS=$(grep -L -r --include 'pom.xml' '<module>' | xargs cat | grep "<artifactId>" | sed "s/^.*<artifactId>\(.*\)<.*$/\1/" | sort -u | grep -e ^net.enilink -e ^komma | grep -e ^komma-all -e ^komma-features -e ^komma-parent -e ^komma-rap-feature -e \.rap\. -v | paste -sd ",")
echo ${ARTIFACTS}

mvn -f updateVersion.pom.xml.tmp org.eclipse.tycho:tycho-versions-plugin:0.23.0:set-version -DnewVersion=1.3.0.qualifier -Dtycho.mode=maven -Dartifacts=${ARTIFACTS}

#rm updateVersion.pom.xml.tmp
