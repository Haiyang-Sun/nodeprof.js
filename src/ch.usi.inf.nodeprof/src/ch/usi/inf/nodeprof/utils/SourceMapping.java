/*******************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class SourceMapping {
    private static int iidGen = 0;
    private static @CompilationFinal HashMap<Integer, String> iidMap;
    private static @CompilationFinal HashMap<SourceSection, Integer> sourceSet;
    private static @CompilationFinal HashMap<Integer, SourceSection> idToSource;

    @TruffleBoundary
    private static void init() {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        iidMap = new HashMap<>();
        sourceSet = new HashMap<>();
        idToSource = new HashMap<>();
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
        StringBuilder b = makeLocationString(sourceSection);

        iidMap.put(newIId, b.toString());
        sourceSet.put(sourceSection, newIId);
        idToSource.put(newIId, sourceSection);
        return newIId;
    }

    @TruffleBoundary
    public static String getLocationForIID(int iid) {
        return iidMap.get(iid);
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
        if (section == null)
            return Undefined.instance;
        Source source = section.getSource();
        DynamicObject o = getJSObjectForSource(source);
        JSObject.set(o, "section", makeSectionString(section).toString());
        return o;
    }

    @TruffleBoundary
    public static DynamicObject getJSObjectForSource(Source source) {
        if (source == null)
            return Undefined.instance;
        JSContext ctx = GlobalObjectCache.getInstance().getJSContext();
        DynamicObject o = JSUserObject.create(ctx);
        JSObject.set(o, "file", shortPath(source.getName()));
        JSObject.set(o, "internal", isInternal(source));
        return o;
    }

    @TruffleBoundary
    public static synchronized void reset() {
        iidGen = 0;
        iidMap.clear();
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
        b.append(sourceSection.getStartLine()).append(":")
                .append(sourceSection.getStartColumn())
                .append(":").append(sourceSection.getEndLine())
                .append(":").append(sourceSection.getEndColumn() + 1);
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

    private static boolean isInternal(Source src) {
        return src.isInternal() || src.getPath() == null || src.getPath().equals("");
    }
}
