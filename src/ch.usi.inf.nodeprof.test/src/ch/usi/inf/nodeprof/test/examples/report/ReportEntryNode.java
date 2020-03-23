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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import ch.usi.inf.nodeprof.test.examples.report.ReportDB.ReportFactory;

public abstract class ReportEntryNode extends Node {
    protected final ReportFactory factory;
    protected final ReportDB reports;

    public ReportEntryNode(ReportDB db, ReportFactory factory) {
        this.reports = db;
        this.factory = factory;
    }

    protected Report getFromDb(Integer iid) {
        Report res;
        if (!reports.contains(iid)) {
            res = factory.create(iid);
            reports.put(iid, res);
        } else {
            res = reports.get(iid);
        }
        return res;
    }

    public abstract Report execute(Integer source);

    /**
     * @param iid the instrumentation id
     * @param cached the cached instrumentaion id
     */
    @Specialization(guards = "iid == cached")
    public Report executeSource(Integer iid, @Cached("iid") Integer cached,
                    @Cached("getFromDb(iid)") Report result) {
        return result;
    }

    @Specialization
    public Report executeSource(Integer iid) {
        return getFromDb(iid);
    }
}
