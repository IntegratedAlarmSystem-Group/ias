#!/usr/bin/env bash

TEMP_PARMS_ARRAY=( "$@" )

JAVA_PROPS=""
OTHER_PARAMS=""

for index in "${!TEMP_PARMS_ARRAY[@]}"
do
    if [[ ${TEMP_PARMS_ARRAY[index]} == -D* ]] ;
    then
    	JAVA_PROPS="$JAVA_PROPS ${TEMP_PARMS_ARRAY[index]}"
	else
		OTHER_PARAMS="$OTHER_PARAMS ${TEMP_PARMS_ARRAY[index]}"
	fi
done

if [[ ! -z $JAVA_PROPS ]] ;
then
	echo "Found java properties: $JAVA_PROPS"
fi

if [[ -z $OTHER_PARAMS ]];
then
	echo "Missing supervisor ID in command line"
else
	TEMP=( $OTHER_PARAMS )
	ID=${TEMP_PARMS_ARRAY[0]}
	echo "Supervisor ID=$ID"
	LOGID_PARAM="-i $ID"
fi

TEMP=( $OTHER_PARAMS )
ID=${TEMP_PARMS_ARRAY[0]}
echo "Supervisor ID=$ID"

CMD="iasRun -l s $JAVA_PROPS $LOGID_PARAM org.eso.ias.sink.email.NotificationsSender $OTHER_PARAMS"

echo Will run
echo $CMD

$CMD
