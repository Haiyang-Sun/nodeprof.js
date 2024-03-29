/* *****************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.PropertyReadEventHandler;

public class GetFieldFactory extends AbstractFactory {

    public GetFieldFactory(Object jalangiAnalysis, JSDynamicObject pre,
                           JSDynamicObject post) {
        super("getField", jalangiAnalysis, pre, post);
        // TODO
    }

    @Override
    public BaseEventHandlerNode create(EventContext context) {
        return new PropertyReadEventHandler(context) {
            @Child CallbackNode cbNode = new CallbackNode();

            @Override
            public void executePre(VirtualFrame frame, Object[] inputs) throws InteropException {
                if (pre != null) {
                    if (!this.isGlobal(inputs)) {
                        cbNode.preCall(this, jalangiAnalysis, pre, getSourceIID(), getReceiver(inputs), getProperty(), false, isOpAssign(), isMethodCall());
                    }
                }
            }

            @Override
            public void executePost(VirtualFrame frame, Object result,
                            Object[] inputs) throws InteropException {
                if (post != null) {
                    if (!this.isGlobal(inputs)) {
                        cbNode.postCall(this, jalangiAnalysis, post, getSourceIID(), getReceiver(inputs), getProperty(), convertResult(result), false, isOpAssign(), isMethodCall());
                    }
                }
            }
        };
    }

}
