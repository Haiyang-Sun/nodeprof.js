#!/bin/bash
svmnode=`find ../graal/vm/latest_graalvm/* -name node -type l`

$svmnode $@
