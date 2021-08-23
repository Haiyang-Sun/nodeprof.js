/* *****************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package ch.usi.inf.nodeprof;

import java.util.Arrays;
import java.util.Iterator;

import org.graalvm.options.OptionDescriptor;
import org.graalvm.options.OptionDescriptors;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Registration;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.InputNodeTag;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSFunction;

/**
 * TruffleInstrument for the profiler
 *
 * @since 0.30
 */
@Registration(id = DebugInstrument.ID, name = "Debuging Instrument", version = "0.1", services = {DebugInstrument.class})
public class DebugInstrument extends TruffleInstrument {
    public static final String ID = "debugi";
    private Instrumenter instrumenter;

    public DebugInstrument() {
        super();
    }

    @Override
    protected OptionDescriptors getOptionDescriptors() {
        return new OptionDescriptors() {

            public Iterator<OptionDescriptor> iterator() {
                return Arrays.asList(DebugInstrCLI.ods).iterator();
            }

            public OptionDescriptor get(String optionName) {
                Iterator<OptionDescriptor> iter = iterator();
                while (iter.hasNext()) {
                    OptionDescriptor od = iter.next();
                    if (od.getName().equals(optionName)) {
                        return od;
                    }
                }
                return null;
            }
        };
    }

    @Override
    protected void onCreate(final Env env) {
        instrumenter = env.getInstrumenter();
        env.registerService(this);
        SourceSectionFilter sourceSectionFilter = SourceSectionFilter.newBuilder().tagIs(JSTags.ALL).build();
        // What generates the input events to track?
        SourceSectionFilter inputGeneratingObjects = SourceSectionFilter.newBuilder().tagIs(
                        StandardTags.ExpressionTag.class,
                        StandardTags.StatementTag.class,
                        InputNodeTag.class).build();
        env.getInstrumenter().attachExecutionEventFactory(sourceSectionFilter, inputGeneratingObjects, new ExecutionEventNodeFactory() {
            public ExecutionEventNode create(EventContext context) {
                // TODO Auto-generated method stub
                return new ExecutionEventNode() {
                    @Node.Child private InteropLibrary dispatch = InteropLibrary.getFactory().createDispatched(5);

                    @TruffleBoundary
                    @Override
                    public void onEnter(VirtualFrame frame) {
                        /*
                         * Internal sources are executed at engine startup time. Such sources
                         * include internal code for the registration of builtins like Promise. We
                         * skip all these internal events to ensure that tests are deterministic.
                         */
                        try {
                            dispatch.execute(JSFunction.createEmptyFunction(JSRealm.get(this)));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
            }
        });
    }

    @Override
    protected void onDispose(final Env env) {
    }

    public Instrumenter getInstrumenter() {
        return instrumenter;
    }

}
