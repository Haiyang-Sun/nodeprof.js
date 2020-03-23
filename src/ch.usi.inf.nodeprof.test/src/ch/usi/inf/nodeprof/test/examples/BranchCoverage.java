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
import com.oracle.truffle.js.runtime.JSRuntime;

import ch.usi.inf.nodeprof.ProfiledTagEnum;
import ch.usi.inf.nodeprof.analysis.AnalysisFactory;
import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.ConditionalEventHandler;
import ch.usi.inf.nodeprof.test.TestableNodeProfAnalysis;
import ch.usi.inf.nodeprof.test.examples.report.Report;
import ch.usi.inf.nodeprof.test.examples.report.ReportDB;
import ch.usi.inf.nodeprof.test.examples.report.ReportEntryNode;
import ch.usi.inf.nodeprof.test.examples.report.ReportEntryNodeGen;
import ch.usi.inf.nodeprof.test.examples.report.SimpleCounterReport;
import ch.usi.inf.nodeprof.utils.Logger;

public class BranchCoverage extends TestableNodeProfAnalysis {

    private final ReportDB trueDB = new ReportDB();
    private final ReportDB falseDB = new ReportDB();

    public BranchCoverage(Instrumenter instrumenter, Env env) {
        super("branch-coverage", instrumenter, env);
    }

    @Override
    public void onClear() {
        trueDB.clear();
        falseDB.clear();
    }

    @Override
    public void initCallbacks() {
        this.onCallback(ProfiledTagEnum.CF_BRANCH, new AnalysisFactory<BaseEventHandlerNode>() {
            @Override
            public BaseEventHandlerNode create(EventContext context) {
                return new ConditionalEventHandler(context) {

                    @Child ReportEntryNode trueCounter = ReportEntryNodeGen.create(trueDB, new SimpleCounterReport.SimleReportFactory());
                    @Child ReportEntryNode falseCounter = ReportEntryNodeGen.create(falseDB, new SimpleCounterReport.SimleReportFactory());

                    @Override
                    public void executePost(VirtualFrame frame, Object result, Object[] inputs) {
                        if (JSRuntime.toBoolean(result)) {
                            addDebugEvent("BC", getSourceIID(), ProfiledTagEnum.CF_BRANCH, true);
                            ((SimpleCounterReport) (trueCounter.execute(getSourceIID()))).incre();
                        } else {
                            addDebugEvent("BC", getSourceIID(), ProfiledTagEnum.CF_BRANCH, false);
                            ((SimpleCounterReport) (falseCounter.execute(getSourceIID()))).incre();
                        }
                    }
                };
            }
        });
    }

    @Override
    public void printResult() {
        int idx = 0;
        Logger.info("BranchCoverage analysis finishes.");
        for (Entry<Integer, Report> entry : trueDB.getRecords().entrySet()) {
            Logger.info("BranchCoverage[" + idx++ + "]: True " + entry.getValue().report());
        }
        for (Entry<Integer, Report> entry : falseDB.getRecords().entrySet()) {
            Logger.info("BranchCoverage[" + idx++ + "]: False " + entry.getValue().report());
        }
    }
}
