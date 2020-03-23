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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Env;
import com.oracle.truffle.api.nodes.Node;

import ch.usi.inf.nodeprof.ProfiledTagEnum;
import ch.usi.inf.nodeprof.analysis.AnalysisFactory;
import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.BaseSingleTagEventHandler;
import ch.usi.inf.nodeprof.test.InputChecker;
import ch.usi.inf.nodeprof.test.TestableNodeProfAnalysis;
import ch.usi.inf.nodeprof.test.examples.report.Report;
import ch.usi.inf.nodeprof.test.examples.report.ReportDB;
import ch.usi.inf.nodeprof.test.examples.report.ReportDB.ReportFactory;
import ch.usi.inf.nodeprof.test.examples.report.ReportEntryNode;
import ch.usi.inf.nodeprof.test.examples.report.ReportEntryNodeGen;
import ch.usi.inf.nodeprof.utils.Logger;
import ch.usi.inf.nodeprof.utils.SourceMapping;

/**
 * Analysis which focuses on verifying the inputs of events
 *
 * @See InputChecker
 */
public final class EventLogger extends TestableNodeProfAnalysis {

    public EventLogger(Instrumenter instrumenter, Env env) {
        super("EventLogger", instrumenter, env);
    }

    private final ReportDB db = new ReportDB();

    @Override
    public boolean checkResult(Object result) {
        return db.getRecords().size() == 0;
    }

    @Override
    public void initCallbacks() {
        for (ProfiledTagEnum tag : ProfiledTagEnum.values()) {
            if (tag == ProfiledTagEnum.EXPRESSION) {
                continue;
            }
            this.onCallback(tag, new AnalysisFactory<BaseEventHandlerNode>() {
                public BaseEventHandlerNode create(EventContext context) {
                    return new BaseSingleTagEventHandler(context, tag) {
                        @Override
                        public void enter(VirtualFrame frame) {
                            addDebugEvent("ENTER", getSourceIID(), tag);
                        }

                        @Override
                        public void executePre(VirtualFrame frame, Object[] inputs) {
                            addDebugEvent("PRE", getSourceIID(), tag);
                        }

                        @Child ReportEntryNode reportNode = ReportEntryNodeGen.create(db, new ReportFactory() {
                            public Report create(int iid) {
                                return new EventReport(iid);
                            }
                        });

                        @Override
                        public void executePost(VirtualFrame frame, Object result, Object[] inputs) {
                            addDebugEvent("POST", getSourceIID(), tag);
                            if (!InputChecker.checkInput(tag, this, inputs)) {
                                EventReport report = (EventReport) reportNode.execute(getSourceIID());
                                report.addError(tag, context.getInstrumentedNode().getClass(), inputs);
                            }
                        }

                    };
                }
            });
        }
    }

    @Override
    public void onClear() {
        db.clear();
    }

    @Override
    public void printResult() {
        for (Entry<Integer, Report> entry : db.getRecords().entrySet()) {
            EventReport report = (EventReport) entry.getValue();
            Logger.info(Logger.printSourceSectionWithCode(SourceMapping.getSourceSectionForIID(entry.getKey())).append(report.report()));
        }
    }

    class EventReport extends Report {
        EventReport(int iid) {
            super(iid);
        }

        @TruffleBoundary
        public void addError(ProfiledTagEnum tag, Class<? extends Node> instrumentedNode, Object[] inputs) {
            if (!mapping.containsKey(tag)) {
                mapping.put(tag, new HashSet<EventLogger.EventReport.InputTypes>());
            }
            mapping.get(tag).add(new InputTypes(instrumentedNode, inputs));
        }

        class InputTypes {
            private final Class<? extends Node> nodeClass;
            private final Class<?>[] types;
            private final String sample;

            InputTypes(Class<? extends Node> instrumentedNode, Object[] inputs) {
                this.nodeClass = instrumentedNode;
                if (inputs == null) {
                    types = null;
                    this.sample = "";
                } else {
                    this.types = new Class<?>[inputs.length];
                    String s = "";
                    for (int i = 0; i < inputs.length; i++) {
                        if (inputs[i] == null) {
                            this.types[i] = null;
                        } else {
                            s += inputs[i];
                            this.types[i] = inputs[i].getClass();
                        }
                        if (i != inputs.length - 1) {
                            s += "/";
                        }
                    }
                    this.sample = s;
                }
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append("node:").append(nodeClass.getSimpleName()).append(",");
                if (types == null) {
                    return sb.toString();
                }

                for (int i = 0; i < types.length; i++) {
                    Class<?> type = types[i];
                    sb.append(i).append(":");
                    if (type != null) {
                        sb.append(type.getSimpleName());
                    } else {
                        sb.append("null");
                    }
                    if (i != types.length - 1) {
                        sb.append(",");
                    }
                }
                sb.append(", sample:").append(this.sample);
                return sb.toString();
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == null) {
                    return false;
                }
                if (obj instanceof InputTypes) {
                    if (this.nodeClass != ((InputTypes) obj).nodeClass) {
                        return false;
                    }
                    return Arrays.equals(this.types, ((InputTypes) obj).types);
                }
                return false;
            }

            @Override
            public int hashCode() {
                if (types == null) {
                    return 0;
                }
                int result = nodeClass.hashCode();
                for (Class<?> c : types) {
                    if (c == null) {
                        result += 1;
                    } else {
                        result += c.hashCode();
                    }
                }
                return result;
            }
        }

        HashMap<ProfiledTagEnum, HashSet<InputTypes>> mapping = new HashMap<>();

        @Override
        public String report() {
            StringBuilder sb = new StringBuilder();
            for (Entry<ProfiledTagEnum, HashSet<InputTypes>> entry : mapping.entrySet()) {
                for (InputTypes it : entry.getValue()) {
                    sb.append("\n\tIncorrect Input Number/Types for tag " + entry.getKey().name() + ": ");
                    sb.append(it.toString());
                }
            }
            return sb.toString();
        }
    }
}
