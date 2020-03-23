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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Assert;

import ch.usi.inf.nodeprof.ProfiledTagEnum;
import ch.usi.inf.nodeprof.test.TestableNodeProfAnalysis.AnalysisEvent;
import ch.usi.inf.nodeprof.utils.Logger;
import ch.usi.inf.nodeprof.utils.SourceMapping;

public abstract class AnalysisEventsVerifier {

    public AnalysisEventsVerifier(ArrayList<AnalysisEvent> events) {
        this.events = events;
    }

    public String dumpEvent(AnalysisEvent e) {
        String data2str = "";
        if (e.getData() != null && e.getData().length > 0) {
            for (int i = 0; i < e.getData().length; i++) {
                data2str += " " + i + ":" + e.getData()[i].toString();
            }
        }
        return e.getEventName() + " " + e.getTag().name() + " " + SourceMapping.getLocationForIID(e.getIId()) + data2str;
    }

    ArrayList<AnalysisEvent> events = new ArrayList<>();

    public void dequeueAndVerifyEvent(String eventName, int line, ProfiledTagEnum tag, Object... data) {
        AnalysisEvent head;
        do {
            Assert.assertTrue(!events.isEmpty());
            head = events.remove(0);
        } while (head.getTag() == ProfiledTagEnum.STATEMENT);
        assertEquals("expected event " + eventName + ", actual " + head.getEventName() + ".", eventName,
                        head.getEventName());
        int startLine = SourceMapping.getSourceSectionForIID(head.getIId()).getStartLine();
        assertEquals("expected line of code " + line + ", actual " + startLine + ".", line, startLine);
        assertEquals(tag, head.getTag());
        assertArrayEquals("expected data " + data + ", actual " + head.getData() + ".", data, head.getData());
    }

    public void printAll() {
        int cnt = 0;
        for (AnalysisEvent e : events) {
            Logger.info("events [" + cnt++ + "]: " + e.toString());
        }
    }

    public void finish() {
        assertEquals(events.size() + " events remaining. ", 0, events.size());
    }

    public abstract void verify();
}
