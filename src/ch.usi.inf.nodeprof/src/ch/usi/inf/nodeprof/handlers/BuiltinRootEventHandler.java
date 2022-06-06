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
package ch.usi.inf.nodeprof.handlers;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;

import ch.usi.inf.nodeprof.ProfiledTagEnum;

/**
 * Abstract event handler for function roots
 */
public abstract class BuiltinRootEventHandler extends BaseSingleTagEventHandler {

    protected final TruffleString builtinName;
    private static final TruffleString CONSTRUCT_STR = Strings.constant("[[Construct]]");

    public BuiltinRootEventHandler(EventContext context) {
        super(context, ProfiledTagEnum.BUILTIN);
        this.builtinName = getAttributeTString("name");
    }

    public Object getReceiver(VirtualFrame frame) {
        Object receiver = frame.getArguments()[0];
        if (receiver == JSFunction.CONSTRUCT) {
            return CONSTRUCT_STR;
        }
        return receiver;
    }

    public TruffleString getBuiltinName() {
        return this.builtinName;
    }

    public Object getFunction(VirtualFrame frame) {
        return frame.getArguments()[1];
    }

    public Object[] getArguments(VirtualFrame frame) {
        return frame.getArguments();
    }

    public Object getArgument(VirtualFrame frame, int index) {
        return getArguments(frame)[2 + index];
    }

}
