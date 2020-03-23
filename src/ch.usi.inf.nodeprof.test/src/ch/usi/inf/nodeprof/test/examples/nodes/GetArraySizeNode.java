/* *****************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * *****************************************************************************/
package ch.usi.inf.nodeprof.test.examples.nodes;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;

import ch.usi.inf.nodeprof.utils.Logger;

/**
 * Get the array size from an array object
 */
public abstract class GetArraySizeNode extends Node {

    public GetArraySizeNode() {
    }

    public abstract long executeSize(Object o);

    private static long getSize(DynamicObject arr) {
        /**
         * TODO, maybe JSArray.arrayGetLength(thisObj) is faster
         */
        try {
            return InteropLibrary.getFactory().getUncached().getArraySize(arr);
        } catch (UnsupportedMessageException e) {
            Logger.error("ArrayGetSize failed");
            e.printStackTrace();
        }
        return -1;
    }

    @Specialization
    long doCache(DynamicObject obj) {
        return getSize(obj);
    }

    @Specialization
    long doObject(Object obj) {
        assert !(obj instanceof DynamicObject);
        return -1;
    }
}
