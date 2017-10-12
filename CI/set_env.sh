#! /bin/bash

RED=`tput setaf 1`
NC=`tput sgr 0`

JAVA_HOME="/usr/lib/jvm/java-1.8.0-openjdk-amd64"
JRE_HOME="/usr/lib/jvm/java-1.8.0-openjdk-amd64/jre"
SCALA_HOME="/usr/share/scala"
IAS_DIR="IntegratedAlarmSystemRoot"

if [ ! -d $JAVA_HOME ]; then
  echo "${RED}ERROR: JAVA home directory does not exist: $JAVA_HOME${NC}"
else
  export JAVA_HOME
  echo "JAVA_HOME: $JAVA_HOME, set successfully"
fi


if [ ! -d $JRE_HOME ]; then
  echo "${RED}ERROR: JRE home directory does not exist: $JRE_HOME${NC}"
else
  export JRE_HOME
  echo "JRE_HOME: $JRE_HOME, set successfully"
fi


if [ ! -d $SCALA_HOME ]; then
  echo "${RED}ERROR: SCALA home directory does not exist: $SCALA_HOME${NC}"
else
  export SCALA_HOME
  echo "SCALA_HOME: $SCALA_HOME, set successfully"
fi


if [ ! -d $IAS_DIR ]; then
  mkdir $IAS_DIR
fi

IAS_ROOT="$(pwd)/$IAS_DIR"
export IAS_ROOT
echo "IAS_ROOT: $IAS_ROOT, set successfully"
