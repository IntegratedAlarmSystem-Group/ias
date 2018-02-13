#!/bin/bash

source Tools/config/ias-bash-profile.sh
iasRun -l s org.eso.ias.converter.Converter ConverterID &
iasRun -l s org.eso.ias.supervisor.Supervisor SupervisorID &
iasRun -l j org.eso.ias.webserversender.WebServerSender &
sleep infinity
