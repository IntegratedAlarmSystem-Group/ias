#!/bin/bash
. ias-env
CMD="iasRun -r org.eso.ias.extras.info.RunningIasTools $@"

echo Will run
echo $CMD

$CMD
