/* *****************************************************************************
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
 * *****************************************************************************/
package ch.usi.inf.nodeprof.jalangi.factory;

import java.util.HashSet;
import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;

import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.FunctionRootEventHandler;
import ch.usi.inf.nodeprof.utils.SourceMapping;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Strings;

public class InitialRootFactory extends AbstractFactory {

    public InitialRootFactory(Object jalangiAnalysis, DynamicObject post) {
        super("newSource", jalangiAnalysis, null, post);
    }

    private final Set<Source> seenSources = new HashSet<>();

    /**
     * Returns the code for source as a string if source has not been seen yet.
     *
     * @param source source object
     * @return source as String or null
     */
    @TruffleBoundary
    private TruffleString sourceStringIfNew(Source source) {
        if (source == null) {
            return null;
        }
        if (seenSources.add(source)) {
            // getCharacters() needs to be behind boundary
            return Strings.fromCharSequence(source.getCharacters());
        }
        return null;
    }

    @Override
    public BaseEventHandlerNode create(EventContext context) {
        return new FunctionRootEventHandler(context) {
            @Child CallbackNode cbNode = new CallbackNode();

            @Override
            public int getPriority() {
                return -1;
            }

            @Override
            public BaseEventHandlerNode wantsToUpdateHandler() {
                // remove after initial execution
                return null;
            }

            @Override
            public void executePre(VirtualFrame frame, Object[] inputs) throws InteropException {
                checkForSymbolicLocation(context.getInstrumentedNode(), getArguments(frame));

                if (post == null) {
                    return;
                }

                Source source = getSource();
                if (source == null) {
                    return;
                }

                TruffleString sourceAsString = sourceStringIfNew(source);

                if (sourceAsString != null) {
                    cbNode.postCall(this,
                                    jalangiAnalysis,
                                    post,
                                    SourceMapping.getJSObjectForSource(source), // arg 1: source
                                                                                // object
                                    sourceAsString); // arg 2: source code
                }
            }
        };
    }
}
