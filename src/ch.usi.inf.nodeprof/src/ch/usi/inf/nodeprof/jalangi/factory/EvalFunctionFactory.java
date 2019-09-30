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

import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.BuiltinRootEventHandler;

public class EvalFunctionFactory extends AbstractFactory {

    public EvalFunctionFactory(Object jalangiAnalysis, DynamicObject pre,
                    DynamicObject post) {
        super("evalfunc", jalangiAnalysis, pre, post, 1, 3);
    }

    @Override
    public BaseEventHandlerNode create(EventContext context) {
        return new BuiltinRootEventHandler(context) {
            @Child DirectCallNode preCall = createPreCallNode();
            @Child DirectCallNode postCall = createPostCallNode();
            @Child MakeArgumentArrayNode makeArgs = MakeArgumentArrayNodeGen.create(pre == null ? post : pre, 2, 0);

            final boolean isTarget = getBuiltinName().equals("Function");

            @Override
            public void executePre(VirtualFrame frame, Object[] inputs) {
                if (isTarget && pre != null) {
                    setPreArguments(0, makeArgs.executeArguments(getArguments(frame)));

                    directCall(preCall, true, getSourceIID());
                }
            }

            @Override
            public void executePost(VirtualFrame frame, Object result,
                            Object[] inputs) {
                if (isTarget && post != null) {
                    setPostArguments(0, makeArgs.executeArguments(getArguments(frame)));
                    setPostArguments(1, convertResult(result));
                    setPostArguments(2, createWrappedException(null));
                    directCall(postCall, false, getSourceIID());
                }
            }

            @Override
            public void executeExceptional(VirtualFrame frame, Throwable exception) {
                if (isTarget && post != null) {
                    setPostArguments(0, makeArgs.executeArguments(getArguments(frame)));
                    setPostArguments(1, Undefined.instance);
                    setPostArguments(2, createWrappedException(exception));
                    directCall(postCall, false, getSourceIID());
                }
            }
        };
    }
}
