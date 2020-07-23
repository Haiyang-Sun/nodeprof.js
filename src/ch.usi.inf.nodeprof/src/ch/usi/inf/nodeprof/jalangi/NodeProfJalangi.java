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
package ch.usi.inf.nodeprof.jalangi;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Env;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JSTypes;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

import ch.usi.inf.nodeprof.analysis.AnalysisFilterBase;
import ch.usi.inf.nodeprof.analysis.AnalysisFilterJS;
import ch.usi.inf.nodeprof.analysis.AnalysisFilterSourceList;
import ch.usi.inf.nodeprof.analysis.NodeProfAnalysis;
import ch.usi.inf.nodeprof.utils.GlobalObjectCache;
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
    public Object onLoad() throws Exception {
        // Get the global object via an indirect eval that works in strict mode.
        // Define __jalangiAdapter on it, then implicitly "return" it to set the JS context.
        Source src = Source.newBuilder(JavaScriptLanguage.ID, "(1,eval)('this').__jalangiAdapter = adapterVar; (1,eval)('this')", "nodeprof").build();
        CallTarget bootstrap = this.getEnv().parse(src, "adapterVar");
        Object globalObject = bootstrap.call(new JalangiAdapter(this));
        assert JSTypes.isDynamicObject(globalObject) : "bootstrap call did not return object";
        GlobalObjectCache.getInstance().addDynamicObject((DynamicObject) globalObject);
        return null;
    }

    /**
     * excluding some known libraries of jalangi2
     *
     * @return the filter
     */
    @Override
    @TruffleBoundary
    public AnalysisFilterSourceList getFilter() {
        AnalysisFilterSourceList baseFilter = super.getFilter();
        return AnalysisFilterSourceList.addMatchSources(baseFilter, Collections.singletonList("jalangi.js"));
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
    public void registerCallback(Object analysis, Object name, Object callback) throws UnsupportedTypeException {
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

    @TruffleBoundary
    private static Object getProperty(TruffleObject cb, String prop) {
        Object result = null;
        try {
            result = InteropLibrary.getFactory().getUncached().readMember(cb, prop);
        } catch (UnknownIdentifierException | UnsupportedMessageException e) {
            // undefined property is expected
        }
        if (Undefined.instance == result || Null.instance == result) {
            result = null;
        }
        return result;
    }

    private static AnalysisFilterBase parseFilterConfig(TruffleObject configObj) {
        AnalysisFilterBase result;

        if (JSFunction.isJSFunction(configObj)) {
            result = new AnalysisFilterJS(configObj);
            Logger.debug("JS filter installed: " + configObj.toString());
        } else {

            Object internal = getProperty(configObj, "internal");
            boolean instrumentInternal = internal == null ? false : JSRuntime.toBoolean(internal);

            boolean excludeFilter = true;
            String filters;
            Object excludes = getProperty(configObj, "excludes");
            if (excludes == null) {
                Object includes = getProperty(configObj, "includes");
                filters = includes == null ? "" : includes.toString();
                excludeFilter = false;
            } else {
                if (getProperty(configObj, "includes") != null) {
                    Logger.error("Filter config must not define 'include' and 'exclude' at the same time (config: " + JSRuntime.safeToString(configObj) + ")");
                    System.exit(-1);
                }
                filters = excludes.toString();
            }
            List<String> filterList = filters == null ? Collections.emptyList() : Arrays.asList(filters.split(","));
            AnalysisFilterSourceList listFilter;
            // return a filter based on excludeFilter, filterList and global excludes
            if (excludeFilter) {
                listFilter = AnalysisFilterSourceList.makeExcludeFilter(filterList, !instrumentInternal);
                listFilter = AnalysisFilterSourceList.addGlobalExcludes(listFilter);
            } else {
                listFilter = AnalysisFilterSourceList.makeIncludeFilter(filterList, "");
            }
            Logger.debug("Custom source filter: " + listFilter.getDescription());
            result = listFilter;
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
        analysisReady(parseFilterConfig(configObj));
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
