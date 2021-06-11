#!/bin/bash 

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

