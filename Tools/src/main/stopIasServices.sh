#!/bin/bash 
if [ -z $KAFKA_HOME ]
then
        echo "Set KAFKA_HOME before running this script"
        exit 1
fi
cd $KAFKA_HOME
echo  "Stopping zookeeper..."
bin/zookeeper-server-stop.sh config/zookeeper.properties
echo  "Stopping kafka..." 
bin/kafka-server-stop.sh config/server.properties
sleep 1
echo "Cleaning folders..."
rm -rf /opt/kafkadata/* /opt/zookeeperdata/*
echo "Done"
echo
cd -

