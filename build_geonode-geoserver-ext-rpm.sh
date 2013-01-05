#!/bin/bash

set -e

export GEONODE_EXT_ROOT=$PWD
DL_ROOT=/var/www/geoserver
GIT_REV=$(git log -1 --pretty=format:%h)

rpmbuild --define "_topdir ${PWD}/rpm" -bb rpm/SPECS/geoserver.spec

scp -P 7777 -i ../jenkins_key.pem ../*.rpm jenkins@build.geonode.org:$DL_ROOT/$GIT_REV