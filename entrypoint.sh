#!/bin/bash

source Tools/config/ias-bash-profile.sh
iasRun -l s -Dorg.eso.ias.converter.kafka.servers=kafka:9092 org.eso.ias.converter.Converter ConverterID -jcdb / &
iasRun -l s -Dorg.eso.ias.kafka.brokers=kafka:9092 org.eso.ias.supervisor.Supervisor SupervisorID -jcdb /cdb &
#iasRun -l j org.eso.ias.webserversender.WebServerSender &
sleep infinity
