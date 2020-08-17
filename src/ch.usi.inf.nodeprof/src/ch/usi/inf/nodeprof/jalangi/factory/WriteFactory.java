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
import ch.usi.inf.nodeprof.handlers.PropertyWriteEventHandler;
import ch.usi.inf.nodeprof.handlers.VarWriteEventHandler;

public class WriteFactory extends AbstractFactory {

    private final boolean isProperty;

    public WriteFactory(Object jalangiAnalysis, DynamicObject post,
                    boolean isProperty) {
        super("write", jalangiAnalysis, null, post);
        this.isProperty = isProperty;
    }

    @Override
    public BaseEventHandlerNode create(EventContext context) {
        if (!isProperty) {
            return new VarWriteEventHandler(context) {
                @Child CallbackNode cbNode = new CallbackNode();

                @Override
                public void executePost(VirtualFrame frame, Object result,
                                Object[] inputs) throws InteropException {
                    if (post == null) {
                        return;
                    }
                    // TODO: the value before write is set to be Undefined and isScriptLocal is
                    // always true
                    cbNode.postCall(this, jalangiAnalysis, post, getSourceIID(), getName(), getValue(inputs), Undefined.instance, false, true);
                }
            };
        } else {
            return new PropertyWriteEventHandler(context) {
                @Child CallbackNode cbNode = new CallbackNode();

                @Override
                public void executePost(VirtualFrame frame, Object result,
                                Object[] inputs) throws InteropException {
                    if (post == null) {
                        return;
                    }
                    if (isGlobal(inputs)) {
                        // TODO: the value before write is set to be Undefined and isScriptLocal is
                        // always true
                        cbNode.postCall(this, jalangiAnalysis, post, getSourceIID(), getProperty(), getValue(inputs), Undefined.instance, true, true);
                    }
                }
            };
        }
    }

}
