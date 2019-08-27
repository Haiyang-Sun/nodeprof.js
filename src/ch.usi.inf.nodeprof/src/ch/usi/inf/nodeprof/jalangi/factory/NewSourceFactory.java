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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.nodes.function.FunctionBodyNode;

import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.FunctionRootEventHandler;

public class NewSourceFactory extends AbstractFactory {

    public NewSourceFactory(Object jalangiAnalysis, DynamicObject post) {
        super("newSource", jalangiAnalysis, null, post, -1, 2);
    }

    private Set<Source> seenSources = new HashSet<>();

    @Override
    public BaseEventHandlerNode create(EventContext context) {
        return new FunctionRootEventHandler(context) {
            @Child DirectCallNode postCall = createPostCallNode();

            @Override
            public void executePre(VirtualFrame frame, Object[] inputs) {
                if (isRegularExpression() || this.isBuiltin || post == null) {
                    return;
                }

                Node n = context.getInstrumentedNode();
                while (n != null && !(n instanceof FunctionBodyNode)) {
                    n = n.getParent();
                }

                if (n == null) {
                    return;
                }

                if (n.getSourceSection() == null) {
                    return;
                }

                Source source = n.getSourceSection().getSource();
                if (source == null) {
                    return;
                }

                if (seenSources.add(source)) {
                    setPostArguments(0, convertResult(source.getName()));
                    setPostArguments(1, source.getCharacters().toString());
                    directCall(postCall, false, getSourceIID());
                }
            }

        };
    }
}