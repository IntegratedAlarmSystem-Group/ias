#!/bin/bash

source Tools/config/ias-bash-profile.sh
if [ $# -eq 0 ]
then
  ant build
else
  cd "$1"
  cd src/main
  ant build install
fi
chmod -R a=u $IAS_ROOT
