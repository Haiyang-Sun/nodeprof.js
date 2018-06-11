/*******************************************************************************
 * Copyright [2018] [Haiyang Sun, Università della Svizzera Italiana (USI)]
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
package ch.usi.inf.nodeprof.test.examples.tests;

import org.junit.Test;

import com.oracle.truffle.api.instrumentation.Instrumenter;

import ch.usi.inf.nodeprof.ProfiledTagEnum;
import ch.usi.inf.nodeprof.analysis.AnalysisSourceFilter;
import ch.usi.inf.nodeprof.test.AnalysisEventsVerifier;
import ch.usi.inf.nodeprof.test.BasicAnalysisTest;
import ch.usi.inf.nodeprof.test.TestableNodeProfAnalysis;
import ch.usi.inf.nodeprof.test.examples.BranchCoverage;

public class BCTest extends BasicAnalysisTest {

    @Override
    public TestableNodeProfAnalysis getAnalysis(Instrumenter _instrumenter) {
        return new BranchCoverage(_instrumenter, null);
    }

    @Test
    public void testBasic() {
        context.eval("js", "if(true) {}; if(!true){}");
        AnalysisEventsVerifier verifier = new AnalysisEventsVerifier(this.analysis.getAnalysisEvents()) {
            @Override
            public void verify() {
                this.dequeueAndVerifyEvent("BC", 1, ProfiledTagEnum.CF_COND, true);
                this.dequeueAndVerifyEvent("BC", 1, ProfiledTagEnum.CF_COND, false);
            }
        };
        verifier.verify();
    }

    @Override
    public AnalysisSourceFilter getFilter() {
        return AnalysisSourceFilter.makeSingleIncludeFilter("Unnamed");
    }
}