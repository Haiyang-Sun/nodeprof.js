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
import ch.usi.inf.nodeprof.handlers.BinaryEventHandler;

public class BinaryFactory extends AbstractFactory {

    public BinaryFactory(Object jalangiAnalysis, DynamicObject pre,
                    DynamicObject post) {
        super("binary", jalangiAnalysis, pre, post, 7, 8);
        // TODO
        setPreArguments(4, false); // isOpAssign
        setPreArguments(5, false); // isSwitchCaseComparison
        setPreArguments(6, false); // isComputed
        setPostArguments(5, false); // isOpAssign
        setPostArguments(6, false); // isSwitchCaseComparison
        setPostArguments(7, false); // isComputed
    }

    @Override
    public BaseEventHandlerNode create(EventContext context) {
        return new BinaryEventHandler(context) {
            @Child DirectCallNode preCall = createPreCallNode();
            @Child DirectCallNode postCall = createPostCallNode();

            @Override
            public void executePre(VirtualFrame frame, Object[] inputs) {
                if (pre != null && !isLogic()) {
                    // the arguments array is shared among different nodes
                    setPreArguments(0, getSourceIID());
                    setPreArguments(1, getOp());
                    setPreArguments(2, getLeft(inputs));
                    setPreArguments(3, getRight(inputs));
                    directCall(preCall, true, getSourceIID());
                }
            }

            @Override
            public void executePost(VirtualFrame frame, Object result,
                            Object[] inputs) {
                if (post != null && !isLogic()) {
                    setPostArguments(0, getSourceIID());
                    setPostArguments(1, getOp());
                    setPostArguments(2, getLeft(inputs));
                    setPostArguments(3, getRight(inputs));
                    setPostArguments(4, convertResult(result));
                    directCall(postCall, false, getSourceIID());
                }
            }
        };
    }
}
