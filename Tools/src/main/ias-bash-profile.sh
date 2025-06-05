#!/bin/bash

# Setup the IAS bash environnment to be used when IAS is not installed by an RPM
# or for custom IAS installations.
#
# Sourcing this sxcript is needed for IAS dedvelopment

ERRORS_FOUND=0
# Check pre-requisites
if ! command -v java &>/dev/null;
then
	echo "java not found (to be installed or added to the PATH)!"
	((ERRORS_FOUND++))
fi

if [ -z "$IAS_ROOT" ]; then
	echo "IAS_ROOT is not defined!"
	((ERRORS_FOUND++))
elif [ ! -d "$IAS_ROOT" ]; then
	echo "IAS root $IAS_ROOT does not exist!"
	((ERRORS_FOUND++))
fi
	
#
# IAS setup
#
if [ -z "$IAS_LOGS_FOLDER" ]; then
    export IAS_LOGS_FOLDER=$IAS_ROOT/logs
fi
if [ ! -d "$IAS_LOGS_FOLDER" ]; then
	mkdir -p "$IAS_LOGS_FOLDER"
fi
if [ ! -d "$IAS_LOGS_FOLDER" ]; then
	echo "Warning: IAS log folder $IAS_LOGS_FOLDER does not exist!"
	((ERRORS_FOUND++))
fi

if [ -z "$IAS_TMP_FOLDER" ]; then
    export IAS_TMP_FOLDER=$IAS_ROOT/tmp
fi
if [ ! -d "$IAS_TMP_FOLDER" ]; then
	mkdir -p "$IAS_TMP_FOLDER"
fi
if [ ! -d "$IAS_TMP_FOLDER" ]; then
	echo "Warning: IAS tmp folder $IAS_TMP_FOLDER does not exist!"
	((ERRORS_FOUND++))
fi

if [ -z "$IAS_CONFIG_FOLDER" ]; then
    export IAS_CONFIG_FOLDER=$IAS_ROOT/config
fi
if [ ! -d "$IAS_CONFIG_FOLDER" ]; then
	mkdir -p "$IAS_CONFIG_FOLDER"
fi
if [ ! -d "$IAS_CONFIG_FOLDER" ]; then
	echo "Warning: IAS config folder $IAS_CONFIG_FOLDER does not exist!"
	((ERRORS_FOUND++))
fi

# Get python version
PY_TEMP=$(python3 -c "import sys; print(f'lib/python{sys.version_info.major}.{sys.version_info.minor}/site-packages')")
export PYTHONPATH="build/$PY_TEMP:$IAS_ROOT/$PY_TEMP:$PYTHONPATH"
unset PY_TEMP

PATH="build/bin:$IAS_ROOT/bin:$PATH"
export PATH

if [[ "$ERRORS_FOUND" != "0" ]]
then 
	echo "$ERRORS_FOUND errors found! Check IAS environment!!!"
fi
