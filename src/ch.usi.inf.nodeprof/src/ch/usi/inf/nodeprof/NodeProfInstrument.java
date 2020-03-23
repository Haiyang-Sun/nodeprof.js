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
package ch.usi.inf.nodeprof;

import org.graalvm.options.OptionDescriptors;

import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.instrumentation.ContextsListener;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Registration;
import com.oracle.truffle.api.nodes.LanguageInfo;

import ch.usi.inf.nodeprof.analysis.NodeProfAnalysis;
import ch.usi.inf.nodeprof.utils.GlobalConfiguration;
import ch.usi.inf.nodeprof.utils.Logger;
import ch.usi.inf.nodeprof.utils.RawEventsTracingSupport;

/**
 * TruffleInstrument for the profiler
 *
 * @since 0.30
 */
@Registration(id = NodeProfInstrument.ID, name = "NodeProf profiling agent", version = "0.1", services = {NodeProfInstrument.class})
public class NodeProfInstrument extends TruffleInstrument implements ContextsListener {
    public static final String ID = "nodeprof";
    private Instrumenter instrumenter;
    private Env instrumentEnv;

    public Env getEnv() {
        return instrumentEnv;
    }

    private boolean readyToLoad = false;

    private boolean loaded = false;

    public NodeProfInstrument() {
        super();
    }

    @Override
    protected OptionDescriptors getOptionDescriptors() {
        return new NodeProfOptionsDescriptors();
    }

    @Override
    protected void onCreate(final Env env) {
        this.instrumentEnv = env;
        GlobalConfiguration.setup(env);
        Logger.debug("NodeProf has been enabled");
        instrumenter = env.getInstrumenter();
        env.registerService(this);
        /**
         * enable analyses based on
         */
        env.getInstrumenter().attachContextsListener(this, true);
    }

    @Override
    protected void onDispose(final Env env) {
        cleanAnalysis();
        /**
         * Dump execution counters for every tag
         */
        ProfiledTagEnum.dump();
    }

    public Instrumenter getInstrumenter() {
        return instrumenter;
    }

    /**
     * reset the state of all the analysis
     */
    public static void cleanAnalysis() {
        /**
         * Run dispose for all analyses
         */
        for (NodeProfAnalysis analysis : NodeProfAnalysis.getEnabledAnalyses()) {
            if (analysis != null) {
                analysis.onDispose();
            }
        }
    }

    @Override
    public void onLanguageContextCreated(TruffleContext context, LanguageInfo language) {

    }

    @Override
    public void onContextCreated(TruffleContext context) {

    }

    @Override
    public void onLanguageContextInitialized(TruffleContext context, LanguageInfo language) {

        assert (context != null);
        /**
         * the language context will be created twice at the beginning. in svm the second context is
         * different than the first one while in jvm the second context is the same as the first
         * one. we enable NodeProf after the second one is initilized
         */
        if (GlobalConfiguration.DEBUG_TRACING) {
            RawEventsTracingSupport.enable(instrumenter);
        }
        if (readyToLoad && !loaded) {
            if (GlobalConfiguration.ANALYSIS != null) {
                String[] names = GlobalConfiguration.ANALYSIS.split(",");
                for (String name : names) {
                    if (GlobalConfiguration.DEBUG) {
                        Logger.debug("loading " + name + " for analysis");
                    }
                    NodeProfAnalysis.enableAnalysis(this.instrumenter, instrumentEnv, name);
                }
            }
            loaded = true;
        }
        // ready to load for the second context
        readyToLoad = true;
    }

    @Override
    public void onLanguageContextFinalized(TruffleContext context, LanguageInfo language) {

    }

    @Override
    public void onLanguageContextDisposed(TruffleContext context, LanguageInfo language) {

    }

    @Override
    public void onContextClosed(TruffleContext context) {

    }
}
