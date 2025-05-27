#!/bin/bash

# Setup required environment variables from defaults, if not already defined
# in the environment. These variables are required by IAS tools but in some
# cases we want to find their values from default instead of dirty the environment.
#
# This script is invoked by other IAS scripts to avoid defining IAS environment
# variables in .bashrc, .bash_profile or .bashrc.d/ as it is the case when the 
# IAS is installed by an RPM.
# No need to setup PATH and PYTHONPATH that are already setup by the RPM
# but note that the RPM does set the environment to get files from the
# folder that is needed for development.
#
# Source ias-bash-profile.sh to set up the IAS environment variables from a bash script
# or for development purposes or when the alarm system is NOT installed with an RPM

if [ -z "$IAS_ROOT" ]; then
  IAS_ROOT="/opt/IasRoot"
fi

if [ -z "$IAS_LOGS_FOLDER" ]; then
  IAS_LOGS_FOLDER="$IAS_ROOT/logs"
fi

if [ -z "$IAS_TMP_FOLDER" ]; then
  IAS_TMP_FOLDER="$IAS_ROOT/tmp"
fi

if [ -z "$IAS_CONFIG_FOLDER" ]; then
  IAS_CONFIG_FOLDER="$IAS_ROOT/config"
fi

if [ -z "$KAFKA_HOME" ]; then
  KAFKA_HOME="/opt/kafka"
fi
