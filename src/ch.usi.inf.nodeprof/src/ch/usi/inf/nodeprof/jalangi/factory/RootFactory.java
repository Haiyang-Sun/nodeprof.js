/*******************************************************************************
 * Copyright [2018] [Haiyang Sun, Università della Svizzera Italiana (USI)]
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
import ch.usi.inf.nodeprof.handlers.FunctionRootEventHandler;

public class RootFactory extends AbstractFactory {

    public RootFactory(Object jalangiAnalysis, DynamicObject pre,
                    DynamicObject post) {
        super("function", jalangiAnalysis, pre, post);
    }

    @Override
    public BaseEventHandlerNode create(EventContext context) {
        return new FunctionRootEventHandler(context) {
            @Child MakeArgumentArrayNode makeArgs = MakeArgumentArrayNodeGen.create(pre == null ? post : pre, 2, 0);
            @Child DirectCallNode preCall = createPreCallNode();
            @Child DirectCallNode postCall = createPostCallNode();

            @Override
            public void executePre(VirtualFrame frame, Object[] inputs) {
                if (isRegularExpression())
                    return;
                if (pre == null) {
                    return;
                }
                directCall(preCall, new Object[]{jalangiAnalysis, pre,
                                getSourceIID(), getFunction(frame), getReceiver(frame),
                                makeArgs.executeArguments(getArguments(frame)), this.getBuiltinName()}, true);
            }

            @Override
            public void executePost(VirtualFrame frame, Object result,
                            Object[] inputs) {
                if (isRegularExpression())
                    return;
                if (post == null) {
                    return;
                }
                directCall(postCall, new Object[]{jalangiAnalysis, post,
                                getSourceIID(), convertResult(result)
                }, false);
            }

            @Override
            public void executeExceptional(VirtualFrame frame) {
                if (isRegularExpression())
                    return;
                if (post == null) {
                    return;
                }
                // TODO add real exceptions
                directCall(postCall, new Object[]{jalangiAnalysis, post,
                                getSourceIID(), Undefined.instance
                }, false);
            }
        };
    }
}
