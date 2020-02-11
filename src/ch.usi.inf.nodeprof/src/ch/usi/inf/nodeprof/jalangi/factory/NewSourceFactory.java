/*******************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package ch.usi.inf.nodeprof.jalangi.factory;

import java.util.HashSet;
import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;

import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.FunctionRootEventHandler;

import static ch.usi.inf.nodeprof.utils.SourceMapping.getJSObjectForSource;

public class NewSourceFactory extends AbstractFactory {

    public NewSourceFactory(Object jalangiAnalysis, DynamicObject post) {
        super("newSource", jalangiAnalysis, null, post);
    }

    private Set<Source> seenSources = new HashSet<>();

    @TruffleBoundary
    private boolean isNewSource(Source source) {
        if (source == null) {
            return false;
        }
        return seenSources.add(source);
    }

    @Override
    public BaseEventHandlerNode create(EventContext context) {
        return new FunctionRootEventHandler(context) {
            @Node.Child private InteropLibrary postDispatch = (post == null) ? null : createDispatchNode();

            @Override
            public void executePre(VirtualFrame frame, Object[] inputs) throws InteropException {
                if (post == null) {
                    return;
                }

                Source source = getSource();
                if (source == null) {
                    return;
                }

                if (isNewSource(source)) {
                    wrappedDispatchExecution(postDispatch, post,
                            getJSObjectForSource(source), // arg 1: source object
                            source.getCharacters().toString()); // arg 2: source code
                }
            }
        };
    }
}
