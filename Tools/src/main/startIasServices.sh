#!/bin/bash 
if [ -z $KAFKA_HOME ]
then
	echo "Set KAFKA_HOME before running this script"
	exit 1
fi

KAFKA_DATA="/var/lib/kafkadata"

echo "Cleaning $KAFKA_DATA..."
sudo mkdir -p $KAFKA_DATA
sudo rm -rf $KAFKA_DATA/*



echo  "Starting kafka..."
cd /opt/kafka
KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
echo "Cluster ID: $KAFKA_CLUSTER_ID" 
sudo bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/kraft/server.properties
sudo bin/kafka-server-start.sh -daemon config/kraft/server.properties
echo "Done"
echo
cd -

