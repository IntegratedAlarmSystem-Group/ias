#!/bin/bash 
{
echo Terminating IAS serices at `date +%Y/%m/%dT%H:%M:%S.%N`
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

cd $KAFKA_HOME
echo  "Stopping kafka..." 
sudo bin/kafka-server-stop.sh config/kraft/server.properties
sleep 1
echo "Cleaning folders..."
sudo rm -rf $KAFKA_DATA/*
echo "Done"
echo
cd -
} 2>&1 |tee /var/log/kafka/stopIasServices-`date +%Y%m%d-%H%M%S`.log

