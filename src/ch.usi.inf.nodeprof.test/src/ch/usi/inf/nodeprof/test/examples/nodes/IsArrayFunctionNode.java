/* *****************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 *
 */
public abstract class IsArrayFunctionNode extends Node {
    /**
     *
     * Checks whether the invocation of "function" on object "receiver" is an built-in array
     * operation
     *
     * @param receiver
     * @param function
     * @return returns the name of the function if it is, otherwise, returns null
     */
    public abstract String executeIsArrayFunction(Object receiver, Object function);

    protected String check(Object receiver, DynamicObject function) {
        if (!JSArray.isJSArray(receiver)) {
            return null;
        }
        return JSFunction.getName(function).toJavaStringUncached();
    }

    protected boolean isUndefined(Object object) {
        return object == Undefined.instance;
    }

    /**
     * @param receiver
     * @param function
     */
    @Specialization(guards = "isUndefined(receiver)")
    protected String executeIsFunction(Object receiver, DynamicObject function) {
        return null;
    }

    /**
     * @param receiver
     * @param function
     * @param cacheFunction
     */
    @Specialization(guards = "cacheFunction == function")
    protected String executeIsFunction(Object receiver, DynamicObject function,
                    @Cached(value = "function") DynamicObject cacheFunction,
                    @Cached(value = "check(receiver, cacheFunction)") String result) {
        return result;
    }

    @Specialization
    protected String executeObject(Object receiver, DynamicObject input) {
        return check(receiver, input);
    }
}
