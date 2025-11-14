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

### We bring in 3.8.2 that wasn't included in GS 2.27.3
rm WEB-INF/lib/geofence-services-impl-3.8.1.jar

## copy the new ones
cp -v ../geoserver-geonode-ext/libs/* WEB-INF/lib/

cp -r ../geoserver-geonode-ext/data ./
zip -r ../artifacts/geoserver.war ./
popd

# create zip artifact for data
pushd geoserver-geonode-ext
zip -r ../artifacts/geonode-geoserver-ext-web-app-data.zip data/
popd

# cleaning up
rm -Rf _tmp/
