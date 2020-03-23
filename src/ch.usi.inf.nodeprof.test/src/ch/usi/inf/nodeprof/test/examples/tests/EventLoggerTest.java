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

import org.junit.After;
import org.junit.Test;

import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;

import ch.usi.inf.nodeprof.ProfiledTagEnum;
import ch.usi.inf.nodeprof.analysis.AnalysisFilterSourceList;
import ch.usi.inf.nodeprof.test.AnalysisEventsVerifier;
import ch.usi.inf.nodeprof.test.BasicAnalysisTest;
import ch.usi.inf.nodeprof.test.TestableNodeProfAnalysis;
import ch.usi.inf.nodeprof.test.examples.EventLogger;

/**
 * Tests for EventLogger
 */
public class EventLoggerTest extends BasicAnalysisTest {
    private EventLogger analysis = null;

    @Override
    public TestableNodeProfAnalysis getAnalysis(Instrumenter instrumenter, TruffleInstrument.Env env) {
        this.analysis = new EventLogger(instrumenter, env);
        return this.analysis;
    }

    @Test
    public void testPropertyRead() {
        /**
         * TODO, missing inputs for property read
         *
         * potential workaround: use the function call inputs
         */
        context.eval("js", "var a = {x:function(){}}; a.x();");
    }

    @Test
    public void testUnary() {
        /**
         * not node are not processed properly
         */
        context.eval("js", "var a = 2 != 1");
    }

    @Test
    public void testElementWrite() {
        /**
         * not node are not processed properly
         */
        context.eval("js", "var a = [1,2,3]; \n a[1] += 1");
    }

    @Test
    public void testNew() {
        /**
         * not node are not processed properly
         */
        context.eval("js", "function A() {};var a = {x:function(){return 1;}};new A(a.x(), a.x());");
    }

    @Test
    public void testSwitch() {
        context.eval("js", "var a = {x:1};var b = {x:1, y:2, z:3};switch (a.x){case b.x:break;case b.y:break;case b.z:break;}");
    }

    @Test
    public void testBinary() {
        context.eval("js", "var a = 42; var b = 1; var c = a + b;");
        AnalysisEventsVerifier verifier = new AnalysisEventsVerifier(this.analysis.getAnalysisEvents()) {
            @Override
            public void verify() {
                dequeueAndVerifyEvent("ENTER", 1, ProfiledTagEnum.ROOT);
                dequeueAndVerifyEvent("PRE", 1, ProfiledTagEnum.ROOT);

                dequeueAndVerifyEvent("ENTER", 1, ProfiledTagEnum.PROPERTY_WRITE);
                dequeueAndVerifyEvent("ENTER", 1, ProfiledTagEnum.LITERAL);
                dequeueAndVerifyEvent("PRE", 1, ProfiledTagEnum.LITERAL);
                dequeueAndVerifyEvent("POST", 1, ProfiledTagEnum.LITERAL);
                dequeueAndVerifyEvent("PRE", 1, ProfiledTagEnum.PROPERTY_WRITE);
                dequeueAndVerifyEvent("POST", 1, ProfiledTagEnum.PROPERTY_WRITE);

                dequeueAndVerifyEvent("ENTER", 1, ProfiledTagEnum.PROPERTY_WRITE);
                dequeueAndVerifyEvent("ENTER", 1, ProfiledTagEnum.LITERAL);
                dequeueAndVerifyEvent("PRE", 1, ProfiledTagEnum.LITERAL);
                dequeueAndVerifyEvent("POST", 1, ProfiledTagEnum.LITERAL);
                dequeueAndVerifyEvent("PRE", 1, ProfiledTagEnum.PROPERTY_WRITE);
                dequeueAndVerifyEvent("POST", 1, ProfiledTagEnum.PROPERTY_WRITE);

                dequeueAndVerifyEvent("ENTER", 1, ProfiledTagEnum.PROPERTY_WRITE);
                dequeueAndVerifyEvent("ENTER", 1, ProfiledTagEnum.BINARY);

                dequeueAndVerifyEvent("ENTER", 1, ProfiledTagEnum.PROPERTY_READ);
                dequeueAndVerifyEvent("PRE", 1, ProfiledTagEnum.PROPERTY_READ);
                dequeueAndVerifyEvent("POST", 1, ProfiledTagEnum.PROPERTY_READ);

                dequeueAndVerifyEvent("ENTER", 1, ProfiledTagEnum.PROPERTY_READ);
                dequeueAndVerifyEvent("PRE", 1, ProfiledTagEnum.PROPERTY_READ);
                dequeueAndVerifyEvent("POST", 1, ProfiledTagEnum.PROPERTY_READ);

                dequeueAndVerifyEvent("PRE", 1, ProfiledTagEnum.BINARY);
                dequeueAndVerifyEvent("POST", 1, ProfiledTagEnum.BINARY);

                dequeueAndVerifyEvent("PRE", 1, ProfiledTagEnum.PROPERTY_WRITE);
                dequeueAndVerifyEvent("POST", 1, ProfiledTagEnum.PROPERTY_WRITE);

                dequeueAndVerifyEvent("POST", 1, ProfiledTagEnum.ROOT);

                finish();
            }
        };
        verifier.verify();
    }

    @Test
    public void testIncDec() {
        context.eval("js", "var a = {x:42}; a.x--;");
    }

    @Test
    public void testPlusEqual() {
        context.eval("js", "function foo(a){var b = 0; b+=a.x;}; foo({x:42});");
    }

    @Test
    public void testEval() {
        context.eval("js", "eval(\"var a = 0;\");");
    }

    /**
     * TODO
     *
     * bug fix: remove the idle input slot
     */
    @Test
    public void testPRInvoke() {
        context.eval("js", "var a = {x:[function(){}]}; a.x[0]()");
    }

    @Test
    public void testIncreElementWrite() {
        context.eval("js", "var u=[2,4,6]; var p = 0; u[p] -= 42");

    }

    @Override
    public AnalysisFilterSourceList getFilter() {
        return AnalysisFilterSourceList.makeSingleIncludeFilter("Unnamed");
    }

    @After
    public void check() {
        assert (this.analysis.checkResult(null));
    }
}
