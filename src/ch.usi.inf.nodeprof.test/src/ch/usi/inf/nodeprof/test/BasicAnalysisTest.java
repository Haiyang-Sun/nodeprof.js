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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.graalvm.polyglot.Context;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.oracle.js.parser.Source;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.js.lang.JavaScriptLanguage;

import ch.usi.inf.nodeprof.NodeProfInstrument;
import ch.usi.inf.nodeprof.analysis.AnalysisFilterSourceList;
import ch.usi.inf.nodeprof.utils.GlobalObjectCache;
import ch.usi.inf.nodeprof.utils.Logger;
import ch.usi.inf.nodeprof.utils.SourceMapping;

public abstract class BasicAnalysisTest {

    protected Context context;
    protected NodeProfInstrument instrument;
    protected TestableNodeProfAnalysis analysis;

    public abstract TestableNodeProfAnalysis getAnalysis(Instrumenter instrumenter, TruffleInstrument.Env env);

    @Before
    public void init() {
        this.context = Context.create("js");
        context.eval(JavaScriptLanguage.ID, "");
        this.instrument = context.getEngine().getInstruments().get(NodeProfInstrument.ID).lookup(NodeProfInstrument.class);
        this.initAnalysis();
        GlobalObjectCache.reset();
        Logger.info("Test starts");
    }

    public abstract AnalysisFilterSourceList getFilter();

    public void initAnalysis() {
        this.analysis = getAnalysis(instrument.getInstrumenter(), this.instrument.getEnv());
        this.analysis.initCallbacks();
        this.analysis.enableTest();
        this.analysis.analysisReady(getFilter());
    }

    @After
    public void disposeAnalysis() {
        /**
         * print and clean
         */
        this.analysis.onDispose();
        SourceMapping.reset();
        Logger.info("Test finishes");
    }

    /**
     * TODO, make test path more reliable
     */
    static String base = "./src/ch.usi.inf.nodeprof.test/";
    public static String microPath = "./js/minitests/";

    /**
     * excluded if the test case uses some Node.js feature which is not availabe in js shell
     */
    List<String> excludeMiniTests = Arrays.asList(
                    "element2.js",
                    "require.js");

    @Test
    public void testMicroTestcases() {
        File dir = new File(microPath);
        if (!dir.exists()) {
            dir = new File(base + microPath);
        }
        assertTrue(dir.isDirectory());
        for (File f : dir.listFiles()) {
            if (f.getName().endsWith(".js")) {
                if (excludeMiniTests.contains(f.getName())) {
                    continue;
                }
                Logger.info("testing micro benchmark " + f.getName());
                try {
                    // every test need to be executed in a new context
                    init();
                    context.eval("js", Source.readFully(new FileReader(f)));
                } catch (IOException e) {
                    assertTrue(e.getMessage(), false);
                }
            }
        }
    }
}
