#!/bin/bash 
{
echo Start IAS services at `date +%Y/%m/%dT%H:%M:%S.%N`
if [ -z $KAFKA_HOME ]
then
	if [ -d "/opt/kafka" ]; then
		export KAFKA_HOME="/opt/kafka"
	else
		echo "Set KAFKA_HOME or install kafka in /opt/kafka before running this script"
		exit 1
	fi
fi

KAFKA_DATA="/var/lib/kafkadata"

echo "Cleaning $KAFKA_DATA..."
sudo mkdir -p $KAFKA_DATA
sudo rm -rf $KAFKA_DATA/*

echo "Starting kafka..."
cd $KAFKA_HOME
KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
echo "Cluster ID: $KAFKA_CLUSTER_ID" 
sudo bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/kraft/server.properties
sudo bin/kafka-server-start.sh -daemon config/kraft/server.properties
echo "Done"
echo
cd -
} 2>&1 | tee /var/log/kafka/startIasServices-`date +%Y%m%d-%H%M%S`.log
