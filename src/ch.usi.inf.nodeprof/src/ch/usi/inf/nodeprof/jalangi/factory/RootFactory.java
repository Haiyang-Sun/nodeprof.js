/*******************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package ch.usi.inf.nodeprof.jalangi.factory;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node.Child;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.FunctionRootEventHandler;
import ch.usi.inf.nodeprof.utils.Logger;
import ch.usi.inf.nodeprof.utils.PropertyExistsNode;

public class RootFactory extends AbstractFactory {

    private final DynamicObject stackEnter;
    private final DynamicObject stackExit;
    private final boolean needStack;

    private int stackDepth = 0;

    // used to filter functions
    @Child PropertyExistsNode preFilter;
    @Child PropertyExistsNode postFilter;

    public RootFactory(Object jalangiAnalysis, DynamicObject pre, DynamicObject post, DynamicObject stackEnter, DynamicObject stackExit) {
        super("function", jalangiAnalysis, pre, post, 4, 3);
        this.stackEnter = stackEnter;
        this.stackExit = stackExit;
        if (this.stackEnter != null || this.stackExit != null) {
            this.needStack = true;
        } else {
            this.needStack = false;
        }
        String preFilterKey = readString(pre, "filterKey");
        if (preFilterKey != null) {
            preFilter = PropertyExistsNode.create(preFilterKey);
        }

        String postFilterKey = readString(post, "filterKey");
        if (postFilterKey != null) {
            postFilter = PropertyExistsNode.create(postFilterKey);
        }
    }

    @Override
    public BaseEventHandlerNode create(EventContext context) {
        return new FunctionRootEventHandler(context) {
            @Child MakeArgumentArrayNode makeArgs = MakeArgumentArrayNodeGen.create(pre == null ? post : pre, 2, 0);
            @Child DirectCallNode preCall = createDirectCallNode(pre);
            @Child DirectCallNode postCall = createDirectCallNode(post);

            @Child DirectCallNode stackPreCall = createDirectCallNode(stackEnter);
            @Child DirectCallNode stackPostCall = createDirectCallNode(stackExit);

            @Override
            public void executePre(VirtualFrame frame, Object[] inputs) {
                if (isRegularExpression())
                    return;

                if (!this.isBuiltin && pre != null) {
                    Object func = getFunction(frame);
                    if (!needStack && preFilter != null && !preFilter.executePropertyExists((TruffleObject) func)) {
                        return;
                    }
                    setPreArguments(0, getSourceIID());
                    setPreArguments(1, func);
                    setPreArguments(2, getReceiver(frame));
                    setPreArguments(3, makeArgs.executeArguments(getArguments(frame)));

                    if (needStack) {
                        if (stackDepth == 0) {
                            directCall(stackPreCall, true, getSourceIID());
                        }
                        stackDepth++;
                    }
                    directCall(preCall, true, getSourceIID());
                }
            }

            @Override
            public void executePost(VirtualFrame frame, Object result,
                            Object[] inputs) {
                if (isRegularExpression())
                    return;

                if (!this.isBuiltin && post != null) {
                    Object func = getFunction(frame);
                    if (!needStack && postFilter != null && !postFilter.executePropertyExists((TruffleObject) func)) {
                        return;
                    }
                    setPostArguments(0, this.getSourceIID());
                    setPostArguments(1, convertResult(result));
                    setPostArguments(2, Undefined.instance);
                    directCall(postCall, false, getSourceIID());
                    if (needStack) {
                        stackDepth--;
                        if (stackDepth == 0) {
                            directCall(stackPostCall, false, getSourceIID());
                        }
                    }
                }
            }

            @Override
            public void executeExceptional(VirtualFrame frame, Throwable exception) {
                if (isRegularExpression())
                    return;
                if (!this.isBuiltin && post != null) {
                    Object func = getFunction(frame);
                    if (!needStack && postFilter != null && !postFilter.executePropertyExists((TruffleObject) func)) {
                        return;
                    }
                    Object exceptionValue = parseErrorObject(exception);
                    setPostArguments(0, getSourceIID());
                    setPostArguments(1, Undefined.instance);
                    setPostArguments(2, exceptionValue == null ? "Unknown Exception" : exceptionValue);
                    if (needStack) {
                        stackDepth--;
                        if (stackDepth == 0) {
                            directCall(stackPostCall, false, getSourceIID());
                        }
                    }
                    directCall(postCall, false, getSourceIID());
                }
            }
        };
    }
}
