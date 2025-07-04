#!/bin/bash

cp -r geoserver-geonode-ext geoserver/src/web/app/target/
pushd geoserver/src/web/app/target/

# output folder with artifacts
mkdir artifacts

mv geoserver.war geoserver_vanilla.war
# copy geoserver_vanilla.wat artifact
cp geoserver_vanilla.war artifacts/

# inflate data folder in geonode.war
mkdir _tmp
pushd _tmp
unzip ../geoserver_vanilla.war

## copy the new ones
cp -v ../geoserver-geonode-ext/libs/* WEB-INF/lib/

rm WEB-INF/lib/postgis-stubs-1.3.3.jar
rm WEB-INF/lib/postgresql-8.3-603.jdbc4.jar

cp -r ../geoserver-geonode-ext/data ./
zip -r ../artifacts/geoserver.war ./
popd

# create zip artifact for data
pushd geoserver-geonode-ext
zip -r ../artifacts/geonode-geoserver-ext-web-app-data.zip data/
popd

# cleaning up
rm -Rf _tmp/