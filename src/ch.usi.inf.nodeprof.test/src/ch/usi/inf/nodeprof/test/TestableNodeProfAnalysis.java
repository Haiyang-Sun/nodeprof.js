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
package ch.usi.inf.nodeprof.test;

import java.util.ArrayList;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Env;

import ch.usi.inf.nodeprof.ProfiledTagEnum;
import ch.usi.inf.nodeprof.analysis.NodeProfAnalysis;
import ch.usi.inf.nodeprof.utils.GlobalConfiguration;
import ch.usi.inf.nodeprof.utils.Logger;

public abstract class TestableNodeProfAnalysis extends NodeProfAnalysis {

    public TestableNodeProfAnalysis(String name, Instrumenter instrumenter, Env env) {
        super(name, instrumenter, env);
    }

    private ArrayList<AnalysisEvent> analysisEvents = new ArrayList<>();

    public ArrayList<AnalysisEvent> getAnalysisEvents() {
        return this.analysisEvents;
    }

    private boolean isTestEnabled = false;

    public boolean isTestEnabled() {
        return this.isTestEnabled;
    }

    public void enableTest() {
        this.isTestEnabled = true;
    }

    public void disableTest() {
        this.isTestEnabled = false;
    }

    @TruffleBoundary
    protected void addDebugEvent(String eventName, int iid, ProfiledTagEnum tag, Object... data) {
        if (this.isTestEnabled()) {
            if (GlobalConfiguration.DEBUG) {
                Logger.debug(iid, eventName + "@" + tag.name());
                if (data != null) {
                    int idx = 0;
                    for (Object d : data) {
                        Logger.debug("\targ[" + idx++ + "]:" + d);
                    }
                }
            }

            this.analysisEvents.add(new AnalysisEvent(eventName, iid, tag, data));
        }

    }

    /**
     * an analysis event can be created in the analysis to mark some special events
     */
    public class AnalysisEvent {
        /**
         *
         * @param eventName can be used to identify the type of the event
         * @param iid tracks the node at which this event happens
         * @param tag tracks the tag of the Node
         * @param data keeps track of any extra data
         */
        public AnalysisEvent(String eventName, int iid, ProfiledTagEnum tag, Object... data) {
            this.eventName = eventName;
            this.tag = tag;
            this.data = data;
            this.iid = iid;
        }

        private String eventName;
        private int iid;
        private ProfiledTagEnum tag;
        private Object[] data;

        public int getIId() {
            return iid;
        }

        public String getEventName() {
            return eventName;
        }

        public ProfiledTagEnum getTag() {
            return this.tag;
        }

        public Object[] getData() {
            return data;
        }
    }
}
