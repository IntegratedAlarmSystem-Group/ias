#!/bin/bash

UUID_FILE="/var/lib/kafka/cluster.uuid"
CONFIG="/opt/kafka/config/kraft/server.properties"
LOG_DIR=$(grep '^log.dirs=' "$CONFIG" | cut -d= -f2)

if [ ! -f "$UUID_FILE" ]; then
    echo "No cluster UUID found. Generating and formatting..."
    UUID=$(/opt/kafka/bin/kafka-storage.sh random-uuid)
    echo "$UUID" > "$UUID_FILE"
    /opt/kafka/bin/kafka-storage.sh format -t "$UUID" -c "$CONFIG"
else
    UUID=$(cat "$UUID_FILE")
    echo "Using existing cluster UUID: $UUID"
fi

exec /opt/kafka/bin/kafka-server-start.sh "$CONFIG"

