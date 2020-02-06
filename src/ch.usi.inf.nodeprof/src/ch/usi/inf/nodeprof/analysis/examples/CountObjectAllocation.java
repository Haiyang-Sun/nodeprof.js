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
package ch.usi.inf.nodeprof.analysis.examples;

import java.util.Map.Entry;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Env;
import com.oracle.truffle.api.object.DynamicObject;

import ch.usi.inf.nodeprof.ProfiledTagEnum;
import ch.usi.inf.nodeprof.analysis.AnalysisFactory;
import ch.usi.inf.nodeprof.analysis.NodeProfAnalysis;
import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.FunctionCallEventHandler;
import ch.usi.inf.nodeprof.handlers.LiteralEventHandler;
import ch.usi.inf.nodeprof.utils.GlobalObjectCache;
import ch.usi.inf.nodeprof.utils.Logger;

public class CountObjectAllocation extends NodeProfAnalysis {

    public CountObjectAllocation(Instrumenter instrumenter, Env env) {
        super("CountObjectAllocation", instrumenter, env);
    }

    private final ReportDB db = new ReportDB();

    @Override
    public void onClear() {
        db.clear();
    }

    @Override
    public void printResult() {
        Logger.info("CountObjectAllocation analysis finishes.");
        for (Entry<Integer, Report> entry : db.getRecords().entrySet()) {
            Logger.info("CountObjectAllocation " + entry.getValue().report());
        }
    }

    private AnalysisFactory<BaseEventHandlerNode> getInvokeOrNewFactory(boolean isInvoke) {
        ProfiledTagEnum tag = isInvoke ? ProfiledTagEnum.INVOKE : ProfiledTagEnum.NEW;
        return new AnalysisFactory<BaseEventHandlerNode>() {
            @Override
            public BaseEventHandlerNode create(EventContext context) {
                return new FunctionCallEventHandler(context, tag) {

                    @Child ReportEntryNode getReport = ReportEntryNodeGen.create(db, new SimpleCounterReport.SimleReportFactory());

                    @Override
                    public void executePost(VirtualFrame frame, Object result,
                                    Object[] inputs) {
                        if (this.isNew()) {
                            SimpleCounterReport report = (SimpleCounterReport) (getReport.execute(this.getSourceIID()));
                            report.incre();
                        } else {
                            Object constructor = GlobalObjectCache.getInstance().getArrayConstructor((DynamicObject) getFunction(inputs));
                            if (getFunction(inputs) == constructor) {
                                SimpleCounterReport report = (SimpleCounterReport) (getReport.execute(this.getSourceIID()));
                                report.incre();
                            }
                        }
                    }
                };
            }
        };
    }

    @Override
    public void initCallbacks() {
        this.onCallback(ProfiledTagEnum.LITERAL, new AnalysisFactory<BaseEventHandlerNode>() {

            @Override
            public BaseEventHandlerNode create(EventContext context) {
                return new LiteralEventHandler(context) {

                    @Child ReportEntryNode getReport = ReportEntryNodeGen.create(db, new SimpleCounterReport.SimleReportFactory());

                    @Override
                    public void executePost(VirtualFrame frame, Object result,
                                    Object[] inputs) {
                        if (this.getLiteralType().equals("ArrayLiteral") || this.equals("ObjectLiteral")) {
                            SimpleCounterReport report = (SimpleCounterReport) (getReport.execute(this.getSourceIID()));
                            report.incre();
                        }
                    }
                };
            }
        });

        this.onCallback(ProfiledTagEnum.INVOKE, getInvokeOrNewFactory(true));

        this.onCallback(ProfiledTagEnum.NEW, getInvokeOrNewFactory(false));

    }

}