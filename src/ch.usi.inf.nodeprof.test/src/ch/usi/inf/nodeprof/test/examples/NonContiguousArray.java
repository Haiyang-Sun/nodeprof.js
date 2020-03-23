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

import java.util.Map.Entry;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Env;
import com.oracle.truffle.js.runtime.builtins.JSArray;

import ch.usi.inf.nodeprof.ProfiledTagEnum;
import ch.usi.inf.nodeprof.analysis.AnalysisFactory;
import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.ElementWriteEventHandler;
import ch.usi.inf.nodeprof.test.TestableNodeProfAnalysis;
import ch.usi.inf.nodeprof.test.examples.nodes.GetArrayIndexNode;
import ch.usi.inf.nodeprof.test.examples.nodes.GetArraySizeNode;
import ch.usi.inf.nodeprof.test.examples.nodes.GetArraySizeNodeGen;
import ch.usi.inf.nodeprof.test.examples.report.Report;
import ch.usi.inf.nodeprof.test.examples.report.ReportDB;
import ch.usi.inf.nodeprof.test.examples.report.ReportEntryNode;
import ch.usi.inf.nodeprof.test.examples.report.ReportEntryNodeGen;
import ch.usi.inf.nodeprof.test.examples.report.SimpleCounterReport;
import ch.usi.inf.nodeprof.utils.Logger;

public class NonContiguousArray extends TestableNodeProfAnalysis {
    public NonContiguousArray(Instrumenter instrumenter, Env env) {
        super("NonContiguousArray", instrumenter, env);
    }

    private final ReportDB db = new ReportDB();

    @Override
    public void onClear() {
        this.db.clear();
    }

    @Override
    public void printResult() {
        int idx = 0;
        Logger.info("NonContiguousArray analysis finishes.");
        for (Entry<Integer, Report> entry : db.getRecords().entrySet()) {
            Logger.info("NonContiguousArray [" + idx++ + "]" + entry.getValue().report());
        }
    }

    @Override
    public void initCallbacks() {
        this.onCallback(ProfiledTagEnum.ELEMENT_WRITE, new AnalysisFactory<BaseEventHandlerNode>() {
            @Override
            public BaseEventHandlerNode create(EventContext context) {
                return new ElementWriteEventHandler(context) {

                    @Child ReportEntryNode getReport = ReportEntryNodeGen.create(db, new SimpleCounterReport.SimleReportFactory());

                    @Child GetArrayIndexNode toArrayIndex = new GetArrayIndexNode();

                    @Child GetArraySizeNode getArraySize = GetArraySizeNodeGen.create();

                    @Override
                    public void executePre(VirtualFrame frame, Object[] inputs) {
                        if (!JSArray.isJSArray(getReceiver(inputs))) {
                            addDebugEvent("EW_NONARRAY", getSourceIID(), ProfiledTagEnum.ELEMENT_WRITE);
                            return;
                        }

                        Object convertedIndex = toArrayIndex.execute(getProperty(inputs));

                        if (convertedIndex instanceof Long && ((Long) convertedIndex) >= 0) {
                            long idx = (long) convertedIndex;
                            long curSize = getArraySize.executeSize(getReceiver(inputs));

                            addDebugEvent("EW_ARRAY_INT", getSourceIID(), ProfiledTagEnum.ELEMENT_WRITE, idx, curSize);

                            if (idx > curSize) {
                                SimpleCounterReport report = (SimpleCounterReport) getReport.execute(this.getSourceIID());
                                report.incre();
                                addDebugEvent("REPORT", getSourceIID(), ProfiledTagEnum.ELEMENT_WRITE);
                            }
                        } else {
                            addDebugEvent("EW_ARRAY_ELSE", getSourceIID(), ProfiledTagEnum.ELEMENT_WRITE, convertedIndex);
                        }
                    }
                };
            }
        });
    }
}
