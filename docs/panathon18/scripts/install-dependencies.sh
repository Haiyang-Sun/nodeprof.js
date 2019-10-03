#!/bin/bash
# Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.

for i in ../tests/franc; do
        pushd $i
	npm install franc
        popd
done

pushd ../sample-analyses
curl -O https://raw.githubusercontent.com/Berkeley-Correctness-Group/JITProf/master/src/js/analyses/jitprof/TypedArray.js
curl -O https://raw.githubusercontent.com/Berkeley-Correctness-Group/JITProf/master/src/js/analyses/jitprof/utils/Utils.js
popd
