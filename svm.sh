#!/bin/bash
svmnode=`find ../graal/vm/latest_graalvm/* -name node | grep jre.bin`

$svmnode $@
