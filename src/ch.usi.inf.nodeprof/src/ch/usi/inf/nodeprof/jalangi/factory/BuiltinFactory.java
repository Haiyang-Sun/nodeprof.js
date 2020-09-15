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
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.BuiltinRootEventHandler;

public class BuiltinFactory extends AbstractFactory {

    final String builtinFilter;

    public BuiltinFactory(Object jalangiAnalysis, DynamicObject pre,
                    DynamicObject post, String builtinFilter) {
        super("builtin", jalangiAnalysis, pre, post);
        this.builtinFilter = builtinFilter;
    }

    @Override
    public BaseEventHandlerNode create(EventContext context) {
        return new BuiltinRootEventHandler(context) {
            @Child CallbackNode cbNode = new CallbackNode();

            @Child MakeArgumentArrayNode makeArgs = MakeArgumentArrayNodeGen.create(pre == null ? post : pre, 2, 0);

            final boolean isTarget = builtinFilter == null ? true : getBuiltinName().equals(builtinFilter);

            @Override
            public void executePre(VirtualFrame frame, Object[] inputs) throws InteropException {
                if (isTarget && pre != null) {
                    cbNode.preCall(this, jalangiAnalysis, pre, getBuiltinName(), getFunction(frame), getReceiver(frame), makeArgs.executeArguments(getArguments(frame)));
                }
            }

            @Override
            public void executePost(VirtualFrame frame, Object result,
                            Object[] inputs) throws InteropException {
                if (isTarget && post != null) {
                    cbNode.postCall(this, jalangiAnalysis, post, this.getBuiltinName(), getFunction(frame), getReceiver(frame), makeArgs.executeArguments(getArguments(frame)),
                                    convertResult(result),
                                    createWrappedException(null));
                }
            }

            @Override
            public void executeExceptional(VirtualFrame frame, Throwable exception) throws InteropException {
                if (isTarget && post != null) {
                    cbNode.postCall(this, jalangiAnalysis, post, this.getBuiltinName(), getFunction(frame), getReceiver(frame), makeArgs.executeArguments(getArguments(frame)),
                                    Undefined.instance,
                                    createWrappedException(null));

                }
            }
        };
    }
}
