#!/bin/bash

# Set the external host and port
if [ ! -z "$ADVERTISED_HOST" ]; then
    echo "advertised host: $ADVERTISED_HOST"
    if grep -q "^advertised.host.name" $IAS_ROOT/config/kafka-server.properties; then
        sed -r -i "s/#(advertised.host.name)=(.*)/\1=$ADVERTISED_HOST/g" $IAS_ROOT/config/kafka-server.properties
    else
        echo "advertised.host.name=$ADVERTISED_HOST" >> $IAS_ROOT/config/kafka-server.properties
    fi
fi
if [ ! -z "$ADVERTISED_PORT" ]; then
    echo "advertised port: $ADVERTISED_PORT"
    if grep -q "^advertised.port" $IAS_ROOT/config/kafka-server.properties; then
        sed -r -i "s/#(advertised.port)=(.*)/\1=$ADVERTISED_PORT/g" $IAS_ROOT/config/kafka-server.properties
    else
        echo "advertised.port=$ADVERTISED_PORT" >> $IAS_ROOT/config/kafka-server.properties
    fi
fi

$KAFKA_HOME/bin/zookeeper-server-start.sh -daemon $IAS_ROOT/config/zoo.cfg
$KAFKA_HOME/bin/kafka-server-start.sh $IAS_ROOT/config/kafka-server.properties
