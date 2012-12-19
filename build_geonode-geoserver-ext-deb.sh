#!/bin/bash

set -e

DL_ROOT=/var/www/geoserver
GIT_REV=$(git log -1 --pretty=format:%h)

debuild

if [ -d $DL_ROOT/$GIT_REV ]; then
    rm -rf $DL_ROOT/$GIT_REV
fi

mkdir $DL_ROOT/$GIT_REV
cp ../*.deb $DL_ROOT/$GIT_REV/.
cp target/geoserver.war $DL_ROOT/$GIT_REV/.
cp target/geonode-geoserver-ext-*-geoserver-plugin.zip $DL_ROOT/$GIT_REV/.

# Remove all but last 4 builds to stop disk from filling up
(ls -t|tail -n 3)|sort|uniq -u | xargs rm -rf

rm -rf $DL_ROOT/latest
ln -sf $DL_ROOT/$GIT_REV $DL_ROOT/latest
