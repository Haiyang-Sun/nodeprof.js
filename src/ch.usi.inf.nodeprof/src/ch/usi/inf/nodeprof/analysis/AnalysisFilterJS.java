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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;

import ch.usi.inf.nodeprof.ProfiledTagEnum;
import ch.usi.inf.nodeprof.jalangi.JalangiAnalysis;
import ch.usi.inf.nodeprof.utils.Logger;
import ch.usi.inf.nodeprof.utils.SourceMapping;

public class AnalysisFilterJS extends AnalysisFilterBase {
    private final TruffleObject jsPredicateFunc;
    private final HashMap<Source, EnumSet<ProfiledTagEnum>> includedSources;
    private final HashSet<Source> excludedSources;
    private boolean isRecursive = false;
    private static final EnumSet<ProfiledTagEnum> allTags = EnumSet.allOf(ProfiledTagEnum.class);

    public AnalysisFilterJS(TruffleObject jsPredicateFunc) {
        this.jsPredicateFunc = jsPredicateFunc;
        this.includedSources = new HashMap<>();
        this.excludedSources = new HashSet<>();
    }

    @Override
    public String getDescription() {
        return "JS-based filter";
    }

    @Override
    @TruffleBoundary
    public boolean test(final Source source) {
        if (isForeignSource(source) || excludedSources.contains(source)) {
            return false;
        }
        if (includedSources.containsKey(source)) {
            return true;
        }

        boolean include = true;

        String name;
        if (SourceMapping.isInternal(source)) {
            name = source.getName();
        } else {
            name = source.getPath();
        }

        Logger.debug("JS Analysis filter testing: " + name + (SourceMapping.isInternal(source) ? " (internal)" : ""));

        if (include && containsDoNotInstrument(source)) {
            include = false;
            Logger.debug("JS Analysis filter: " + name + " -> excluded due to 'DO NOT INSTRUMENT'");
        }

        // we need to bail out during builtin calls inside the JS predicate
        if (include && isRecursive) {
            if (!(name.equals("<builtin>") || name.equals("<internal>"))) {
                Logger.error("JS Analysis filter bailout due to recursive call while testing: " + name);
            }
            return false;
        }

        EnumSet<ProfiledTagEnum> includeTags = allTags;

        if (include) {

            // prevent JS predicate being entered more than once
            isRecursive = true;

            try {
                Object ret = InteropLibrary.getFactory().getUncached().execute(jsPredicateFunc, SourceMapping.getJSObjectForSource(source));
                if (JSArray.isJSArray(ret)) {
                    include = JSAbstractArray.arrayGetLength((DynamicObject) ret) > 0;
                    includeTags = mapToTags(JSAbstractArray.toArray((DynamicObject) ret));
                } else {
                    include = JSRuntime.toBoolean(ret);
                }
            } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
                Logger.error("JS Analysis filter: call to JS predicate failed");
                Thread.dumpStack();
                System.exit(-1);
            }

            isRecursive = false;

            String tagStr = "";
            if (includeTags != allTags) {
                tagStr = " " + includeTags.toString();
            }
            Logger.debug("JS Analysis filter: " + name + " -> " + (include ? "included" : "excluded") + tagStr);

        }

        if (include) {
            includedSources.put(source, includeTags);
        } else {
            excludedSources.add(source);
        }

        return include;
    }

    @TruffleBoundary
    private static EnumSet<ProfiledTagEnum> mapToTags(Object[] callbacks) {
        EnumSet<ProfiledTagEnum> set = EnumSet.noneOf(ProfiledTagEnum.class);
        for (Object cb : callbacks) {
            EnumSet<ProfiledTagEnum> tags = JalangiAnalysis.callbackMap.get(cb.toString());
            if (tags == null) {
                Logger.error("JS Analysis filter predicate returned non-Jalangi callback: " + cb);
            } else {
                set.addAll(tags);
            }
        }
        return set;
    }

    @Override
    @TruffleBoundary
    public boolean testTag(final Source source, ProfiledTagEnum tag) {
        EnumSet<ProfiledTagEnum> tags = includedSources.get(source);
        assert (tags != null);
        return tags == allTags || tags.contains(tag);
    }
}
