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
package ch.usi.inf.nodeprof.analysis;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.runtime.Evaluator;

import ch.usi.inf.nodeprof.ProfiledTagEnum;
import ch.usi.inf.nodeprof.utils.GlobalConfiguration;
import ch.usi.inf.nodeprof.utils.Logger;
import ch.usi.inf.nodeprof.utils.SourceMapping;

/**
 * Customized SourcePredicate
 */
public final class AnalysisFilterSourceList extends AnalysisFilterBase {
    private static final List<String> NO_EXCLUDES = Collections.emptyList();

    public enum ScopeEnum {
        app,
        allExceptInternal,
        all,
        builtin
    }

    public static AnalysisFilterSourceList getFilter(ScopeEnum scope) {
        switch (scope) {
            case all:
                return addGlobalExcludes(makeExcludeFilter(NO_EXCLUDES, false));
            case builtin:
                AnalysisFilterSourceList filter = makeIncludeFilter(Collections.singletonList("<builtin>"), "(builtin-filter)");
                filter.loggingEnabled = false;
                return filter;
            case app:
                return addGlobalExcludes(makeExcludeFilter(Arrays.asList("node_modules", Evaluator.FUNCTION_SOURCE_NAME), true));
            case allExceptInternal:
            default:
                return addGlobalExcludes(makeExcludeFilter(Collections.singletonList(Evaluator.FUNCTION_SOURCE_NAME), true));
        }
    }

    private final boolean instrumentInternal;
    private final boolean filterExcludes;
    private final String debugHint;
    private final HashSet<String> matchSources;
    private final HashSet<Source> loggedSources;
    private boolean loggingEnabled = true;

    /**
     * @param filterExcludes true for an exclude-filter, false for an include-filter
     * @param instrumentInternal true if we want to consider the internal Node.js modules
     * @param matchSources strings specifying key words to include/exclude with source files
     * @param debugHint string to identify a filter in debug output (or "")
     */
    @TruffleBoundary
    private AnalysisFilterSourceList(boolean filterExcludes, boolean instrumentInternal, HashSet<String> matchSources, String debugHint) {
        this.filterExcludes = filterExcludes;
        this.instrumentInternal = instrumentInternal;
        this.matchSources = matchSources;
        this.debugHint = debugHint;
        this.loggedSources = new HashSet<>();

        // ensure filter does not use empty string during matching
        this.matchSources.remove("");
    }

    public static AnalysisFilterSourceList makeExcludeFilter(List<String> matchSources, boolean excludeInternal) {
        return new AnalysisFilterSourceList(true, !excludeInternal, new HashSet<>(matchSources), "");
    }

    public static AnalysisFilterSourceList makeSingleIncludeFilter(String includeSource) {
        return new AnalysisFilterSourceList(false, true, new HashSet<>(Collections.singleton(includeSource)), "");
    }

    public static AnalysisFilterSourceList makeIncludeFilter(List<String> includeSources, String debugHint) {
        return new AnalysisFilterSourceList(false, true, new HashSet<>(includeSources), debugHint);
    }

    private static HashSet<String> parseExcludeConfig() {
        if (GlobalConfiguration.EXCL == null) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(GlobalConfiguration.EXCL.split(",")));
    }

    @Override
    public String getDescription() {
        return "list-based " + (filterExcludes ? "exclusion" : "inclusion") + " " + matchSources;
    }

    /**
     * Adds globally configured exclude sources to the already excluded sources of a given existing
     * filter, creating a new filter.
     *
     * @param filter an existing *exclude*-filter to be used as basis for the new filter
     * @return a new filter that excludes sources set in original filter and globally configured
     *         excludes
     */
    public static AnalysisFilterSourceList addGlobalExcludes(final AnalysisFilterSourceList filter) {
        // adding excludes to an include filter does not make sense
        assert (filter.filterExcludes);
        return addMatchSources(filter, parseExcludeConfig());
    }

    @TruffleBoundary
    public static AnalysisFilterSourceList addMatchSources(final AnalysisFilterSourceList filter, List<String> matchSources) {
        return addMatchSources(filter, new HashSet<>(matchSources));
    }

    @TruffleBoundary
    public static AnalysisFilterSourceList addMatchSources(final AnalysisFilterSourceList filter, HashSet<String> matchSources) {
        HashSet<String> mergedMatchSources = new HashSet<>(matchSources);
        mergedMatchSources.addAll(filter.matchSources);
        return new AnalysisFilterSourceList(filter.filterExcludes, filter.instrumentInternal, mergedMatchSources, filter.debugHint);
    }

    public static AnalysisFilterSourceList getDefault() {
        AnalysisFilterSourceList res;
        switch (GlobalConfiguration.SCOPE) {
            case "all":
                res = AnalysisFilterSourceList.getFilter(ScopeEnum.all);
                break;
            case "module":
                res = AnalysisFilterSourceList.getFilter(ScopeEnum.allExceptInternal);
                break;
            case "app":
                res = AnalysisFilterSourceList.getFilter(ScopeEnum.app);
                break;
            default:
                res = AnalysisFilterSourceList.getFilter(ScopeEnum.allExceptInternal);
                break;
        }
        if (GlobalConfiguration.DEBUG) {
            Logger.debug("default source filter " + res.filterExcludes + " " + res.matchSources.toString());
        }
        return res;
    }

    @Override
    @TruffleBoundary
    public boolean test(final Source source) {
        // don't try to instrument other languages
        if (isForeignSource(source)) {
            return false;
        }

        // if it's an exclusion filter, we include the source by default
        boolean res = filterExcludes;

        boolean isInternal = SourceMapping.isInternal(source);
        // use name or path of source depending whether we consider it internal

        boolean isEval = SourceMapping.isEval(source);
        boolean isIndirectEval = source.getName().startsWith(Evaluator.FUNCTION_SOURCE_NAME) && !matchSources.contains(Evaluator.FUNCTION_SOURCE_NAME);

        String name;
        if (isEval) {
            name = source.getName();
            String sourceOfEval = SourceMapping.innerMostEvalSource(name);
            if (sourceOfEval != null) {
                name = sourceOfEval.split(":")[0];
                // TODO, currently there is no way to judge if the eval is called from internal
                // for the moment, we assume it's not internal
                isInternal = false;
            }
        } else if (isIndirectEval) {
            name = source.getName();
            isInternal = false;
        } else if (isInternal) {
            name = source.getName();
        } else {
            name = source.getPath();
        }
        assert (name != null);

        if (instrumentInternal || !isInternal) {
            // log warning if source does not have a name
            if (name.equals("")) {
                if (logSource(source)) {
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
                if (res && containsDoNotInstrument(source)) {
                    res = false;
                    if (logSource(source)) {
                        Logger.debug("Source filter: " + logName(name, isInternal) + " -> excluded due to 'DO NOT INSTRUMENT'" + (this.debugHint.isEmpty() ? "" : (" " + this.debugHint)));
                    }
                }
            }
        } else {
            // not instrumenting internal source
            res = false;
        }

        if (res && logSource(source)) {
            Logger.debug("Source filter: " + logName(name, isInternal) + " -> included " + (this.debugHint.isEmpty() ? "" : (" " + this.debugHint)));
        }

        // debug log (once per source) if filter did something
        if (logSource(source)) {
            // don't log internal if they are being excluded (there are a lot of them)
            if (instrumentInternal || !isInternal) {
                Logger.debug("Source filter: " + logName(name, isInternal) + " -> " + (res ? "included" : "excluded") + (this.debugHint.isEmpty() ? "" : (" " + this.debugHint)));
            }
        }
        return res;
    }

    @Override
    public boolean testTag(final Source source, ProfiledTagEnum tag) {
        return true;
    }

    private boolean logSource(final Source source) {
        if (!loggingEnabled) {
            return false;
        }
        return loggedSources.add(source);
    }

    static String logName(String name, boolean internal) {
        StringBuilder b = new StringBuilder();
        b.append(name);
        if (internal) {
            b.append("*");
        }
        return b.toString();
    }
}
