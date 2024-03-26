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

## get rid of clashing/outdated dependencies
rm -Rfv WEB-INF/lib/gs-sec-oauth2-core*
## copy the new ones
cp -v ../geoserver-geonode-ext/libs/* WEB-INF/lib/

## replace H2 hibernate with PostGIS for the Geofence rules DB
wget https://repo1.maven.org/maven2/org/postgis/postgis-jdbc/1.3.3/postgis-jdbc-1.3.3.jar -O WEB-INF/lib/postgis-jdbc-1.3.3.jar
wget https://maven.geo-solutions.it/org/hibernatespatial/hibernate-spatial-postgis/1.1.3.2/hibernate-spatial-postgis-1.1.3.2.jar -O WEB-INF/lib/hibernate-spatial-postgis-1.1.3.2.jar
rm WEB-INF/lib/hibernate-spatial-h2-geodb-1.1.3.2.jar 

cp -r ../geoserver-geonode-ext/data ./
zip -r ../artifacts/geoserver.war ./
popd

# create zip artifact for data
pushd geoserver-geonode-ext
zip -r ../artifacts/geonode-geoserver-ext-web-app-data.zip data/
popd

# cleaning up
rm -Rf _tmp/