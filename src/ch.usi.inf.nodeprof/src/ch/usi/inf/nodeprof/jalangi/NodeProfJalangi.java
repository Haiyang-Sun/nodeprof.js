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
package ch.usi.inf.nodeprof.jalangi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Env;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.parser.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

import ch.usi.inf.nodeprof.analysis.AnalysisSourceFilter;
import ch.usi.inf.nodeprof.analysis.NodeProfAnalysis;
import ch.usi.inf.nodeprof.utils.GlobalConfiguration;
import ch.usi.inf.nodeprof.utils.Logger;

/**
 * The Jalangi implementation in NodeProf
 */
public class NodeProfJalangi extends NodeProfAnalysis {
    @TruffleBoundary
    public NodeProfJalangi(Instrumenter instrumenter, Env env) {
        super("jalangi", instrumenter, env);
        this.jalangiAnalyses = new HashMap<>();
    }

    @Override
    @TruffleBoundary
    public void onLoad() throws Exception {
        Source src = Source.newBuilder("__jalangiAdapter = adapterVar").name("nodeprof").mimeType(JavaScriptLanguage.TEXT_MIME_TYPE).build();
        CallTarget bootstrap = this.getEnv().parse(src, "adapterVar");
        bootstrap.call(new JalangiAdapter(this));
    }

    /**
     * excluding some known libraries of jalangi2
     *
     * @return the filter
     */
    @Override
    @TruffleBoundary
    public AnalysisSourceFilter getFilter() {
        List<String> exclude = new ArrayList<>(Collections.singletonList("jalangi.js"));
        if (GlobalConfiguration.SCOPE.equals("app")) {
            exclude.add("node_modules");
        }
        return AnalysisSourceFilter.addGlobalExcludes(
                        AnalysisSourceFilter.makeExcludeFilter(exclude, !GlobalConfiguration.SCOPE.equals("all")));
    }

    /**
     * keep track of the analysis object (created by Jalangi ChainedAnalysisNoCheck) to the Java
     * representation
     */
    private HashMap<Object, JalangiAnalysis> jalangiAnalyses;

    /**
     * register hooks
     *
     * @param analysis object
     * @param name functionName
     * @param callback experimental extra feature for specialization, e.g., "this.putFieldPre.arr"
     */
    @TruffleBoundary
    public void registerCallback(Object analysis, Object name, Object callback) {
        if (!jalangiAnalyses.containsKey(analysis)) {
            jalangiAnalyses.put(analysis, new JalangiAnalysis(this, analysis));
        }
        jalangiAnalyses.get(analysis).registerCallback(name, callback);
    }

    /**
     * called in ChainedAnalysesNoCheck.js when all callbacks are defined
     */
    @TruffleBoundary
    public void onReady(Object analysis) {
        if (jalangiAnalyses.containsKey(analysis)) {
            jalangiAnalyses.get(analysis).onReady();
        }
        analysisReady();
    }

    Node read = Message.READ.createNode();

    @TruffleBoundary
    private Object getProperty(TruffleObject cb, String prop) {
        Object result = null;
        try {
            result = ForeignAccess.sendRead(this.read, cb, prop);
        } catch (UnknownIdentifierException | UnsupportedMessageException e) {
            // undefined property is expected
        }
        if (Undefined.instance == result || Null.instance == result)
            result = null;
        return result;
    }

    private AnalysisSourceFilter parseConfigObject(TruffleObject configObj) {
        AnalysisSourceFilter result;
        Object internal = getProperty(configObj, "internal");
        boolean instrumentInternal = internal == null ? false : Boolean.parseBoolean(internal.toString());

        boolean excludeFilter = true;
        String filters;
        Object excludes = getProperty(configObj, "excludes");
        if (excludes == null) {
            Object includes = getProperty(configObj, "includes");
            filters = includes == null ? "" : includes.toString();
            excludeFilter = false;
        } else {
            filters = excludes.toString();
        }
        List<String> filterList = filters == null ? Collections.emptyList() : Arrays.asList(filters.split(","));
        // return a filter based on excludeFilter, filterList and global excludes
        if (excludeFilter) {
            Logger.debug("a customized filter with exclusion list " + filters);
            result = AnalysisSourceFilter.makeExcludeFilter(filterList, !instrumentInternal);
            result = AnalysisSourceFilter.addGlobalExcludes(result);
        } else {
            Logger.debug("a customized filter with inclusion list " + filters);
            result = AnalysisSourceFilter.makeIncludeFilter(filterList, "");
        }
        return result;
    }

    /**
     * called in ChainedAnalysesNoCheck.js when all callbacks are defined
     */
    @TruffleBoundary
    public void onReady(Object analysis, TruffleObject configObj) {
        if (jalangiAnalyses.containsKey(analysis)) {
            jalangiAnalyses.get(analysis).onReady();
        }
        analysisReady(parseConfigObject(configObj));
    }

    @Override
    public void onClear() {

    }

    @Override
    public void printResult() {

    }

    @Override
    public void initCallbacks() {
    }

}
