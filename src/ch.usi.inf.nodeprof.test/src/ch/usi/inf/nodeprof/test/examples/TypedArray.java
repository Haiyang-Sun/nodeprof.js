/* *****************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Map.Entry;
import java.util.WeakHashMap;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Env;
import com.oracle.truffle.api.object.DynamicObject;

import ch.usi.inf.nodeprof.ProfiledTagEnum;
import ch.usi.inf.nodeprof.analysis.AnalysisFactory;
import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.ElementWriteEventHandler;
import ch.usi.inf.nodeprof.handlers.FunctionCallEventHandler;
import ch.usi.inf.nodeprof.handlers.FunctionRootEventHandler;
import ch.usi.inf.nodeprof.handlers.LiteralEventHandler;
import ch.usi.inf.nodeprof.test.TestableNodeProfAnalysis;
import ch.usi.inf.nodeprof.test.examples.nodes.GetArrayIndexNode;
import ch.usi.inf.nodeprof.test.examples.nodes.IsArrayFunctionNode;
import ch.usi.inf.nodeprof.test.examples.nodes.IsArrayFunctionNodeGen;
import ch.usi.inf.nodeprof.test.examples.report.Report;
import ch.usi.inf.nodeprof.test.examples.report.ReportDB;
import ch.usi.inf.nodeprof.test.examples.report.ReportDB.ReportFactory;
import ch.usi.inf.nodeprof.test.examples.report.ReportEntryNode;
import ch.usi.inf.nodeprof.test.examples.report.ReportEntryNodeGen;
import ch.usi.inf.nodeprof.utils.GlobalConfiguration;
import ch.usi.inf.nodeprof.utils.GlobalObjectCache;
import ch.usi.inf.nodeprof.utils.Logger;
import ch.usi.inf.nodeprof.utils.SourceMapping;

public class TypedArray extends TestableNodeProfAnalysis {
    private final ReportDB db = new ReportDB();
    private final WeakHashMap<Object, Integer> allocationTrack = new WeakHashMap<>();

    @TruffleBoundary
    public void trackAllocation(Object array, int iid) {
        allocationTrack.put(array, iid);
    }

    @TruffleBoundary
    public int getAllocation(Object array) {
        if (!allocationTrack.containsKey(array)) {
            return -1;
        }
        return allocationTrack.get(array);
    }

    public TypedArray(Instrumenter instrumenter, Env env) {
        super("TypedArray", instrumenter, env);
    }

    @Override
    public void printResult() {
        int idx = 0;
        int typedArray = 0;
        Logger.info("TypedArray analysis finishes.");
        for (Entry<Integer, Report> entry : db.getRecords().entrySet()) {
            TypedArrayReport report = (TypedArrayReport) entry.getValue();
            if (GlobalConfiguration.DEBUG) {
                Logger.debug("TypedArray-Arrays[" + idx++ + "]: " + entry.getValue().report());
            }
            if (report.isTyped && !report.creationOnly) {
                Logger.info("TypedArray[" + typedArray++ + "]: " + entry.getValue().report());
            }
        }
    }

    @Override
    public synchronized void onClear() {
        allocationTrack.clear();
        db.clear();
    }

    class TypedArrayReport extends Report {
        public boolean creationOnly = true;
        public boolean readOnly = true;
        public boolean isLiteral = false;
        public boolean isTyped = true;

        TypedArrayReport(int iid) {
            super(iid);
        }

        @Override
        public String report() {
            return SourceMapping.getLocationForIID(iid) + (isTyped ? " [Number]" : "");
        }
    }

    class TypedArrayFactory implements ReportFactory {
        @Override
        public TypedArrayReport create(int iid) {
            return new TypedArrayReport(iid);
        }

    }

    private AnalysisFactory<BaseEventHandlerNode> getInvokeOrNewFactory(boolean isInvoke) {
        ProfiledTagEnum tag = isInvoke ? ProfiledTagEnum.INVOKE : ProfiledTagEnum.NEW;
        return new AnalysisFactory<BaseEventHandlerNode>() {
            @Override
            public BaseEventHandlerNode create(
                            EventContext context) {
                return new FunctionCallEventHandler(context, tag) {
                    @Child ReportEntryNode getReportNode = ReportEntryNodeGen.create(db, new TypedArrayFactory());

                    @Child IsArrayFunctionNode arrayFunc = IsArrayFunctionNodeGen.create();

                    @Override
                    public void executePost(VirtualFrame frame,
                                    Object result, Object[] inputs) {
                        Object funcObj = getFunction(inputs);
                        if (funcObj instanceof DynamicObject) {
                            Object constructor = GlobalObjectCache.getInstance().getArrayConstructor((DynamicObject) funcObj);
                            if (funcObj == constructor) {
                                trackAllocation(result, getSourceIID());
                                addDebugEvent("TA_ARRAY_ALLOC", getSourceIID(), tag);
                                getReportNode.execute(this.getSourceIID());
                            }
                        }
                    }
                };
            }
        };
    }

    @Override
    public void initCallbacks() {

        this.onCallback(ProfiledTagEnum.INVOKE, getInvokeOrNewFactory(true));
        this.onCallback(ProfiledTagEnum.NEW, getInvokeOrNewFactory(false));

        this.onCallback(ProfiledTagEnum.LITERAL, new AnalysisFactory<BaseEventHandlerNode>() {
            @Override
            public LiteralEventHandler create(EventContext context) {
                return new LiteralEventHandler(context) {
                    @Child ReportEntryNode getReportNode = ReportEntryNodeGen.create(db, new TypedArrayFactory());

                    @Override
                    public void executePost(VirtualFrame frame,
                                    Object result, Object[] inputs) {
                        if (this.getLiteralType().equals("ArrayLiteral")) {
                            addDebugEvent("TA_ARRAY_ALLOC", getSourceIID(), ProfiledTagEnum.LITERAL);
                            trackAllocation(result, getSourceIID());
                            TypedArrayReport report = (TypedArrayReport) getReportNode.execute(this.getSourceIID());
                            report.isLiteral = true;
                        }
                    }
                };
            }
        });

        this.onCallback(ProfiledTagEnum.BUILTIN, new AnalysisFactory<BaseEventHandlerNode>() {
            @Override
            public BaseEventHandlerNode create(
                            EventContext context) {
                return new FunctionRootEventHandler(context) {
                    @Child ReportEntryNode getReportNode = ReportEntryNodeGen.create(db, new TypedArrayFactory());

                    final boolean arrayPrototype = this.funcName.startsWith("Array.prototype");
                    final boolean isArrayPush = this.funcName.equals("Array.prototype.push");
                    final boolean isArrayPop = this.funcName.equals("Array.prototype.pop");

                    @Override
                    public void executePost(VirtualFrame frame,
                                    Object result, Object[] inputs) {
                        if (this.arrayPrototype) {
                            int iid = getAllocation(getReceiver(frame));
                            if (iid >= 0) {
                                addDebugEvent("TA_ARRAY_OP", iid, ProfiledTagEnum.BUILTIN, this.funcName);
                                TypedArrayReport report = (TypedArrayReport) getReportNode.execute(iid);
                                report.creationOnly = false;
                                if (isArrayPush) {
                                    report.readOnly = false;
                                    if (report.isTyped) {
                                        Object firstArg = getArgument(frame, 0);
                                        if (!(firstArg instanceof Number)) {
                                            addDebugEvent("TA_UNTYPED", iid, ProfiledTagEnum.BUILTIN);
                                            report.isTyped = false;
                                        }
                                    }
                                }
                                if (isArrayPop) {
                                    report.readOnly = false;
                                }
                            }
                        }
                    }

                };
            }
        });

        this.onCallback(ProfiledTagEnum.ELEMENT_WRITE, new AnalysisFactory<BaseEventHandlerNode>() {

            @Override
            public ElementWriteEventHandler create(EventContext context) {

                return new ElementWriteEventHandler(context) {
                    @Child GetArrayIndexNode toArrayIndex = new GetArrayIndexNode();

                    @Child ReportEntryNode getReportNode = ReportEntryNodeGen.create(db, new TypedArrayFactory());

                    @Override
                    public void executePre(VirtualFrame frame,
                                    Object[] inputs) {
                        Object convertedIndex = toArrayIndex.execute(getProperty(inputs));

                        if (convertedIndex instanceof Long && ((Long) convertedIndex) >= 0) {
                            int iid = getAllocation(getReceiver(inputs));
                            if (iid <= 0) {
                                /**
                                 * the receiver is not allocated as an array and as a result is not
                                 * considered to be typed-able
                                 */
                                return;
                            }
                            addDebugEvent("TA_EW_INT", iid, ProfiledTagEnum.ELEMENT_WRITE, convertedIndex);
                            TypedArrayReport report = (TypedArrayReport) getReportNode.execute(iid);
                            report.creationOnly = false;
                            report.readOnly = false;
                            if (report.isTyped) {
                                if (!(getValue(inputs) instanceof Number)) {
                                    report.isTyped = false;
                                    addDebugEvent("TA_UNTYPED", iid, ProfiledTagEnum.ELEMENT_WRITE);
                                }
                            }
                        } else {
                            addDebugEvent("TA_EW_OTHER", getSourceIID(), ProfiledTagEnum.ELEMENT_WRITE);
                        }
                    }

                };
            }
        });
    }
}
