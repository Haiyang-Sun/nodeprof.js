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
package ch.usi.inf.nodeprof.jalangi.factory;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.control.ReturnException;
import com.oracle.truffle.js.nodes.control.YieldException;
import com.oracle.truffle.js.runtime.objects.Undefined;

import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.FunctionRootEventHandler;
import ch.usi.inf.nodeprof.utils.Logger;

public class RootFactory extends AbstractFactory {

    protected final TruffleInstrument.Env env;

    public RootFactory(Object jalangiAnalysis, DynamicObject pre, DynamicObject post, TruffleInstrument.Env env) {
        super("function", jalangiAnalysis, pre, post);
        this.env = env;
    }

    @Override
    public BaseEventHandlerNode create(EventContext context) {
        return new FunctionRootEventHandler(context) {
            @Child MakeArgumentArrayNode makeArgs = MakeArgumentArrayNodeGen.create(pre == null ? post : pre, 2, 0);
            @Child CallbackNode cbNode = new CallbackNode();

            @Override
            public void executePre(VirtualFrame frame, Object[] inputs) throws InteropException {
                if (isRegularExpression()) {
                    return;
                }

                if (!this.isBuiltin && pre != null) {
                    cbNode.preCall(this, jalangiAnalysis, pre, getSourceIID(), getFunction(frame), getReceiver(frame, env), makeArgs.executeArguments(getArguments(frame)));
                }
            }

            @Override
            public void executePost(VirtualFrame frame, Object result,
                            Object[] inputs) throws InteropException {
                if (isRegularExpression()) {
                    return;
                }

                if (!this.isBuiltin && post != null) {
                    cbNode.postCall(this, jalangiAnalysis, post, getSourceIID(), convertResult(result), createWrappedException(null));
                }
            }

            @Override
            public void executeExceptional(VirtualFrame frame, Throwable exception) throws InteropException {
                if (isRegularExpression()) {
                    return;
                }

                if (!this.isBuiltin && post != null) {
                    cbNode.postCall(this, jalangiAnalysis, post, getSourceIID(), Undefined.instance, createWrappedException(exception));
                }
            }

            @Override
            public void executeExceptionalCtrlFlow(VirtualFrame frame, Throwable exception,
                            Object[] inputs) throws InteropException {
                // ignore Truffle-internal control flow exceptions
                if (exception instanceof ReturnException) {
                    Object returnExceptionValue = ((ReturnException) exception).getResult();

                    if (returnExceptionValue != null) {
                        // ConstantReturnNode
                        executePost(frame, returnExceptionValue, inputs);
                    } else {
                        // FrameReturnNode
                        // TODO, ideally there should be some util function in JSFrameUtil to fetch
                        // the return slot's value
                        executePost(frame, getReturnValueFromFrameOrDefault(frame, Undefined.instance), inputs);
                    }
                    return;
                } else if (exception instanceof YieldException) {
                    executeExceptional(frame, exception);
                    return;
                } else {
                    Logger.error(this.getSourceIID(), "Unexpected control flow exception", exception.getClass());
                    executeExceptional(frame, exception);
                }
            }

        };
    }
}
