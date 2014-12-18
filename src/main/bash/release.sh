#!/bin/sh

function getVersionToRelease() {
    CURRENT_VERSION=`mvn ${MVN_ARGS} org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v "\[INFO\]"`
    echo ${CURRENT_VERSION%%-SNAPSHOT}
}

function guess() {
    v=$1
    echo "${v%.*}.$((${v##*.}+1))-SNAPSHOT"
}

<<<<<<< HEAD
=======

>>>>>>> a5b73eedbb1aeacc9f4bbf73751998ae6ac32d78
VERSION=$(getVersionToRelease)
NEXT=$(guess $VERSION)
TAG="choco-graph-${VERSION}"

git fetch
<<<<<<< HEAD
git checkout -b release || exit 1

mvn -q dependency:purge-local-repository

echo "New version is ${VERSION}"
#Update the poms:wq
mvn versions:set -DnewVersion=${VERSION} -DgenerateBackupPoms=false
git commit -m "initiate release ${VERSION}" -a

echo "Start release"
#Extract the version
COMMIT=$(git rev-parse HEAD)

#Quit if tag already exists
git ls-remote --exit-code --tags origin ${TAG} && quit "tag ${TAG} already exists"

#Working version ?
# Well, we assume the tests have been run before, and everything is OK for the release
mvn clean test ||exit 1

git fetch origin master:refs/remotes/origin/master||quit "Unable to fetch master"
#Integrate with master and tag
echo "** Integrate to master **"
git checkout master ||quit "No master branch"
git pull origin master || quit "Unable to pull master"
git merge --no-ff -m "Merge tag '${TAG}' into master" ${COMMIT} ||quit "Unable to integrate to master"

#NOT USED FOR THE MOMENT
##Javadoc
#./bin/push_javadoc apidocs.git ${VERSION}

git tag ${TAG} ||quit "Unable to tag with ${TAG}"
git push --tags ||quit "Unable to push the tag ${TAG}"
git push origin master ||quit "Unable to push master"

#    #Deploy the artifacts
#echo "** Deploying the artifacts **"
mvn -P release clean javadoc:jar source:jar deploy -DskipTests ||quit "Unable to deploy"

#Set the next development version
#echo "** Prepare develop for the next version **"
git checkout develop ||quit "Unable to checkout develop"
git pull origin develop ||quit "Unable to pull develop"
git merge --no-ff -m "Merge tag '${TAG}' into develop"  ${TAG} ||quit "Unable to integrate to develop"
mvn versions:set -DnewVersion=${NEXT} -DgenerateBackupPoms=false
git commit -m "Prepare the code for the next version" -a ||quit "Unable to commit to develop"
#
##Push changes on develop, with the tag
git push origin develop ||quit "Unable to push to develop"

#Clean
git branch --delete release ||quit "Unable to delete release"

git checkout $TAG
mvn clean install -DskipTests
=======
git checkout -b release

mvn versions:set -DnewVersion=${VERSION} -DgenerateBackupPoms=false
git commit -m "initiate release ${VERSION}" -a

COMMIT=$(git rev-parse HEAD)

git ls-remote --exit-code --tags origin ${TAG}

## MASTER
git checkout master
git merge --no-ff ${COMMIT}
git tag ${TAG}
git push --tags
git pull origin master
git push origin master

## DEVELOP
git checkout develop
git merge --no-ff ${TAG}
mvn versions:set -DnewVersion=${NEXT} -DgenerateBackupPoms=false
git commit -m "Prepare the code for the next version" -a
git push origin develop
git push --all && git push --tags
git push origin --delete release

git checkout $TAG
mkdir ${TAG}
mvn clean install -DskipTests
mv ./target/choco-graph-${VERSION}.jar ./$TAG/
mv ./doc/ChocoGraphDoc.pdf ./$TAG/
>>>>>>> a5b73eedbb1aeacc9f4bbf73751998ae6ac32d78
git checkout develop