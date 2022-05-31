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

import ch.usi.inf.nodeprof.utils.Logger;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.NodeLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.function.FunctionBodyNode;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
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

    @Child private NodeLibrary nodeLibrary;
    private static final InteropLibrary INTEROP = InteropLibrary.getUncached();

    public FunctionRootEventHandler(EventContext context) {
        super(context, ProfiledTagEnum.ROOT);
    }

    @Override
    protected SourceSection getSourceSectionForIID() {
        return context.getInstrumentedNode().getRootNode().getSourceSection();
    }

    public Object getReceiver(VirtualFrame frame) {
        // otherwise, retrieve the current scope to look up this
        return getReceiverFromScope(frame.materialize());
    }

    @TruffleBoundary
    private Object getReceiverFromScope(MaterializedFrame frame) {
        if (nodeLibrary == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            nodeLibrary = NodeLibrary.getFactory().create(context.getInstrumentedNode());
            adoptChildren();
        }

        Object receiver = null;
        try {
            Object scope = nodeLibrary.getScope(context.getInstrumentedNode(), frame, true);
            receiver = INTEROP.readMember(scope, "this");
        } catch (UnknownIdentifierException | UnsupportedMessageException e) {
            Logger.error(e.getMessage());
            Logger.error(e.getStackTrace());
        }
        assert receiver != null;
        return receiver;
    }

    public boolean isRegularExpression() {
        return this.isRegExp;
    }

    public boolean isBuiltin() {
        return this.isBuiltin;
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

    /**
     * @return the source of the instrumented node (or its closest parent), or null if no source is
     *         available
     */
    @TruffleBoundary
    public Source getSource() {
        if (isRegularExpression() || this.isBuiltin) {
            return null;
        }

        Node n = context.getInstrumentedNode();
        while (n != null && !(n instanceof FunctionBodyNode)) {
            n = n.getParent();
        }

        if (n == null) {
            return null;
        }

        if (n.getSourceSection() == null) {
            return null;
        }

        return n.getSourceSection().getSource();
    }
}
