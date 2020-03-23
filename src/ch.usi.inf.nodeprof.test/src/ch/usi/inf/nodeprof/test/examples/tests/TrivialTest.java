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
package ch.usi.inf.nodeprof.test.examples.tests;

import org.junit.Test;

import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;

import ch.usi.inf.nodeprof.analysis.AnalysisFilterSourceList;
import ch.usi.inf.nodeprof.test.BasicAnalysisTest;
import ch.usi.inf.nodeprof.test.TestableNodeProfAnalysis;
import ch.usi.inf.nodeprof.test.examples.TrivialAnalysis;
import ch.usi.inf.nodeprof.utils.Logger;

public class TrivialTest extends BasicAnalysisTest {

    @Override
    public TestableNodeProfAnalysis getAnalysis(Instrumenter instrumenter, TruffleInstrument.Env env) {
        return new TrivialAnalysis(instrumenter, env);
    }

    @Override
    public AnalysisFilterSourceList getFilter() {
        return AnalysisFilterSourceList.getFilter(AnalysisFilterSourceList.ScopeEnum.all);
    }

    /**
     * test regular expression()
     */
    @Test
    public void testRegExp() {
        Logger.debug("testRegExp");
        context.eval("js", "var reg = /(\\d*)(\\D*)/g; reg.exec(528);");
    }
}
