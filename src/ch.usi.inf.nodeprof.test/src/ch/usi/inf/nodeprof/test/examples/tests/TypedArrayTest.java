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

import ch.usi.inf.nodeprof.ProfiledTagEnum;
import ch.usi.inf.nodeprof.analysis.AnalysisFilterSourceList;
import ch.usi.inf.nodeprof.test.AnalysisEventsVerifier;
import ch.usi.inf.nodeprof.test.BasicAnalysisTest;
import ch.usi.inf.nodeprof.test.TestableNodeProfAnalysis;
import ch.usi.inf.nodeprof.test.examples.TypedArray;
import ch.usi.inf.nodeprof.utils.Logger;

public class TypedArrayTest extends BasicAnalysisTest {

    @Override
    public TestableNodeProfAnalysis getAnalysis(Instrumenter instrumenter, TruffleInstrument.Env env) {
        return new TypedArray(instrumenter, env);
    }

    @Test
    public void testBasic() {
        Logger.debug("testBasic");
        context.eval("js", "var a = new Array(); a[0]=1; a[1]={};");
        AnalysisEventsVerifier verifier = new AnalysisEventsVerifier(this.analysis.getAnalysisEvents()) {
            @Override
            public void verify() {
                dequeueAndVerifyEvent("TA_ARRAY_ALLOC", 1, ProfiledTagEnum.NEW);
                dequeueAndVerifyEvent("TA_EW_INT", 1, ProfiledTagEnum.ELEMENT_WRITE, 0L);
                dequeueAndVerifyEvent("TA_EW_INT", 1, ProfiledTagEnum.ELEMENT_WRITE, 1L);
                dequeueAndVerifyEvent("TA_UNTYPED", 1, ProfiledTagEnum.ELEMENT_WRITE);
                finish();
            }
        };

        verifier.verify();
    }

    @Test
    public void testBasic2() {
        Logger.debug("testBasic2");
        context.eval("js", "var b = Array(); b[0]=1; b[1]={};");
        AnalysisEventsVerifier verifier = new AnalysisEventsVerifier(this.analysis.getAnalysisEvents()) {
            @Override
            public void verify() {
                dequeueAndVerifyEvent("TA_ARRAY_ALLOC", 1, ProfiledTagEnum.INVOKE);
                dequeueAndVerifyEvent("TA_EW_INT", 1, ProfiledTagEnum.ELEMENT_WRITE, 0L);
                dequeueAndVerifyEvent("TA_EW_INT", 1, ProfiledTagEnum.ELEMENT_WRITE, 1L);
                dequeueAndVerifyEvent("TA_UNTYPED", 1, ProfiledTagEnum.ELEMENT_WRITE);
                finish();
            }
        };

        verifier.verify();
    }

    /**
     * depending on ArrayObjectsTest
     */
    @Test
    public void testPushPop() {
        Logger.debug("testPushPop");
        context.eval("js", "var a = [1,2,3]; a.push(1); a.pop(); ");
        AnalysisEventsVerifier verifier = new AnalysisEventsVerifier(this.analysis.getAnalysisEvents()) {
            @Override
            public void verify() {
                dequeueAndVerifyEvent("TA_ARRAY_ALLOC", 1, ProfiledTagEnum.LITERAL);
                dequeueAndVerifyEvent("TA_ARRAY_OP", 1, ProfiledTagEnum.BUILTIN, "Array.prototype.push");
                dequeueAndVerifyEvent("TA_ARRAY_OP", 1, ProfiledTagEnum.BUILTIN, "Array.prototype.pop");
                finish();
            }
        };
        verifier.verify();
    }

    @Override
    public AnalysisFilterSourceList getFilter() {
        return AnalysisFilterSourceList.getFilter(AnalysisFilterSourceList.ScopeEnum.all);
    }
}
