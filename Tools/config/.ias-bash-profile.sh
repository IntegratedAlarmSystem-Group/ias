#!/bin/bash
# IAS bash environnment

bold=$(tput bold)
normal=$(tput sgr0)

echo
echo "Setting IAS environment..."

ERRORS_FOUND=0
# Check pre-requisites
if [ -z $JAVA_HOME ]; then
	echo "${bold}JAVA_HOME is not defined!${normal}"
	((ERRORS_FOUND++))
fi
if [ -z $SCALA_HOME ]; then
	echo "${bold}SCALA_HOME is not defined!${normal}"
	((ERRORS_FOUND++))
fi
if [ -z "$JRE_HOME" ]; then
	# Try to guess about jre folder...
	TEMP_JRE_HOME = $JAVA_HOME/jre
	if [ -d "$TEMP_JRE_HOME" ]; then
		export JRE_HOME=$TEMP_JRE_HOME
	else
		echo "${bold}JRE_HOME not found and $JAVA_HOME/jre does not exist!${normal}"
		((ERRORS_FOUND++))
	fi
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
export IAS_LOGS_FOLDER=$IAS_ROOT/logs
export IAS_TMP_FOLDER=$IAS_ROOT/tmp
export IAS_CONFIG_FOLDER=$IAS_ROOT/config

export PYTHONPATH="../lib/python:$IAS_ROOT/lib/python:$PYTHONPATH"

PATH="../bin:$IAS_ROOT/bin:$JAVA_HOME/bin:$JRE_HOME/bin:$SCALA_HOME/bin:$PATH"
export PATH

if [ "$ERRORS_FOUND" -eq "0" ]; then 
	echo "${normal}IAS environment ready"
	echo "  ${normal}ROOT${normal}: ${bold}$IAS_ROOT${normal}"
	echo "  ${normal}LOGS${normal}: ${bold}$IAS_LOGS_FOLDER${normal}"
	echo "  ${normal}TMP${normal}: ${bold}$IAS_TMP_FOLDER${normal}"
	echo "  ${normal}CONFIG${normal}: ${bold}$IAS_CONFIG_FOLDER${normal}"
	echo
else
	echo "${bold}$ERRORS_FOUND errors found.${normal} Check IAS environment!!!"
fi
echo
