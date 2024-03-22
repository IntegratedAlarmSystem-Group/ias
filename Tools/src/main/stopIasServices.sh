#!/bin/bash 
if [ -z $KAFKA_HOME ]
then
        echo "Set KAFKA_HOME before running this script"
        exit 1
fi
KAFKA_DATA="/var/lib/kafkadata"

cd $KAFKA_HOME
echo  "Stopping kafka..." 
sudo bin/kafka-server-stop.sh config/kraft/server.properties
sleep 1
echo "Cleaning folders..."
sudo rm -rf $KAFKA_DATA/*
echo "Done"
echo
cd -

