#!/usr/bin/bash
unset IAS_CDB; testConfigNoCdb
export IAS_CDB="src/test"; testConfigWithCdb
