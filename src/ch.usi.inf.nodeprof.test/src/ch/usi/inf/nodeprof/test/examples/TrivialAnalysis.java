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
package ch.usi.inf.nodeprof.test.examples;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Env;

import ch.usi.inf.nodeprof.test.TestableNodeProfAnalysis;
import ch.usi.inf.nodeprof.utils.Logger;

public class TrivialAnalysis extends TestableNodeProfAnalysis {

    public TrivialAnalysis(Instrumenter instrumenter, Env env) {
        super("trivial", instrumenter, env);
    }

    @Override
    public void initCallbacks() {
        this.onAllCallback(new ExecutionEventNodeFactory() {
            public ExecutionEventNode create(EventContext context) {
                return new ExecutionEventNode() {
                    @Override
                    protected void onReturnValue(VirtualFrame frame, Object result) {
                        super.onReturnValue(frame, result);
                    }
                };
            }
        });
    }

    @Override
    public void onClear() {

    }

    @Override
    public void printResult() {
        Logger.info("Trivial analysis finishes.");
    }

}
