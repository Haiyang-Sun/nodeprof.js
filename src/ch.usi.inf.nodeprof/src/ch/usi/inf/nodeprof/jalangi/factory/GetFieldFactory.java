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

import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.PropertyReadEventHandler;

public class GetFieldFactory extends AbstractFactory {

    public GetFieldFactory(Object jalangiAnalysis, DynamicObject pre,
                    DynamicObject post) {
        super("getField", jalangiAnalysis, pre, post, 6, 7);
        // TODO
        setPreArguments(3, false);// isComputed
        setPostArguments(4, false);// isComputed
    }

    @Override
    public BaseEventHandlerNode create(EventContext context) {
        return new PropertyReadEventHandler(context) {
            @Child DirectCallNode preCall = createPreCallNode();
            @Child DirectCallNode postCall = createPostCallNode();

            @Override
            public void executePre(VirtualFrame frame, Object[] inputs) {
                if (pre != null) {
                    if (!this.isGlobal(inputs)) {
                        setPreArguments(0, getSourceIID());
                        setPreArguments(1, getReceiver(inputs));
                        setPreArguments(2, getProperty());
                        setPreArguments(4, isOpAssign());
                        setPreArguments(5, isMethodCall());
                        directCall(preCall, true, getSourceIID());
                    }
                }
            }

            @Override
            public void executePost(VirtualFrame frame, Object result,
                            Object[] inputs) {
                if (post != null) {
                    if (!this.isGlobal(inputs)) {
                        setPostArguments(0, getSourceIID());
                        setPostArguments(1, getReceiver(inputs));
                        setPostArguments(2, getProperty());
                        setPostArguments(3, convertResult(result));
                        setPostArguments(5, isOpAssign());
                        setPostArguments(6, isMethodCall());
                        directCall(postCall, false, getSourceIID());
                    }
                }
            }
        };
    }

}
