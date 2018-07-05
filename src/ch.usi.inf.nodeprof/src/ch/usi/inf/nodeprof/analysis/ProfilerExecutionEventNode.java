/*******************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
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
package ch.usi.inf.nodeprof.analysis;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;

import ch.usi.inf.nodeprof.ProfiledTagEnum;
import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.utils.Logger;

public class ProfilerExecutionEventNode extends ExecutionEventNode {
    final protected EventContext context;
    final ProfiledTagEnum cb;
    @Child BaseEventHandlerNode child;
    int hasOnEnter = 0;

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
        saveInputValue(frame, inputIndex, inputValue);
        if (this.child.isLastIndex(getInputCount(), inputIndex)) {
            this.cb.preHitCount++;
            this.child.executePre(frame, getSavedInputValues(frame));
        }
    }

    @Override
    protected void onEnter(VirtualFrame frame) {
        hasOnEnter++;
        this.child.enter(frame);
        if (this.child.isLastIndex(getInputCount(), -1)) {
            this.cb.preHitCount++;
            this.child.executePre(frame, null);
        }
    }

    @Override
    protected void onReturnValue(VirtualFrame frame, Object result) {
        if (hasOnEnter > 0) {
            hasOnEnter--;
            this.cb.postHitCount++;
            Object[] inputs = getSavedInputValues(frame);
            try {
                this.child.executePost(frame, result, inputs);
            } catch (Exception e) {
                reportError(inputs, e);
            }
        }
    }

    @TruffleBoundary
    private void reportError(Object[] inputs, Exception e) {
        Logger.error(context.getInstrumentedSourceSection(), this.cb + " inputs: " + inputs.length);
        for (int i = 0; i < inputs.length; i++) {
            Logger.error(context.getInstrumentedSourceSection(),
                            "\targ[" + i + "]: " + inputs[i]);
        }
        e.printStackTrace();
        System.exit(-1);

    }

    @Override
    protected void onReturnExceptional(VirtualFrame frame, Throwable exception) {
        if (hasOnEnter > 0) {
            hasOnEnter--;
            this.cb.exceptionHitCount++;
            this.child.executeExceptional(frame, exception);
        }
    }

    public ProfiledTagEnum getType() {
        return this.cb;
    }
}
