/*******************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 *******************************************************************************/
package ch.usi.inf.nodeprof.handlers;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.Scope;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.regex.RegexBodyNode;
import com.oracle.truffle.regex.RegexRootNode;

import ch.usi.inf.nodeprof.ProfiledTagEnum;

/**
 * Abstract event handler for function roots
 */
public abstract class FunctionRootEventHandler extends BaseSingleTagEventHandler {
    protected final String funcName = context.getInstrumentedNode().getRootNode().getName();
    protected final boolean isRegExp = context.getInstrumentedNode().getRootNode() instanceof RegexRootNode || context.getInstrumentedNode() instanceof RegexBodyNode;
    protected final boolean isBuiltin = context.getInstrumentedNode() instanceof JSBuiltinNode;

    protected final String builtinName;

     @CompilationFinal private FrameSlot thisSlot;
     @CompilationFinal private boolean thisSlotInitialized = false;

    public FunctionRootEventHandler(EventContext context) {
        super(context, ProfiledTagEnum.ROOT);
        if (isBuiltin) {
            builtinName = getAttribute("name").toString();
        } else {
            builtinName = null;
        }
    }

    @Override
    protected SourceSection getSourceSectionForIID() { return context.getInstrumentedNode().getRootNode().getSourceSection(); }

    public Object getReceiver(VirtualFrame frame, TruffleInstrument.Env env) {
        // cache the frame slot for `this`
        if (!thisSlotInitialized) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            thisSlot = JSFrameUtil.getThisSlot(frame.getFrameDescriptor());
            thisSlotInitialized = true;
        }
        // if function has a <this> slot and its value is not undefined, we have a shortcut to `this`
        if (thisSlot != null) {
            Object maybeThis = frame.getValue(thisSlot);
            if (maybeThis != null && maybeThis != Undefined.instance) {
                return maybeThis;
            }
        }

        // otherwise, retrieve the current scope to look up this
        Iterable<Scope> scopes = env.findLocalScopes(context.getInstrumentedNode(), frame);
        assert scopes.iterator().hasNext();
        Object receiver = scopes.iterator().next().getReceiver();
        assert receiver != null;
        return receiver;
    }


    public boolean isRegularExpression() {
        return this.isRegExp;
    }

    public boolean isBuiltin() {
        return this.isBuiltin;
    }

    public Object getBuiltinName() {
        return this.isBuiltin ? this.builtinName : Undefined.instance;
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

    public String getFunctionName() {
        return this.funcName;
    }
}
