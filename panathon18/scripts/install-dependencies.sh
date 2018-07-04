#!/bin/bash
# Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.

for i in ../tests/npms/*; do
        pushd $i
	npm install
        popd
done

