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

rm WEB-INF/lib/gt-complex-*.jar
rm WEB-INF/lib/commons-jxpath-*.jar

PATCH="    <security-constraint>
      <web-resource-collection>
          <web-resource-name>BlockDemoRequests</web-resource-name>
          <url-pattern>/TestWfsPost/*</url-pattern>
      </web-resource-collection>
      <auth-constraint>
          <role-name>BLOCKED</role-name>
      </auth-constraint>
    </security-constraint>
    <security-constraint>
      <web-resource-collection>
          <web-resource-name>BlockGWC</web-resource-name>
          <url-pattern>/gwc/*</url-pattern>
      </web-resource-collection>
      <auth-constraint>
          <role-name>BLOCKED</role-name>
      </auth-constraint>
    </security-constraint>
    <security-constraint>
      <web-resource-collection>
          <web-resource-name>AllowGWC_Demo</web-resource-name>
          <url-pattern>/gwc/demo/*</url-pattern>
      </web-resource-collection>
    </security-constraint>
    <security-constraint>
      <web-resource-collection>
          <web-resource-name>AllowGWC_Services</web-resource-name>
          <url-pattern>/gwc/service/*</url-pattern>
      </web-resource-collection>
    </security-constraint>
    <security-constraint>
      <web-resource-collection>
          <web-resource-name>AllowGWC_Rest</web-resource-name>
          <url-pattern>/gwc/rest/*</url-pattern>
      </web-resource-collection>
    </security-constraint>"

awk -v block="$PATCH" '
/<\/web-app>/ {
  print block
}
{ print }
' WEB-INF/web.xml > WEB-INF/web_patched.xml

mv WEB-INF/web_patched.xml WEB-INF/web.xml

cp -r ../geoserver-geonode-ext/data ./
zip -r ../artifacts/geoserver.war ./
popd

# create zip artifact for data
pushd geoserver-geonode-ext
zip -r ../artifacts/geonode-geoserver-ext-web-app-data.zip data/
popd

# cleaning up
rm -Rf _tmp/
