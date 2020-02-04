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
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;

import ch.usi.inf.nodeprof.ProfiledTagEnum;
import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.FunctionCallEventHandler;

public class InvokeFactory extends AbstractFactory {
    private final ProfiledTagEnum tag; // can be INVOKE or NEW

    public InvokeFactory(Object jalangiAnalysis, ProfiledTagEnum tag, DynamicObject pre,
                    DynamicObject post) {
        super("invokeFun", jalangiAnalysis, pre, post);
        this.tag = tag;
    }

    @Override
    public BaseEventHandlerNode create(EventContext context) {
        return new FunctionCallEventHandler(context, tag) {

            @Child MakeArgumentArrayNode makeArgs = MakeArgumentArrayNodeGen.create(pre == null ? post : pre, getOffSet(), 0);
            @Node.Child private InteropLibrary preDispatch = (pre == null) ? null : createDispatchNode();
            @Node.Child private InteropLibrary postDispatch = (post == null) ? null : createDispatchNode();

            @Override
            public void executePre(VirtualFrame frame, Object[] inputs) throws InteropException {
                if (pre != null) {
                    // TODO Jalangi's function iid/sid are set to be 0/0
                    wrappedDispatchExecution(preDispatch, pre, getSourceIID(), getFunction(inputs), getReceiver(inputs), makeArgs.executeArguments(inputs), isNew(), isInvoke(), 0, 0);
                }
            }

            @Override
            public void executePost(VirtualFrame frame, Object result,
                            Object[] inputs) throws InteropException {
                if (post != null) {
                    // TODO Jalangi's function iid/sid are set to be 0/0
                    wrappedDispatchExecution(postDispatch, post, getSourceIID(), getFunction(inputs), getReceiver(inputs), makeArgs.executeArguments(inputs), convertResult(result), isNew(), isInvoke(), 0, 0);
                }
            }
        };
    }
}
