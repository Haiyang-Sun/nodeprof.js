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

import java.util.HashMap;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

public class ReportDB {
    public interface ReportFactory {
        Report create(int iid);
    }

    private final HashMap<Integer, Report> records = new HashMap<>();

    @TruffleBoundary
    public Report get(Integer entryKey) {
        return records.get(entryKey);
    }

    @TruffleBoundary
    public Report put(Integer entryKey, Report value) {
        return records.put(entryKey, value);
    }

    @TruffleBoundary
    public void remove(Integer key) {
        records.remove(key);
    }

    @TruffleBoundary
    public boolean contains(Integer key) {
        return records.containsKey(key);
    }

    public HashMap<Integer, Report> getRecords() {
        return records;
    }

    @TruffleBoundary
    public void clear() {
        this.records.clear();
    }
}
