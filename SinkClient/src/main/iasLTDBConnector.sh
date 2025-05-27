#!/usr/bin/env bash
. ias-env
if [ "x$IAS_LOGS_FOLDER" = "x" ]
then
	export LOG_DIR="$IAS_ROOT/logs"
else 
	export LOG_DIR="$IAS_LOGS_FOLDER"
fi
echo "Log folder = $LOG_DIR"

LOG4J_PROP_FILE="kafka-connect-log4j.xml"
if [ -e "../../config/$LOG4J_PROP_FILE" ]
then
	LOG4JPROPS="../../config/$LOG4J_PROP_FILE"
elif [ -e "$IAS_ROOT/config/$LOG4J_PROP_FILE" ]
then
	LOG4JPROPS="$IAS_ROOT/config/$LOG4J_PROP_FILE"
else
	LOG4JPROPS="$KAFKA_HOME//config/tools-log4j.properties"
fi
echo "Log4j config file = $LOG4JPROPS"

CONN_PROPS="LtdbCassandraConnector.properties"
if [ -e "../../config/$CONN_PROPS" ]
then
    CONN_PROPS_FILE="../../config/$CONN_PROPS"
else
    CONN_PROPS_FILE="$IAS_ROOT/config/$CONN_PROPS"
fi
echo "Connector properties file = $CONN_PROPS_FILE"

STANDALONE_PROPS="LtdbCassandraStandalone.properties"
if [ -e "../../config/$STANDALONE_PROPS" ]
then
    STANDALONE_PROPS_FILE="../../config/$STANDALONE_PROPS"
else
    STANDALONE_PROPS_FILE="$IAS_ROOT/config/$STANDALONE_PROPS"
fi
echo "Standalone properties file = $STANDALONE_PROPS_FILE"

export KAFKA_LOG4J_OPTS="-Dlog4j.configurationFile=file:$LOG4JPROPS -Dlog4j.configuration=file:$LOG4JPROPS -Dias.logs.folder=$LOG_DIR"
$KAFKA_HOME/bin/connect-standalone.sh $STANDALONE_PROPS_FILE $CONN_PROPS_FILE

