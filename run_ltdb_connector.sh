#!/bin/bash
source Tools/config/ias-bash-profile.sh

if [ $CASSANDRA_HOST -eq "0" ] || [ -z "${CASSANDRA_HOST// }"]; then
    echo "There is no CASSANDRA_HOST in environment variables"
    exit
fi

if [ $KAFKA_HOST -eq "0" ] || [ -z "${KAFKA_HOST// }"]; then
    echo "There is no KAFKA_HOST in environment variables"
    exit
fi

# Set the plugin path
if grep -q "^plugin.path" $IAS_ROOT/config/LtdbCassandraStandalone.properties; then
    pwdesc=$(echo $IAS_ROOT | sed 's_/_\\/_g')
    echo "Overwrite plugin path with $pwdesc"
    sed -r -i "s/(plugin.path)=(.*)/\1=$pwdesc/g" $IAS_ROOT/config/LtdbCassandraStandalone.properties
fi

# Set the kafka broker server and port
if grep -q "^bootstrap.servers" $IAS_ROOT/config/LtdbCassandraStandalone.properties; then
    echo "Overwrite bootstrap.servers with $KAFKA_HOST:$KAFKA_PORT"
    sed -r -i "s/(bootstrap.servers)=(.*)/\1=$KAFKA_HOST:$KAFKA_PORT/g" $IAS_ROOT/config/LtdbCassandraStandalone.properties
fi

# Set the cassandra host server
if grep -q "^cassandra.contact.points" $IAS_ROOT/config/LtdbCassandraConnector.properties; then
    echo "Overwrite cassandra.contact.points with $CASSANDRA_HOST"
    sed -r -i "s/(cassandra.contact.points)=(.*)/\1=$CASSANDRA_HOST/g" $IAS_ROOT/config/LtdbCassandraConnector.properties
fi

iasLTDBConnector
