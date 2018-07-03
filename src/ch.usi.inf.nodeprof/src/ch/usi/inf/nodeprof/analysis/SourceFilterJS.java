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

package ch.usi.inf.nodeprof.analysis;


import ch.usi.inf.nodeprof.utils.Logger;
import ch.usi.inf.nodeprof.utils.SourceMapping;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter.SourcePredicate;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;

import java.util.HashSet;

import static ch.usi.inf.nodeprof.analysis.SourceFilterUtil.containsDoNotInstrument;

public class SourceFilterJS implements SourcePredicate {
    private Node call;
    private TruffleObject jsPredicateFunc;
    private final HashSet<Source> includedSources;
    private final HashSet<Source> excludedSources;
    private boolean isRecursive = false;

    public SourceFilterJS(TruffleObject jsPredicateFunc) {
        this.call = Message.createExecute(1).createNode();
        this.jsPredicateFunc = jsPredicateFunc;
        this.includedSources = new HashSet<>();
        this.excludedSources = new HashSet<>();
    }

    @Override
    @CompilerDirectives.TruffleBoundary
    public boolean test(final Source source) {
        if (excludedSources.contains(source))
            return false;
        if (includedSources.contains(source))
            return true;

        boolean include = true;

        String name;
        if (SourceMapping.isInternal(source)) {
            name = source.getName();
        } else {
            name = source.getPath();
        }

        Logger.debug("Source filter testing: " + name);

        if (include && containsDoNotInstrument(source)) {
            include = false;
            Logger.debug("Source filter: " + name + " -> excluded due to 'DO NOT INSTRUMENT'");
        }

        // we need to bail out during builtin calls inside the JS predicate
        if (include && isRecursive) {
            Logger.error("Source filter bailout due to recursive call of: " + name);
            return false;
        }

        if (include) {
            isRecursive = true;
            try {
                Object ret = ForeignAccess.sendExecute(call, jsPredicateFunc, SourceMapping.getJSObjectForSource(source));
                if (JSArray.isJSArray(ret)) {
                    Logger.error("TODO array return not implemented");
                }
                // TODO JSBoolean.isJSBoolean(ret) is false, is this really the right way?
                include = Boolean.parseBoolean(ret.toString());
            } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
                Logger.error("Source filter: call to JS predicate failed");
                Thread.dumpStack();
                System.exit(-1);
            }
            Logger.debug("JS Source filter: " + name + " -> " + (include ? "included": "excluded"));
            isRecursive = false;
        }

        if (include)
            includedSources.add(source);
        else
            excludedSources.add(source);

        return include;
    }
}
