#!/bin/bash

set -e

export GEONODE_EXT_ROOT=$PWD

rpmbuild --define "_topdir ${PWD}/rpm" -bb rpm/SPECS/geoserver.spec