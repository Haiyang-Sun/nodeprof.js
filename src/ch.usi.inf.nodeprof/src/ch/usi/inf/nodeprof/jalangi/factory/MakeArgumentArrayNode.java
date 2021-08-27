/* *****************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package ch.usi.inf.nodeprof.jalangi.factory;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSArray;

import ch.usi.inf.nodeprof.utils.GlobalObjectCache;

/**
 * create a JS array including the arguments for invoke/functionEnter
 */
public abstract class MakeArgumentArrayNode extends Node {
    private final JSContext jsContext;

    @CompilationFinal(dimensions = 0) private Object[] arguments;

    /**
     * offset marks where the first argument is in the inputs
     */
    private final int offset;

    /**
     * expected not used slots in the tail of the inputs array
     */
    private final int tillEnd;

    public MakeArgumentArrayNode(DynamicObject function, int offset, int tillEnd) {
        this.jsContext = GlobalObjectCache.getInstance().getJSContext(function);
        this.offset = offset;
        this.tillEnd = tillEnd;
    }

    public abstract Object executeArguments(Object[] input);

    private void copy(Object[] input) {
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = input[i + offset];
        }
    }

    public Object[] getArguments() {
        return this.arguments;
    }

    private Object toJSArray() {
        return JSArray.createConstantObjectArray(jsContext, JSRealm.get(this), arguments);
    }

    protected boolean notEnoughArgs(Object[] input) {
        return input == null || input.length <= (offset + tillEnd);
    }

    protected boolean argumentsMatch(Object[] input) {
        return arguments != null && (input.length == (arguments.length + offset + tillEnd));
    }

    /**
     * @param input the inputs from savedInputValues
     */
    @Specialization(guards = "notEnoughArgs(input)")
    public Object executeNull(Object[] input) {
        return JSArray.createEmpty(jsContext, JSRealm.get(this), 0);
    }

    @Specialization(guards = "argumentsMatch(input)")
    public Object executeCache(Object[] input) {
        copy(input);
        return toJSArray();
    }

    @Specialization
    public Object executeOther(Object[] input) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.arguments = new Object[input.length - offset - tillEnd];
        copy(input);
        return toJSArray();
    }
}
