#! /bin/bash

## SET UP:
EMP=`tput setaf 2`
NE=`tput sgr 0`

#    DISCLAIMER: edit these with care, nasty things may happen
#    E.g: "rm -rf /" would be catastrophic!!
SCALA="scala-2.12.1.deb"


# JAVA
echo "${EMP}Installing Java:${NE}"
sudo apt-get -y install openjdk-8-jdk

## Ant
echo "${EMP}Installing Ant:${NE}"
sudo apt-get -y install ant
sudo apt-get -y install ant-contrib

## Scala
echo "${EMP}Installing Scala:${NE}"
sudo apt-get remove scala-library scala
if [ ! -f $SCALA ]; then
  sudo wget http://scala-lang.org/files/archive/$SCALA
fi
sudo dpkg -i $SCALA
sudo apt-get update
sudo apt-get -y install scala

## KAFKA:
echo "${EMP}Installing Kafka:${NE}"
sudo apt-get -y install zookeeperd
KAFKA="kafka_2.12-0.11.0.0"
KAFKA_TGZ="$KAFKA.tgz"
if [ ! -f $KAFKA_TGZ ]; then
  wget http://www-eu.apache.org/dist/kafka/0.11.0.0/$KAFKA_TGZ
fi
tar xvf $KAFKA_TGZ
sudo mv $KAFKA_TGZ /usr/local/kafka

## FINISH:
echo "${EMP}Installation successful.${NE}"
rm -f $SCALA
rm -rf $KAFKA
