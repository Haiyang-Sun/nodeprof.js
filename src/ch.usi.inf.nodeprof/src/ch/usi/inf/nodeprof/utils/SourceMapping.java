/*******************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package ch.usi.inf.nodeprof.utils;

import java.io.File;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.runtime.Evaluator;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.objects.Undefined;

import static ch.usi.inf.nodeprof.utils.ObjectHelper.setConfigProperty;

public abstract class SourceMapping {
    private static int iidGen = 0;
    @CompilationFinal private static HashMap<Integer, String> iidToLocationCache;
    @CompilationFinal private static HashMap<SourceSection, Integer> sourceSet;
    @CompilationFinal private static HashMap<Integer, SourceSection> idToSource;
    @CompilationFinal private static HashMap<SourceSection, String> syntheticLocations;

    @TruffleBoundary
    private static void init() {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        iidToLocationCache = new HashMap<>();
        sourceSet = new HashMap<>();
        idToSource = new HashMap<>();
        syntheticLocations = new HashMap<>();
    }

    static {
        init();
    }

    @TruffleBoundary
    public static int getIIDForSourceSection(SourceSection sourceSection) {
        if (sourceSet.containsKey(sourceSection)) {
            return sourceSet.get(sourceSection);
        }
        int newIId = ++iidGen;
        assert (newIId < Integer.MAX_VALUE);
        sourceSet.put(sourceSection, newIId);
        idToSource.put(newIId, sourceSection);
        return newIId;
    }

    @TruffleBoundary
    public static String getLocationForIID(int iid) {
        if (iidToLocationCache.containsKey(iid)) {
            return iidToLocationCache.get(iid);
        } else if (idToSource.containsKey(iid)) {
            String res = makeLocationString(idToSource.get(iid)).toString();
            iidToLocationCache.put(iid, res);
            return res;
        } else {
            return null;
        }
    }

    @TruffleBoundary
    public static String getCodeForIID(int iid) {
        if (idToSource.containsKey(iid)) {
            return idToSource.get(iid).getCharacters().toString();
        } else {
            return null;
        }
    }

    @TruffleBoundary
    public static SourceSection getSourceSectionForIID(int iid) {
        return idToSource.get(iid);
    }

    @TruffleBoundary
    public static DynamicObject getJSObjectForIID(int iid) {
        return getJSObjectForSourceSection(getSourceSectionForIID(iid));
    }

    @TruffleBoundary
    public static DynamicObject getJSObjectForSourceSection(SourceSection section) {
        if (section == null) {
            return Undefined.instance;
        }

        JSContext ctx = GlobalObjectCache.getInstance().getJSContext();
        JSRealm realm = JSRealm.get(null);
        Source source = section.getSource();
        DynamicObject o = getJSObjectForSource(source);

        DynamicObject range = JSArray.createConstant(ctx, realm, new Object[]{section.getCharIndex(), section.getCharEndIndex()});
        setConfigProperty(o, "range", range);

        DynamicObject loc = JSOrdinary.create(ctx, realm);
        DynamicObject start = JSOrdinary.create(ctx, realm);
        DynamicObject end = JSOrdinary.create(ctx, realm);
        setConfigProperty(start, "line", section.getStartLine());
        setConfigProperty(start, "column", section.getStartColumn());
        setConfigProperty(end, "line", section.getEndLine());
        setConfigProperty(end, "column", section.getEndColumn());
        setConfigProperty(loc, "start", start);
        setConfigProperty(loc, "end", end);
        setConfigProperty(o, "loc", loc);
        if (syntheticLocations.containsKey(section)) {
            setConfigProperty(o, "symbolic", syntheticLocations.get(section));
        }
        return o;
    }

    @TruffleBoundary
    public static DynamicObject getJSObjectForSource(Source source) {
        if (source == null) {
            return Undefined.instance;
        }
        JSContext ctx = GlobalObjectCache.getInstance().getJSContext();
        JSRealm realm = JSRealm.get(null);
        DynamicObject o = JSOrdinary.create(ctx, realm);
        String srcName = source.getName();
        if (isEval(source)) {
            String evalSrc = innerMostEvalSource(source.getName());
            if (evalSrc != null) {
                // strip :line:column from source name
                srcName = evalSrc.split(":")[0];
            } else {
                Logger.error("Failed to parse eval source: " + source.getName());
            }
            // 'eval' property signals the source is an eval-string, it contains the full eval hint
            setConfigProperty(o, "eval", source.getName());
        }
        setConfigProperty(o, "name", shortPath(srcName));
        setConfigProperty(o, "internal", isInternal(source));
        return o;
    }

    public static boolean isModuleOrWrapper(SourceSection section) {
        if (section == null) {
            return false;
        }
        int start = section.getCharIndex();
        return start <= 1;
    }

    @TruffleBoundary
    public static synchronized void reset() {
        iidGen = 0;
        iidToLocationCache.clear();
        sourceSet.clear();
        idToSource.clear();
    }

    /**
     * Helper to create shorter (relative) paths for a given path.
     *
     * @param path the path string to shorten
     * @return possibly shortened (relative) path
     */
    private static String shortPath(String path) {
        if (!GlobalConfiguration.LOG_ABSOLUTE_PATH && path.startsWith("/")) {
            String base = System.getProperty("user.dir");
            return new File(base).toURI().relativize(new File(path).toURI()).getPath();
        } else {
            return path;
        }
    }

    private static StringBuilder makeSectionString(SourceSection sourceSection) {
        StringBuilder b = new StringBuilder();
        if (GlobalConfiguration.SYMBOLIC_LOCATIONS && syntheticLocations.containsKey(sourceSection)) {
            b.append("{{");
            b.append(syntheticLocations.get(sourceSection));
            return b.append("}}");
        }
        b.append(sourceSection.getStartLine()).append(":").append(sourceSection.getStartColumn()).append(":").append(sourceSection.getEndLine()).append(":").append(sourceSection.getEndColumn() + 1);
        return b;
    }

    @TruffleBoundary
    public static StringBuilder makeLocationString(SourceSection sourceSection) {
        StringBuilder b = new StringBuilder();
        String fileName = sourceSection.getSource().getName();
        boolean isInternal = isInternal(sourceSection.getSource());
        b.append("(");
        if (isInternal) {
            b.append("*");
        }
        b.append(shortPath(fileName));
        b.append(":").append(makeSectionString(sourceSection));
        b.append(")");
        return b;
    }

    public static void addSyntheticLocation(SourceSection sourceSection, String name) {
        assert GlobalConfiguration.SYMBOLIC_LOCATIONS : "SYMBOLIC_LOCATIONS not enabled";
        boolean added = syntheticLocations.put(sourceSection, name) != null;
        if (added) {
            // invalidate cache
            iidToLocationCache.remove(sourceSet.get(sourceSection));
        }
    }

    /**
     * Helper function to determine what is considered internal by the source filter.
     *
     * @param src the Source to test
     * @return true if src is considered internal
     */
    public static boolean isInternal(final Source src) {
        return src.isInternal() || (!isEval(src) && (src.getPath() == null || src.getPath().equals("")));
    }

    public static boolean isEval(final Source src) {
        return src.getName().startsWith(Evaluator.EVAL_AT_SOURCE_NAME_PREFIX);
    }

    public static String innerMostEvalSource(String srcString) {
        String res = null;
        Pattern p = Pattern.compile(Evaluator.EVAL_AT_SOURCE_NAME_PREFIX + "[^\\s]+ \\((.*)\\)");
        Matcher m = p.matcher(srcString);
        while (m.matches()) {
            res = m.group(1);
            m = p.matcher(res);
        }
        return res;
    }
}
