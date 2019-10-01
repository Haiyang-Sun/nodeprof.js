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
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.CFRootEventHandler;

public class AsyncRootFactory extends AbstractFactory {

    public AsyncRootFactory(Object jalangiAnalysis, DynamicObject pre, DynamicObject post) {
        super("async-root", jalangiAnalysis, pre, post, 1, 3);
    }

    @Override
    public BaseEventHandlerNode create(EventContext context) {
        return new CFRootEventHandler(context) {
            @Child DirectCallNode preCall = createDirectCallNode(pre);
            @Child DirectCallNode postCall = createDirectCallNode(post);

            @Override
            public void executePre(VirtualFrame frame, Object[] inputs) {
                if (pre != null && this.isAsyncRoot()) {
                    setPreArguments(0, getSourceIID());
                    directCall(preCall, true, getSourceIID());
                }
            }

            @Override
            public void executePost(VirtualFrame frame, Object result,
                            Object[] inputs) {

                if (post != null && this.isAsyncRoot()) {
                    setPostArguments(0, this.getSourceIID());
                    assert (result instanceof DynamicObject);
                    setPostArguments(1, result);
                    setPostArguments(2, createWrappedException(null));
                    directCall(postCall, false, getSourceIID());
                }
            }

            @Override
            public void executeExceptional(VirtualFrame frame, Throwable exception) {
                if (post != null) {
                    setPostArguments(0, getSourceIID());
                    setPostArguments(1, Undefined.instance);
                    setPostArguments(2, createWrappedException(exception));
                    directCall(postCall, false, getSourceIID());
                }
            }
        };
    }
}