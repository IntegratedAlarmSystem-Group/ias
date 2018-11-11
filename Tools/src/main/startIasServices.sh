#!/bin/bash 

echo "Cleaning folders..."
rm -rf /opt/kafkadata/* /opt/zookeeperdata/*
cd /opt/kafka_2.12-2.0.0
echo  "Starting zookeeper..."
bin/zookeeper-server-start.sh -daemon config/zookeeper.properties
sleep 2
echo  "Starting kafka..." 
bin/kafka-server-start.sh -daemon config/server.properties
echo "Done"
echo
cd -

