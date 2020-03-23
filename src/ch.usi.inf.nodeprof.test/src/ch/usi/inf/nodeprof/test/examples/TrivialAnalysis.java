/* *****************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * *****************************************************************************/
package ch.usi.inf.nodeprof.test.examples;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Env;

import ch.usi.inf.nodeprof.ProfiledTagEnum;
import ch.usi.inf.nodeprof.test.TestableNodeProfAnalysis;
import ch.usi.inf.nodeprof.utils.GlobalConfiguration;
import ch.usi.inf.nodeprof.utils.Logger;

public class TrivialAnalysis extends TestableNodeProfAnalysis {

    public TrivialAnalysis(Instrumenter instrumenter, Env env) {
        super("trivial", instrumenter, env);
    }

    @Override
    @TruffleBoundary
    public void initCallbacks() {
        String tagsStr = System.getenv("NODEPROF_BENCH_TAG");
        final boolean saveInput = "true".equals(System.getenv("NODEPROF_SAVE_INPUT"));
        final boolean saveInputLocal = "true".equals(System.getenv("NODEPROF_LOCAL_INPUT"));
        final boolean useInput = saveInput && "true".equals(System.getenv("NODEPROF_USE_INPUT"));
        if (tagsStr == null || tagsStr.isEmpty()) {
            Logger.debug("No tag is provided in env NODEPROF_BENCH_TAG, run without instrumentation");
            return;
        }
        for (String tagStr : tagsStr.split(",")) {
            try {
                final ProfiledTagEnum tag = ProfiledTagEnum.valueOf(tagStr);
                Logger.debug("Tag enabled: " + tag);

                if (!saveInputLocal) {
                    this.onSingleTagCallback(tag.getTag(), new ExecutionEventNodeFactory() {
                        public ExecutionEventNode create(EventContext context) {
                            return new ExecutionEventNode() {
                                @Override
                                protected void onInputValue(VirtualFrame frame, EventContext inputContext, int inputIndex, Object inputValue) {
                                    if (saveInput && tag.getExpectedNumInputs() != 0) {
                                        saveInputValue(frame, inputIndex, inputValue);
                                    }
                                }

                                @Override
                                protected void onReturnValue(VirtualFrame frame, Object result) {
                                    if (useInput && tag.getExpectedNumInputs() != 0) {
                                        Object[] inputs = getSavedInputValues(frame);
                                        assert inputs.length < 100 : "avoid warning: unused inputs";
                                    }
                                }
                            };
                        }
                    });
                } else {
                    this.onSingleTagCallback(tag.getTag(), new ExecutionEventNodeFactory() {

                        public ExecutionEventNode create(EventContext context) {

                            return new ExecutionEventNode() {
                                @CompilationFinal Object[] inputs = null;

                                @Override
                                protected void onInputValue(VirtualFrame frame, EventContext inputContext, int inputIndex, Object inputValue) {
                                    if (saveInput && tag.getExpectedNumInputs() != 0) {
                                        if (tag.getExpectedNumInputs() > 0) {
                                            if (inputs == null) {
                                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                                inputs = new Object[tag.getExpectedNumInputs()];
                                            }
                                            if (inputIndex >= inputs.length) {
                                                return;
                                            }
                                        } else {
                                            if (inputs == null) {
                                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                                inputs = new Object[inputIndex + 1];
                                            } else if (inputs.length <= inputIndex) {
                                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                                Object[] newInputs = new Object[inputIndex + 1];
                                                for (int i = 0; i < inputs.length; i++) {
                                                    newInputs[i] = inputs[i];
                                                }
                                                inputs = newInputs;
                                            }
                                        }
                                        inputs[inputIndex] = inputValue;
                                    }
                                }

                                @Override
                                protected void onReturnValue(VirtualFrame frame, Object result) {
                                    if (useInput && tag.getExpectedNumInputs() != 0) {
                                        assert inputs.length < 100 : "avoid warning: unused inputs";
                                    }
                                }
                            };
                        }
                    });
                }
            } catch (Exception e) {
                Logger.error("Invalid tag given " + tagStr);
                System.exit(-1);
            }

        }
    }

    @Override
    public void onClear() {

    }

    @Override
    public void printResult() {
        if (GlobalConfiguration.DEBUG) {
            Logger.debug("Trivial analysis finishes.");
        }
    }

}
