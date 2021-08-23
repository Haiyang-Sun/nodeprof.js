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
package ch.usi.inf.nodeprof.analysis;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.js.runtime.GraalJSException;

import ch.usi.inf.nodeprof.ProfiledTagEnum;
import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.utils.GlobalConfiguration;
import ch.usi.inf.nodeprof.utils.Logger;

public class ProfilerExecutionEventNode extends ExecutionEventNode {
    protected final EventContext context;
    protected final ProfiledTagEnum cb;
    @Child BaseEventHandlerNode child;
    int hasOnEnter = 0;
    /**
     * A flag to switch on/off the profiling analysis: true => enabled, false => disabled
     *
     * by default the instrumentation is on. It can be updated with
     * ProfilerExecutionEventNode.updateEnabled.
     *
     * After disabled, this class acts as an empty ExecutionEventNode which can be fully optimized
     * out by the compiler
     */
    @CompilationFinal private static boolean profilerEnabled = true;

    public static boolean getEnabled() {
        return profilerEnabled;
    }

    /**
     * @param value true to enable the profiler or false to disable
     */
    public static void updateEnabled(boolean value) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        profilerEnabled = value;
    }

    public ProfilerExecutionEventNode(ProfiledTagEnum cb, EventContext context,
                    BaseEventHandlerNode child) {
        this.context = context;
        this.cb = cb;
        this.cb.nodeCount++;
        this.child = child;
    }

    public EventContext getContext() {
        return context;
    }

    @Override
    protected void onInputValue(VirtualFrame frame, EventContext inputContext,
                    int inputIndex, Object inputValue) {
        if (!profilerEnabled) {
            return;
        }
        if (child.expectedNumInputs() < 0 || inputIndex < child.expectedNumInputs()) {
            // save input only necessary
            saveInputValue(frame, inputIndex, inputValue);
        }
        if (this.child.isLastIndex(getInputCount(), inputIndex)) {
            this.cb.preHitCount++;
            try {
                this.child.executePre(frame, child.expectedNumInputs() != 0 ? getSavedInputValues(frame) : null);

                // allow for handler changes after executePre/Post
                checkHandlerChanges();
            } catch (Throwable e) {
                reportError(null, e);
            }
        }
    }

    @Override
    protected void onEnter(VirtualFrame frame) {
        if (!profilerEnabled) {
            return;
        }

        hasOnEnter++;
        try {
            this.child.enter(frame);
            if (this.child.isLastIndex(getInputCount(), -1)) {
                this.cb.preHitCount++;
                this.child.executePre(frame, null);

                // allow for handler changes after executePre/Post
                checkHandlerChanges();
            }
        } catch (Throwable e) {
            reportError(null, e);
        }
    }

    @Override
    protected void onReturnValue(VirtualFrame frame, Object result) {
        if (!profilerEnabled) {
            return;
        }
        Object[] inputs = null;
        try {
            if (hasOnEnter > 0) {
                hasOnEnter--;
                this.cb.postHitCount++;
                inputs = child.expectedNumInputs() != 0 ? getSavedInputValues(frame) : null;
                this.child.executePost(frame, result, inputs);

                // allow for handler changes after executePre/Post
                checkHandlerChanges();
            }
        } catch (Throwable e) {
            reportError(inputs, e);
        }
    }

    @TruffleBoundary
    private void reportError(Object[] inputs, Throwable e) {
        if (e instanceof GraalJSException) {
            /*
             * Dump JS exception messages in the analysis callback and avoid dumping full Graal.js
             * stack trace. This helps to avoid showing the Graal.js internals when debugging a new
             * dynamic analysis.
             */
            Logger.reportJSException((GraalJSException) e);
            return;
        }

        Logger.error(context.getInstrumentedSourceSection(), this.cb + " inputs: " + (inputs == null ? "null" : inputs.length) + " exception: " + e.getMessage());
        if (inputs != null) {
            for (int i = 0; i < inputs.length; i++) {
                Logger.error(context.getInstrumentedSourceSection(),
                                "\targ[" + i + "]: " + inputs[i]);
            }
        }
        if (!GlobalConfiguration.IGNORE_JALANGI_EXCEPTION) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    @Override
    protected void onReturnExceptional(VirtualFrame frame, Throwable exception) {
        if (!profilerEnabled) {
            return;
        }

        Object[] inputs = null;
        try {
            if (hasOnEnter > 0) {
                hasOnEnter--;
                this.cb.exceptionHitCount++;
                if (exception instanceof ControlFlowException) {
                    inputs = child.expectedNumInputs() != 0 ? getSavedInputValues(frame) : null;
                    this.child.executeExceptionalCtrlFlow(frame, exception, inputs);
                } else {
                    this.child.executeExceptional(frame, exception);
                }

            }
        } catch (Throwable e) {
            reportError(inputs, e);
        }
    }

    public ProfiledTagEnum getType() {
        return this.cb;
    }

    private void checkHandlerChanges() {
        // check for handler changes
        BaseEventHandlerNode newChild = this.child.wantsToUpdateHandler();
        if (newChild == null) {
            removeInstrumentation();
        } else if (newChild != this.child) {
            updateChild(newChild);
        }
    }

    private void updateChild(BaseEventHandlerNode newChild) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.child = insert(newChild);
    }

    private void removeInstrumentation() {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        Logger.debug("Removing instrumentation for " + this.child.getClass().getTypeName() + " / " + this + " @ " + context.getInstrumentedNode());
        this.replace(new ExecutionEventNode() {
        }, "NodeProf instrumentation handler removed");
        this.cb.deactivatedCount++;
    }
}
