/*******************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
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
package ch.usi.inf.nodeprof.analysis;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter.SourcePredicate;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.runtime.Evaluator;

import ch.usi.inf.nodeprof.utils.GlobalConfiguration;
import ch.usi.inf.nodeprof.utils.Logger;

/**
 * Customized SourcePredicate
 */
public class AnalysisSourceFilter implements SourcePredicate {
    private static final List<String> NO_EXCLUDES = Collections.emptyList();
    private static final AnalysisSourceFilter allExceptInternal = addGlobalExcludes(makeExcludeFilter(NO_EXCLUDES, true));
    private static final AnalysisSourceFilter all = addGlobalExcludes(makeExcludeFilter(NO_EXCLUDES, false));
    private static final AnalysisSourceFilter app = addGlobalExcludes(makeExcludeFilter(Collections.singletonList("node_modules"), true));
    private static final AnalysisSourceFilter builtinFilter = makeIncludeFilter(Collections.singletonList("<builtin>"), "(builtin-filter)");

    private final boolean instrumentInternal;
    private final boolean filterExcludes;
    private final String debugHint;
    private final HashSet<String> matchSources;
    private final HashSet<Source> loggedSources;

    /**
     * @param filterExcludes true for an exclude-filter, false for an include-filter
     * @param instrumentInternal true if we want to consider the internal Node.js modules
     * @param matchSources strings specifying key words to include/exclude with source files
     * @param debugHint string to identify a filter in debug output (or "")
     */
    @TruffleBoundary
    private AnalysisSourceFilter(boolean filterExcludes, boolean instrumentInternal, HashSet<String> matchSources, String debugHint) {
        this.filterExcludes = filterExcludes;
        this.instrumentInternal = instrumentInternal;
        this.matchSources = matchSources;
        this.debugHint = debugHint;
        this.loggedSources = new HashSet<>();

        // ensure filter does not use empty string during matching
        this.matchSources.remove("");
    }

    public static AnalysisSourceFilter makeExcludeFilter(List<String> matchSources, boolean excludeInternal) {
        return new AnalysisSourceFilter(true, !excludeInternal, new HashSet<>(matchSources), "");
    }

    public static AnalysisSourceFilter makeSingleIncludeFilter(String includeSource) {
        return new AnalysisSourceFilter(false, true, new HashSet<>(Collections.singleton(includeSource)), "");
    }

    public static AnalysisSourceFilter makeIncludeFilter(List<String> includeSources, String debugHint) {
        return new AnalysisSourceFilter(false, true, new HashSet<>(includeSources), debugHint);
    }

    private static HashSet<String> parseExcludeConfig() {
        if (GlobalConfiguration.EXCL == null)
            return new HashSet<>();
        return new HashSet<>(Arrays.asList(GlobalConfiguration.EXCL.split(",")));
    }

    /**
     * Adds globally configured exclude sources to the already excluded sources of a given existing
     * filter, creating a new filter.
     *
     * @param filter an existing *exclude*-filter to be used as basis for the new filter
     * @return a new filter that excludes sources set in original filter and globally configured
     *         excludes
     */
    public static AnalysisSourceFilter addGlobalExcludes(final AnalysisSourceFilter filter) {
        // adding excludes to an include filter does not make sense
        assert (filter.filterExcludes);
        HashSet<String> mergedMatchSources = parseExcludeConfig();
        mergedMatchSources.addAll(filter.matchSources);
        return new AnalysisSourceFilter(filter.filterExcludes, filter.instrumentInternal, mergedMatchSources, filter.debugHint);
    }

    public static AnalysisSourceFilter getDefault() {
        AnalysisSourceFilter res;
        switch (GlobalConfiguration.SCOPE) {
            case "all":
                res = AnalysisSourceFilter.all;
                break;
            case "module":
                res = AnalysisSourceFilter.allExceptInternal;
                break;
            case "app":
                res = AnalysisSourceFilter.app;
                break;
            default:
                res = AnalysisSourceFilter.app;
                break;
        }
        return res;
    }

    public static AnalysisSourceFilter getAll() {
        return AnalysisSourceFilter.all;
    }

    public static AnalysisSourceFilter allExceptInternal() {
        return AnalysisSourceFilter.allExceptInternal;
    }

    public static AnalysisSourceFilter getBuiltinFilter() {
        return AnalysisSourceFilter.builtinFilter;
    }

    @Override
    @TruffleBoundary
    public boolean test(final Source source) {
        // if it's an exclusion filter, we include the source by default
        boolean res = filterExcludes;

        boolean isInternal = isInternal(source);
        // use name or path of source depending whether we consider it internal

        boolean isEval = isInternal && source.getName().startsWith(Evaluator.EVAL_AT_SOURCE_NAME_PREFIX);
        String name;
        if (isEval) {
            name = source.getName();
            int startPos = name.indexOf('(');
            int endPos = name.indexOf(')');
            if (startPos > -1 && endPos > -1) {
                int fileEnd = name.indexOf(':', startPos);
                if (fileEnd > -1 && fileEnd < endPos) {
                    endPos = fileEnd;
                }
                name = name.substring(startPos + 1, endPos);
                // TODO, currently there is no way to judge if the eval is called from internal
                // for the moment, we assume it's not internal
                isInternal = false;
            }
        } else if (isInternal) {
            name = source.getName();
        } else {
            name = source.getPath();
        }
        assert (name != null);

        if (instrumentInternal || !isInternal) {
            // log warning if source does not have a name
            if (name.equals("")) {
                if (loggedSources.add(source)) {
                    Logger.warning("Source filter: ignoring source without name");
                }
            } else {
                // match against included/excluded sources
                for (final String str : matchSources) {
                    if (name.contains(str)) {
                        // apply filter
                        res = !filterExcludes;
                        break;
                    }
                }
                if (res && source.getLineCount() > 0) {
                    // check if the source code has a special filter string at its beginning
                    CharSequence sourceChars = source.getCharacters();
                    String sourceHead = sourceChars.subSequence(0, Math.min(sourceChars.length() - 1, 1000)).toString().trim();
                    // should be enough
                    if (sourceHead.contains("DO NOT INSTRUMENT")) {
                        res = false;
                        if (loggedSources.add(source)) {
                            Logger.debug("Source filter: " + name + " -> excluded due to 'DO NOT INSTRUMENT'" + (this.debugHint.isEmpty() ? "" : (" " + this.debugHint)));
                        }
                    }
                }
            }
        } else {
            // not instrumenting internal source
            res = false;
        }

        // debug log (once per source) if filter did something
        if (res != filterExcludes && loggedSources.add(source)) {
            // don't log internal if they are being excluded (there are a lot of them)
            if (instrumentInternal || !isInternal) {
                Logger.debug("Source filter: " + name + " -> " + (res ? "included" : "excluded") + (this.debugHint.isEmpty() ? "" : (" " + this.debugHint)));
            }
        }
        return res;
    }

    /**
     * Helper function to determine what is considered internal by the source filter.
     *
     * @param src the Source to test
     * @return true if src is considered internal
     */
    static private boolean isInternal(final Source src) {
        return src.isInternal() || src.getPath() == null || src.getPath().equals("");
    }
}
