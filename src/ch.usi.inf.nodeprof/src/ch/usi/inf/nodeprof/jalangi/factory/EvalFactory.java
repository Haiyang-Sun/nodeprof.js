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
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.EvalEventHandler;

public class EvalFactory extends AbstractFactory {

    private final boolean isInvoke;

    public EvalFactory(Object jalangiAnalysis, DynamicObject pre,
                    DynamicObject post, boolean isInvoke) {
        super("eval", jalangiAnalysis, pre, post);
        this.isInvoke = isInvoke;
    }

    @Override
    public BaseEventHandlerNode create(EventContext context) {
        return new EvalEventHandler(context) {
            @Child CallbackNode cbNode = new CallbackNode();
            @Node.Child MakeArgumentArrayNode makeArgs = isInvoke ? (MakeArgumentArrayNodeGen.create(pre == null ? post : pre, 1, 0)) : null;

            @Override
            public void executePre(VirtualFrame frame, Object[] inputs) throws InteropException {
                if (pre != null) {
                    if (!isInvoke) {
                        cbNode.preCall(this, jalangiAnalysis, pre, getSourceIID(), getCode(inputs));
                    } else {
                        inputs[1] = getCode(inputs);
                        cbNode.preCall(this, jalangiAnalysis, pre, getSourceIID(), inputs[0], Undefined.instance, makeArgs.executeArguments(inputs), false, false, 0, 0);
                    }
                }
            }

            @Override
            public void executePost(VirtualFrame frame, Object result,
                            Object[] inputs) throws InteropException {
                if (post != null) {
                    if (!isInvoke) {
                        cbNode.postCall(this, jalangiAnalysis, post, getSourceIID(), getCode(inputs), convertResult(result));
                    } else {
                        inputs[1] = getCode(inputs);
                        cbNode.postCall(this, jalangiAnalysis, post, getSourceIID(), inputs[0], Undefined.instance, makeArgs.executeArguments(inputs), convertResult(result), false, false, 0, 0);
                    }
                }
            }
        };
    }
}
