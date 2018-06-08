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

import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.PropertyReadEventHandler;

public class GetFieldFactory extends AbstractFactory {

    public GetFieldFactory(Object jalangiAnalysis, DynamicObject pre,
                    DynamicObject post) {
        super("getField", jalangiAnalysis, pre, post);
    }

    @Override
    public BaseEventHandlerNode create(EventContext context) {
        return new PropertyReadEventHandler(context) {
            @Child DirectCallNode preCall = createPreCallNode();
            @Child DirectCallNode postCall = createPostCallNode();

            @Override
            public void executePre(VirtualFrame frame, Object[] inputs) {
                if (pre == null) {
                    return;
                }
                if (!this.isGlobal(inputs)) {
                    directCall(preCall, new Object[]{jalangiAnalysis, pre,
                                    getSourceIID(), getReceiver(inputs), getProperty(),
                                    false,// isComputed
                                    isOpAssign(),// isOpAssign
                                    isMethodCall(),// isMethodCall
                    }, true);
                }
            }

            @Override
            public void executePost(VirtualFrame frame, Object result,
                            Object[] inputs) {
                if (post == null) {
                    return;
                }
                if (!this.isGlobal(inputs)) {
                    directCall(postCall, new Object[]{jalangiAnalysis, post,
                                    getSourceIID(), getReceiver(inputs), getProperty(),
                                    convertResult(result), false,// isComputed
                                    isOpAssign(),// isOpAssign
                                    isMethodCall(),// isMethodCall
                    }, false);
                }
            }
        };
    }

}
