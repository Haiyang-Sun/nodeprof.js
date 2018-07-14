#!/bin/bash
# Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
for i in $DIR/*.patch; do
        git apply $i
done

