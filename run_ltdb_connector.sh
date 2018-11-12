#!/bin/bash
source Tools/config/ias-bash-profile.sh

# Set the plugin path
if grep -q "^plugin.path" $IAS_ROOT/config/LtdbCassandraStandalone.properties; then
    pwdesc=$(echo $IAS_ROOT | sed 's_/_\\/_g')
    echo "Overwrite plugin path with $pwdesc"
    sed -r -i "s/(plugin.path)=(.*)/\1=$pwdesc/g" $IAS_ROOT/config/LtdbCassandraStandalone.properties
fi

# Set the kafka broker server and port
if grep -q "^bootstrap.servers" $IAS_ROOT/config/LtdbCassandraStandalone.properties; then
    echo "Overwrite bootstrap.servers with $ADVERTISED_HOST:$ADVERTISED_PORT"
    sed -r -i "s/(bootstrap.servers)=(.*)/\1=$ADVERTISED_HOST:$ADVERTISED_PORT/g" $IAS_ROOT/config/LtdbCassandraStandalone.properties
fi

iasLTDBConnector
