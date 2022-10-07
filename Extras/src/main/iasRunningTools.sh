#!/bin/bash

CMD="iasRun -l s org.eso.ias.extras.info.RunningIasTools $@"

echo Will run
echo $CMD

$CMD