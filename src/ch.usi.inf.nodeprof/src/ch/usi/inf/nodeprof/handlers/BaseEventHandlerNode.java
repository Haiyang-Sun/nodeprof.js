/*******************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.Undefined;

import ch.usi.inf.nodeprof.utils.GlobalConfiguration;
import ch.usi.inf.nodeprof.utils.Logger;
import ch.usi.inf.nodeprof.utils.SourceMapping;

import static com.oracle.truffle.js.runtime.Strings.REQUIRE_PROPERTY_NAME;

/**
 *
 * BaseEventHandlerNode defines the common methods needed to handle an event
 *
 */
public abstract class BaseEventHandlerNode extends Node {
    protected final EventContext context;
    @CompilationFinal private FrameSlot returnSlot;
    @CompilationFinal private boolean noReturnSlot = false;
    @CompilationFinal private boolean deactivated = false;

    public Object getReturnValueFromFrameOrDefault(VirtualFrame frame, Object defaultValue) {
        // cache the frame slot for the return value
        if (returnSlot == null && !noReturnSlot) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            returnSlot = frame.getFrameDescriptor().findFrameSlot("<return>");
            if (returnSlot == null) {
                Logger.warning("Could not find <return> slot");
                noReturnSlot = true;
            }
        }
        if (noReturnSlot) {
            return defaultValue;
        }
        return frame.getValue(returnSlot);
    }

    /**
     * the unique instrumentation ID for the instrumented source section
     */
    protected final int sourceIID;

    public BaseEventHandlerNode(EventContext context) {
        this.context = context;
        this.sourceIID = SourceMapping.getIIDForSourceSection(getSourceSectionForIID());
    }

    /**
     * @return the instrumented source section
     */
    private SourceSection getInstrumentedSourceSection() {
        return this.context.getInstrumentedSourceSection();
    }

    /**
     * @return the source section to be used for reporting purposes
     */
    protected SourceSection getSourceSectionForIID() {
        return this.context.getInstrumentedSourceSection();
    }

    /**
     * @return the instrumentation ID for the instrumented source section
     */
    public int getSourceIID() {
        return sourceIID;
    }

    @SuppressWarnings(value = {"unused"})
    public void enter(VirtualFrame frame) {
    }

    /**
     * @param frame the current virtual frame
     * @param inputs the input array get from ExecutionEventNode.getSavedInputValues()
     * @throws Exception
     */
    public void executePre(VirtualFrame frame, Object[] inputs) throws Exception {

    }

    /**
     * @param frame the current virtual frame
     * @param result of the execution of the instrumented node
     * @param inputs the input array get from ExecutionEventNode.getSavedInputValues()
     */
    public void executePost(VirtualFrame frame, Object result, Object[] inputs) throws Exception {

    }

    public void executeExceptional(@SuppressWarnings("unused") VirtualFrame frame, @SuppressWarnings("unused") Throwable exception) throws Exception {

    }

    /**
     * Control flow exception that may be treated like executePost, thus includes inputs[]
     *
     * @throws Exception
     */
    public void executeExceptionalCtrlFlow(VirtualFrame frame, Throwable exception, @SuppressWarnings("unused") Object[] inputs) throws Exception {
        executeExceptional(frame, exception);
    }

    public TruffleString getAttributeTString(String key) {
        Object result = getAttribute(key);
        assert Strings.isTString(result);
        return (TruffleString) result;
    }

    public String getAttributeInternalString(String key) {
        Object result = getAttribute(key);
        assert result instanceof String;
        return (String) result;
    }

    public TruffleString getAttributeConvertTString(String key) {
        Object result = getAttribute(key);
        assert result instanceof String;
        return Strings.fromJavaString((String) result);
    }

    /**
     *
     * get the node-specific attribute, in case of missing such attributes report an error
     *
     * @param key of the current InstrumentableNode
     * @return the value of this key
     */
    @TruffleBoundary
    public Object getAttribute(String key) {
        Object result = null;
        try {
            result = InteropLibrary.getFactory().getUncached().readMember(((InstrumentableNode) context.getInstrumentedNode()).getNodeObject(), key);
        } catch (Exception e) {
            reportAttributeMissingError(key, e);
        }
        return result;
    }

    /**
     *
     * get the node-specific attribute, in case of missing such attributes, return null
     *
     * @param key of the current InstrumentableNode
     * @return the value of this key or null if it does not exist
     */
    public Object getAttributeOrNull(String key) {
        if (!InteropLibrary.getFactory().getUncached().isMemberReadable(((InstrumentableNode) context.getInstrumentedNode()).getNodeObject(), key)) {
            return null;
        }
        Object result = null;
        try {
            result = InteropLibrary.getFactory().getUncached().readMember(((InstrumentableNode) context.getInstrumentedNode()).getNodeObject(), key);
        } catch (Exception e) {
            reportAttributeMissingError(key, e);
        }
        return result;
    }

    @TruffleBoundary
    private void reportAttributeMissingError(String key, Exception e) {
        Logger.error(getInstrumentedSourceSection(), "attribute " + key + " doesn't exist " + context.getInstrumentedNode().getClass().getSimpleName());
        e.printStackTrace();
        if (!GlobalConfiguration.IGNORE_JALANGI_EXCEPTION) {
            Thread.dumpStack();
            System.exit(-1);
        }
    }

    /**
     * retrieve the real value from the inputs with exception handler
     *
     * @param index
     * @param inputs
     * @param inputHint
     * @return the value of inputs[index]
     */
    protected Object assertGetInput(int index, Object[] inputs, String inputHint) {
        if (inputs == null) {
            reportInputsError(index, null, "InputsArrayNull", inputHint);
            return Undefined.instance;
        }
        if (index < inputs.length) {
            Object result = inputs[index];
            if (result == null) {
                result = Undefined.instance;
                reportInputsError(index, inputs, "InputElementNull", inputHint);
            }
            return result;
        } else {
            /**
             * if the inputs are not there, report the detail and stop the engine.
             */
            reportInputsError(index, inputs, "MissingInput", inputHint);
        }
        return Undefined.instance;
    }

    protected TruffleString assertGetStringInput(int index, Object[] inputs, String inputHint) {
        Object input = assertGetInput(index, inputs, inputHint);
        if (input instanceof String) {
            Logger.warning("Input not TruffleString but String");
            return Strings.fromJavaString((String) input);
        } else if (input instanceof TruffleString) {
            return (TruffleString) input;
        }
        reportInputsError(1, inputs, "ExpectedStringLike", inputHint);
        return null;
    }

    @TruffleBoundary
    private void reportInputsError(int index, Object[] inputs, String info, String inputHint) {
        Logger.error(context.getInstrumentedSourceSection(),
                        "Error[" + info + "] getting input (" + inputHint + ") at index '" + index + "' from " +
                                        context.getInstrumentedNode().getClass().getSimpleName() + " (has " + (inputs == null ? 0 : inputs.length) + " input(s))");

        if (!GlobalConfiguration.IGNORE_JALANGI_EXCEPTION) {
            Thread.dumpStack();
            System.exit(-1);
        }
    }

    public abstract int expectedNumInputs();

    @SuppressWarnings("unused")
    public Object onUnwind(VirtualFrame frame, Object info) {
        return null;
    }

    /**
     * used to determinate whether the necessary inputs are all collected
     *
     * @return the index or -1 if no inputs are needed
     */
    public final boolean isLastIndex(int inputCount, int index) {
        int expected = expectedNumInputs();
        assert inputCount >= expected : Logger.printSourceSectionWithCode(this.getInstrumentedSourceSection()).append(context.getInstrumentedNode().getClass()).append(" ").append(inputCount).append(
                        " < ").append(expected + " ").toString();
        if (expected == -1) {
            // not sure how many inputs
            return index == inputCount - 1;
        } else {
            return index == expected - 1;
        }
    }

    /**
     * Can be overridden to avoid multiple handlers, which rely on the same instrumentation tag (see
     * {@link MultiEventHandler}), to execute in arbitrary order. A lower integer means higher
     * priority.
     *
     * @return the priority value of the handler
     */
    public int getPriority() {
        return 0;
    }

    /**
     * Allows handlers to ask for eventual replacement or removal.
     *
     * @return <code>null</code> to remove this handler, another instance of
     *         {@link BaseEventHandlerNode} to replace this handler with, or <code>this</code> to
     *         continue without change.
     */
    public BaseEventHandlerNode wantsToUpdateHandler() {
        return deactivated ? null : this;
    }

    public void deactivate() {
        CompilerAsserts.neverPartOfCompilation();
        deactivated = true;
    }

    private static boolean isModuleInvocation(Object[] args) {
        if (args.length != 7) {
            return false;
        }
        if (JSFunction.isJSFunction(args[3])) {
            return REQUIRE_PROPERTY_NAME.equals(JSFunction.getName((DynamicObject) args[3]));
        }
        return false;
    }

    @TruffleBoundary
    protected static void checkForSymbolicLocation(Node node, Object[] args) {
        if (GlobalConfiguration.SYMBOLIC_LOCATIONS) {
            RootNode root = node.getRootNode();
            assert root != null;
            if (":program".equals(root.getName())) {
                SourceMapping.addSyntheticLocation(root.getSourceSection(), ":program");
            } else if (SourceMapping.isModuleOrWrapper(root.getSourceSection()) && isModuleInvocation(args)) {
                SourceMapping.addSyntheticLocation(root.getSourceSection(), "module");
            }
        }
    }
}
