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

import java.util.HashMap;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.source.SourceSection;

public abstract class SourceMapping {
    private static int iidGen = 0;
    private static @CompilationFinal HashMap<Long, String> iidMap;
    private static @CompilationFinal HashMap<SourceSection, Long> sourceSet;
    private static @CompilationFinal HashMap<Long, SourceSection> idToSource;

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
    public static long getIIDForSourceSection(SourceSection sourceSection) {
        if (sourceSet.containsKey(sourceSection)) {
            return sourceSet.get(sourceSection);
        }
        long newIId = ++iidGen;
        StringBuilder b = Logger.getSourceSectionId(sourceSection);

        iidMap.put(newIId, b.toString());
        sourceSet.put(sourceSection, newIId);
        idToSource.put(newIId, sourceSection);
        return newIId;
    }

    @TruffleBoundary
    public static String getLocationForIID(long iid) {
        return iidMap.get(iid);
    }

    @TruffleBoundary
    public static SourceSection getSourceSectionForIID(long iid) {
        return idToSource.get(iid);
    }

    @TruffleBoundary
    public static synchronized void reset() {
        iidGen = 0;
        iidMap.clear();
        sourceSet.clear();
        idToSource.clear();
    }
}
