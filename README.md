geoserver-geonode-ext
=====================

This repository contains branches with the default Geoserver for GeoNode Data Directory, which includes configurations for its plugins (Security, GWC, Printing, etc.).

The data directory is uploaded to an AWS Bucket and is used to build the [geoserver_data](https://github.com/GeoNode/geonode-docker/tree/master/docker/geoserver_data) Docker iamges, published to the [Docker Hub](https://hub.docker.com/repository/docker/geonode/geoserver_data)

> [!NOTE]
> The main branch is empty. A dedicated branch is used for each published Geoserver version.