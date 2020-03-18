#!/bin/bash 

cd /opt/kafka_2.12-2.4.0
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

