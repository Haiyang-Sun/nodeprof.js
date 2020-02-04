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
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.array.ArrayLengthNode.ArrayLengthReadNode;
import com.oracle.truffle.js.nodes.cast.ToArrayIndexNode;
import com.oracle.truffle.js.runtime.builtins.JSArray;

import ch.usi.inf.nodeprof.ProfiledTagEnum;
import ch.usi.inf.nodeprof.analysis.AnalysisFactory;
import ch.usi.inf.nodeprof.analysis.NodeProfAnalysis;
import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.ElementWriteEventHandler;
import ch.usi.inf.nodeprof.utils.Logger;

public class NonContiguousArray extends NodeProfAnalysis {
    public NonContiguousArray(Instrumenter instrumenter, Env env) {
        super("NonContiguousArray", instrumenter, env);
    }

    private final boolean empty = "true".equals(System.getenv("NODEPROFEMPTY"));

    private final ReportDB db = new ReportDB();

    @Override
    public void onClear() {
        this.db.clear();
    }

    @Override
    public void printResult() {
        if (empty)
            return;
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

                    @Override
                    public void executePre(VirtualFrame frame, Object[] inputs) {
                        if (empty)
                            return;
                        Object base = getReceiver(inputs);
                        if (isArray(base)) {
                            Object offset = getProperty(inputs);
                            if (getArrayLength(base) < getArrayIndex(offset)) {
                                reportOnIID(this.sourceIID);
                            }
                        }
                    }

                    @Child ReportEntryNode getReport = ReportEntryNodeGen.create(db, new SimpleCounterReport.SimleReportFactory());

                    @Child ToArrayIndexNode toArrayIndex = ToArrayIndexNode.createNoToPropertyKey();

                    @Child ArrayLengthReadNode getArraySize = ArrayLengthReadNode.create();

                    private boolean isArray(Object input) {
                        return JSArray.isJSArray(input);
                    }

                    private long getArrayIndex(Object value) {
                        Object convertedIndex = toArrayIndex.execute(value);
                        if (convertedIndex instanceof Long) {
                            return (long) convertedIndex;
                        }
                        return -1;
                    }

                    private int getArrayLength(Object arr) {
                        try {
                            return getArraySize.executeInt((DynamicObject) arr, true);
                        } catch (UnexpectedResultException e) {
                            return -1;
                        }
                    }

                    private void reportOnIID(long iid) {
                        SimpleCounterReport report = (SimpleCounterReport) getReport.execute(this.getSourceIID());
                        report.incre();
                    }

                };
            }
        });
    }
}
