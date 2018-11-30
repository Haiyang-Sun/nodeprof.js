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
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node.Child;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

import ch.usi.inf.nodeprof.ProfiledTagEnum;
import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.FunctionCallEventHandler;
import ch.usi.inf.nodeprof.utils.PropertyExistsNode;

public class InvokeFactory extends AbstractFactory {
    private final ProfiledTagEnum tag; // can be INVOKE or NEW

    // used to filter functions
    private final PropertyExistsNode preFilter;
    private final PropertyExistsNode postFilter;

    public InvokeFactory(Object jalangiAnalysis, ProfiledTagEnum tag, DynamicObject pre,
                    DynamicObject post) {
        super("invokeFun", jalangiAnalysis, pre, post, 8, 9);
        this.tag = tag;
        // TODO
        if (skipInputs) {
            setPreArguments(1, Undefined.instance);
            setPreArguments(2, Undefined.instance);
            setPreArguments(3, Undefined.instance);
            setPostArguments(1, Undefined.instance);
            setPostArguments(2, Undefined.instance);
            setPostArguments(3, Undefined.instance);
        }
        setPreArguments(6, 0);// functionIid
        setPreArguments(7, 0);// functionSid
        setPostArguments(7, 0);// functionIid
        setPostArguments(8, 0);// functionSid

        String preFilterKey = readString(pre, "filterKey");
        if (preFilterKey != null) {
            preFilter = PropertyExistsNode.create(preFilterKey);
        } else {
            preFilter = null;
        }

        String postFilterKey = readString(post, "filterKey");
        if (postFilterKey != null) {
            postFilter = PropertyExistsNode.create(postFilterKey);
        } else {
            postFilter = null;
        }
    }

    @Override
    public BaseEventHandlerNode create(EventContext context) {
        return new FunctionCallEventHandler(context, tag) {

            @Child MakeArgumentArrayNode makeArgs = MakeArgumentArrayNodeGen.create(pre == null ? post : pre, getOffSet(), 0);
            @Child DirectCallNode preCall = createPreCallNode();

            @Child DirectCallNode postCall = createPostCallNode();

            @Override
            public void executePre(VirtualFrame frame, Object[] inputs) {
                if (pre != null) {
                    if (!skipInputs) {
                        Object func = getFunction(inputs);
                        if (preFilter != null && !preFilter.executePropertyExists((TruffleObject) func))
                            return;
                        setPreArguments(1, func);
                        setPreArguments(2, getReceiver(inputs));
                        setPreArguments(3, makeArgs.executeArguments(inputs));
                    }
                    setPreArguments(0, getSourceIID());
                    setPreArguments(4, this.isNew());
                    setPreArguments(5, this.isInvoke());
                    directCall(preCall, true, getSourceIID());
                }
            }

            @Override
            public void executePost(VirtualFrame frame, Object result,
                            Object[] inputs) {
                if (post != null) {
                    if (!skipInputs) {
                        Object func = getFunction(inputs);
                        if (postFilter != null && !postFilter.executePropertyExists((TruffleObject) func))
                            return;
                        setPostArguments(1, func);
                        setPostArguments(2, getReceiver(inputs));
                        setPostArguments(3, makeArgs.executeArguments(inputs));
                    }
                    setPostArguments(0, getSourceIID());
                    setPostArguments(4, convertResult(result));
                    setPostArguments(5, this.isNew());
                    setPostArguments(6, this.isInvoke());
                    directCall(postCall, false, getSourceIID());
                }
            }

            @Override
            public int expectedNumInputs() {
                return skipInputs ? 0 : super.expectedNumInputs();
            }
        };
    }
}
