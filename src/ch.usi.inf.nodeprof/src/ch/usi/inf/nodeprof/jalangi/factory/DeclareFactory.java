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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.DeclareEventHandler;

public class DeclareFactory extends AbstractFactory {

    @TruffleBoundary
    public DeclareFactory(Object jalangiAnalysis, DynamicObject pre, DynamicObject post) {
        super("declare", jalangiAnalysis, pre, post);
    }

    @Override
    public BaseEventHandlerNode create(EventContext context) {
        return new DeclareEventHandler(context) {
            @Child CallbackNode cbNode = new CallbackNode();

            @Override
            public void executePre(VirtualFrame frame, Object[] inputs) throws InteropException {
                if (pre != null) {
                    checkForSymbolicLocation(context.getInstrumentedNode(), frame.getArguments());

                    cbNode.preCall(
                                    this,
                                    jalangiAnalysis,
                                    pre,
                                    getSourceIID(),
                                    getDeclareName(),
                                    getDeclareType(),
                                    isFunctionDeclaration() ? "FunctionDeclaration" : Undefined.instance);
                }
            }

            @Override
            public void executePost(VirtualFrame frame, Object result,
                            Object[] inputs) throws InteropException {
                if (post != null) {
                    checkForSymbolicLocation(context.getInstrumentedNode(), frame.getArguments());

                    cbNode.postCall(
                                    this,
                                    jalangiAnalysis,
                                    post,
                                    getSourceIID(),
                                    getDeclareName(),
                                    getDeclareType(),
                                    isFunctionDeclaration() ? "FunctionDeclaration" : Undefined.instance);
                }
            }

        };
    }

}
