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
package ch.usi.inf.nodeprof.jalangi.factory;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

import ch.usi.inf.nodeprof.handlers.AsyncRootEventHandler;
import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;

public class AsyncRootFactory extends AbstractFactory {

    public AsyncRootFactory(Object jalangiAnalysis, DynamicObject post) {
        super("asyncroot", jalangiAnalysis, null, post, -1, 5);
    }

    @Override
    public BaseEventHandlerNode create(EventContext context) {
        return new AsyncRootEventHandler(context) {
            @Child MakeArgumentArrayNode makeArgs = MakeArgumentArrayNodeGen.create(post, 2, 0);
            @Child DirectCallNode postCall = createDirectCallNode(post);

            @Override
            public void executePost(VirtualFrame frame, Object result,
                            Object[] inputs) {
                if (post != null) {
                    setPostArguments(0, getSourceIID());
                    setPostArguments(1, convertResult(result));
                    setPostArguments(2, (getPromise(inputs)));
                    setPostArguments(3, false);
                    setPostArguments(4, Undefined.instance);
                    directCall(postCall, false, getSourceIID());
                }
            }

            @Override
            public void executeExceptional(VirtualFrame frame, Throwable exception) {
                if (post != null) {
                    Object exceptionValue = parseErrorObject(exception);
                    setPostArguments(0, getSourceIID());
                    setPostArguments(1, Undefined.instance);
                    setPostArguments(2, Undefined.instance);
                    setPostArguments(3, false);
                    setPostArguments(4, exceptionValue == null ? "Unknown Exception" : exceptionValue);

                    directCall(postCall, false, getSourceIID());
                }
            }
        };
    }
}
