#!/bin/bash 

set -e 
#set -x

#trap 'on_exit' EXIT
#
#on_exit()
#{
# if [ "$BAMBOO" == "1" ]
# then
#  svn revert -R .
# fi
#}

grep "STS PARENT POM" pom.xml > /dev/null || (echo "Script must be run from top-level directory"; exit 1)

echo "Booting..."
ulimit -a

ARGS=""
if [ "$BAMBOO" == "1" ]
then
 ARGS="-Dmaven.repo.local=/opt/bamboo-home/.m2/repository -Dhttpclient.retry-max=20"

 if [ "$JAVA_5" == "1" ]
 then
  export JAVA_HOME=/opt/java/jdk/Sun/1.5/jre
 else
  export JAVA_HOME=/opt/java/jdk/Sun/1.6
 fi
 export MAVEN_HOME=/opt/java/tools/maven/apache-maven-3.0
 export PATH=$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH
 export DISPLAY=:1

# svn revert -R .
fi

# clean up
echo "Cleaning test reports and screenshots..."
find -type d -name surefire-reports -print -exec rm -r {} +
find -type d -name screenshots -print -exec rm -r {} +

# build

echo "Building..."

mvn --version

if [ "$HEARTBEAT" == "1" ]
then

# compile and test
mvn \
-V -B -e \
${ARGS} \
$* \
clean compile integration-test

elif [ "$SKIP_TEST" == "1" ]
then

# compile and deploy
mvn \
--fail-at-end -V -B -e \
${ARGS} \
-Dmaven.test.skip=true \
$* \
clean deploy

else

# compile
mvn \
--fail-at-end -V -B -e \
${ARGS} \
-Dmaven.test.skip=true \
$* \
clean package

# test and deploy
mvn \
--fail-at-end -B -e \
${ARGS} \
$* \
deploy

fi
