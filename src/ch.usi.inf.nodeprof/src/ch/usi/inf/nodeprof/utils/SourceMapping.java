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
package ch.usi.inf.nodeprof.utils;

import java.io.File;
import java.util.HashMap;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

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
    public static synchronized void reset() {
        iidGen = 0;
        iidMap.clear();
        sourceSet.clear();
        idToSource.clear();
    }

    @TruffleBoundary
    private static String getRelative(String path) {
        if (path.startsWith("/")) {
            String base = System.getProperty("user.dir");
            return new File(base).toURI().relativize(new File(path).toURI()).getPath();
        } else {
            return path;
        }
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
        if (GlobalConfiguration.LOG_ABSOLUTE_PATH)
            b.append(fileName);
        else
            b.append(getRelative(fileName));
        b.append(":").append(sourceSection.getStartLine()).append(":").append(sourceSection.getStartColumn()).append(":").append(sourceSection.getEndLine()).append(":").append(
                sourceSection.getEndColumn() + 1);
        b.append(")");
        return b;
    }

    private static boolean isInternal(Source src) {
        return src.isInternal() || src.getPath() == null || src.getPath().equals("");
    }
}
