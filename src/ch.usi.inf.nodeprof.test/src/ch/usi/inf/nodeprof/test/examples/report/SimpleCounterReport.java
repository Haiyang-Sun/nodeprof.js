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
package ch.usi.inf.nodeprof.test.examples.report;

import ch.usi.inf.nodeprof.test.examples.report.ReportDB.ReportFactory;
import ch.usi.inf.nodeprof.utils.SourceMapping;

public class SimpleCounterReport extends Report {
    private int cnt = 0;

    public SimpleCounterReport(int iid) {
        super(iid);
    }

    public static SimpleCounterReport getFromDB(
                    ReportDB db, int iid) {
        if (!db.contains(iid)) {
            db.put(iid, new SimpleCounterReport(iid));
        }
        return (SimpleCounterReport) db.get(iid);
    }

    public int getCnt() {
        return cnt;
    }

    public void incre() {
        this.cnt++;
    }

    @Override
    public String report() {
        return SourceMapping.getLocationForIID(this.iid) + ": counter " + this.getCnt();
    }

    public static class SimleReportFactory implements ReportFactory {
        @Override
        public Report create(int iid) {
            return new SimpleCounterReport(iid);
        }
    }
}
