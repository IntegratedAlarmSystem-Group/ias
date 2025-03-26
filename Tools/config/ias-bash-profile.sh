#!/bin/bash
# IAS bash environnment

ERRORS_FOUND=0
# Check pre-requisites
if ! command -v java &>/dev/null;
then
	echo "${bold}java not found (to be installed or added to the PATH)!${normal}"
	((ERRORS_FOUND++))
fi

if [ -z "$IAS_ROOT" ]; then
	echo "${bold}IAS_ROOT is not defined!${normal}"
	((ERRORS_FOUND++))
fi

# Check if IAS_ROOT folder exists
if [ ! -d "$IAS_ROOT" ]; then
	echo "${bold}IAS root $IAS_ROOT does not exist!${normal}"
	((ERRORS_FOUND++))
fi
	
#
# IAS setup
#
if [ -z "$IAS_LOGS_FOLDER" ]; then
    export IAS_LOGS_FOLDER=$IAS_ROOT/logs
fi

if [ -z "$IAS_TMP_FOLDER" ]; then
    export IAS_TMP_FOLDER=$IAS_ROOT/tmp
fi

if [ -z "$IAS_CONFIG_FOLDER" ]; then
    export IAS_CONFIG_FOLDER=$IAS_ROOT/config
fi

# Get python version from the output of 'python3 -V'
PYTHON_VERSION=$(python3 -V|cut -d ' ' -f2|cut -d '.' -f1-2)
PY_TEMP=lib/python$PYTHON_VERSION/site-packages
export PYTHONPATH="build/$PY_TEMP:$IAS_ROOT/$PY_TEMP:$PYTHONPATH"
unset PY_TEMP

PATH="build/bin:$IAS_ROOT/bin:$PATH"
export PATH

if [[ "$ERRORS_FOUND" != "0" ]]
then 
	echo "$ERRORS_FOUND errors found! Check IAS environment!!!"
fi
