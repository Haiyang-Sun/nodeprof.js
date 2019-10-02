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
import ch.usi.inf.nodeprof.handlers.PropertyWriteEventHandler;

public class PutFieldFactory extends AbstractFactory {

    public PutFieldFactory(Object jalangiAnalysis, DynamicObject pre,
                    DynamicObject post) {
        super("putField", jalangiAnalysis, pre, post, 6, 6);
        // TODO
        setPreArguments(4, true); // isComputed
        setPostArguments(4, true); // isComputed
    }

    @Override
    public BaseEventHandlerNode create(EventContext context) {
        return new PropertyWriteEventHandler(context) {
            @Child DirectCallNode preCall = createPreCallNode();
            @Child DirectCallNode postCall = createPostCallNode();

            @Override
            public void executePre(VirtualFrame frame, Object[] inputs) {
                if (pre != null) {
                    if (!this.isGlobal(inputs)) {
                        setPreArguments(0, getSourceIID());
                        setPreArguments(1, getReceiver(inputs));
                        setPreArguments(2, getProperty());
                        setPreArguments(3, getValue(inputs));
                        setPreArguments(5, isOpAssign());
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
                        setPostArguments(3, getValue(inputs));
                        setPostArguments(5, isOpAssign());
                        directCall(postCall, false, getSourceIID());
                    }
                }
            }
        };
    }
}
