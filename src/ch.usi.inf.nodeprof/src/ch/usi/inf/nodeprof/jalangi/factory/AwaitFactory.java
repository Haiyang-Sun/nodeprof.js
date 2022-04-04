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
package ch.usi.inf.nodeprof.jalangi.factory;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.builtins.JSPromise;

import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.CFBranchEventHandler;

import java.util.Stack;

public class AwaitFactory extends AbstractFactory {

    public AwaitFactory(Object jalangiAnalysis, DynamicObject pre, DynamicObject post) {
        super("await", jalangiAnalysis, pre, post);
    }

    @Override
    public BaseEventHandlerNode create(EventContext context) {
        return new CFBranchEventHandler(context) {
            @Child CallbackNode cbNode = new CallbackNode();

            @Override
            public void executePre(VirtualFrame frame, Object[] inputs) throws InteropException {
                if (!this.isAwaitNode()) {
                    return;
                }
                if (inputs == null || inputs.length == 0) {
                    return;
                }

                if (pre != null) {
                    if (inputs[0] == inputs[1] && JSPromise.isJSPromise(inputs[0])) {
                        // both inputs are the same promise: await this promise
                        storeAwaitValue(frame, inputs[0]);
                        cbNode.preCall(this, jalangiAnalysis, pre, getSourceIID(), assertGetInput(0, inputs, "awaited val"));
                        return;
                    } else if (inputs[0] != inputs[1] && JSPromise.isJSPromise(inputs[1])) {
                        // inputs[0] is some value that is awaited, and inputs[1] is the internal promise
                        storeAwaitValue(frame, inputs[0]);
                        cbNode.preCall(this, jalangiAnalysis, pre, getSourceIID(), assertGetInput(0, inputs, "awaited val"));
                        return;
                    }
                }
                if (post != null) {
                    if (!JSPromise.isJSPromise((inputs[1]))) {
                        // inputs[1] is the value returned by await
                        assert inputs[0] == null : "await return inputs[0] expected to be null";
                        Object awaitVal = loadAwaitValue(frame);
                        assert awaitVal != null;
                        cbNode.postCall(this, jalangiAnalysis, post, getSourceIID(),
                                        awaitVal,
                                        assertGetInput(1, inputs, "awaited ret"),
                                        JSPromise.isJSPromise(awaitVal) && JSPromise.isRejected((DynamicObject) awaitVal));
                        return;
                    }
                }
                assert false : "should not reach here";
            }
        };
    }

    private static final String AuxSlotKey = ":nodeprof:promise";

    private void storeAwaitValue(VirtualFrame frame, Object input) {
        assert input != null;
        int aux = getAuxSlot(frame.getFrameDescriptor());
        Object newStack = createIfNotStack(frame.getAuxiliarySlot(aux));
        if (newStack != null) {
            frame.setAuxiliarySlot(aux, newStack);
        }
        pushStackValue(frame.getAuxiliarySlot(aux), input);
    }

    private Object loadAwaitValue(VirtualFrame frame) {
        int aux = getAuxSlot(frame.getFrameDescriptor());
        return popStackValue(frame.getAuxiliarySlot(aux));
    }

    @TruffleBoundary
    private int getAuxSlot(FrameDescriptor descriptor) {
        return descriptor.findOrAddAuxiliarySlot(AuxSlotKey);
    }

    @TruffleBoundary
    private Object createIfNotStack(Object maybeStack) {
        if (!(maybeStack instanceof Stack)) {
            return new Stack<>();
        }
        return null;
    }

    @TruffleBoundary
    private void pushStackValue(Object stack, Object value) {
        @SuppressWarnings("unchecked")
        Stack<Object> s = (Stack<Object>) stack;
        s.push(value);
    }

    @TruffleBoundary
    private Object popStackValue(Object stack) {
        @SuppressWarnings("unchecked")
        Stack<Object> s = (Stack<Object>) stack;
        return s.pop();
    }
}
