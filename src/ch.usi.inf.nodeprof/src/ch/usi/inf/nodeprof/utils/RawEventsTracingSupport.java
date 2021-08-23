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
package ch.usi.inf.nodeprof.utils;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BuiltinRootTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.DeclareTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadVariableTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteVariableTag;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSObject;

import ch.usi.inf.nodeprof.ProfiledTagEnum;
import ch.usi.inf.nodeprof.analysis.AnalysisFilterSourceList;

public class RawEventsTracingSupport {

    private static final Class<?>[] ALL = ProfiledTagEnum.getTags();

    // TODO maybe there's a nicer way to avoid enabling an instrument twice...
    private static boolean enabled = false;

    @TruffleBoundary
    public static void enable(Instrumenter instrumenter) {
        if (enabled == false) {
            SourceSectionFilter sourceSectionFilter = SourceSectionFilter.newBuilder().sourceIs(AnalysisFilterSourceList.getDefault()).tagIs(ALL).build();
            SourceSectionFilter inputGeneratingObjects = SourceSectionFilter.newBuilder().tagIs(
                            StandardTags.ExpressionTag.class,
                            JSTags.InputNodeTag.class).build();
            instrumenter.attachExecutionEventFactory(sourceSectionFilter, inputGeneratingObjects, getFactory());
            Logger.info("Low-level event tracing enabled [SVM: " + JSConfig.SubstrateVM + "]");
            enabled = true;
        }
    }

    private static ExecutionEventNodeFactory getFactory() {
        ExecutionEventNodeFactory factory = new ExecutionEventNodeFactory() {

            private int depth = 0;

            @Override
            public ExecutionEventNode create(EventContext c) {
                return new ExecutionEventNode() {

                    private void log(String s) {
                        String p = "";
                        int d = depth;
                        while (d-- > 0) {
                            p += "    ";
                        }
                        Logger.info(p + s);
                    }

                    @TruffleBoundary
                    private String getValueDescription(Object inputValue) {
                        if (JSFunction.isJSFunction(inputValue)) {
                            return "JSFunction:'" + JSFunction.getName((DynamicObject) inputValue) + "'";
                        } else if (JSObject.isJSObject(inputValue)) {
                            return "JSObject: instance";
                        } else if (inputValue instanceof String) {
                            return inputValue.toString();
                        } else if (inputValue instanceof Number) {
                            return inputValue.toString();
                        } else if (inputValue instanceof Boolean) {
                            return inputValue.toString();
                        }
                        return inputValue != null ? inputValue.getClass().getSimpleName() : "null";
                    }

                    @TruffleBoundary
                    @Override
                    protected void onInputValue(VirtualFrame frame, EventContext i, int inputIndex, Object inputValue) {
                        String format = String.format("%-7s|tag: %-20s @ %-20s|val: %-25s|from: %-20s", "IN " + (1 + inputIndex) + "/" + getInputCount(),
                                        getTagNames((JavaScriptNode) c.getInstrumentedNode()),
                                        c.getInstrumentedNode().getClass().getSimpleName(), getValueDescription(inputValue), i.getInstrumentedNode().getClass().getSimpleName());
                        log(format);
                    }

                    @Node.Child private InteropLibrary dispatch = InteropLibrary.getFactory().createDispatched(5);

                    @TruffleBoundary
                    @Override
                    public void onEnter(VirtualFrame frame) {
                        try {
                            dispatch.execute(JSFunction.createEmptyFunction(JSRealm.get(null)));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        String format = String.format("%-7s|tag: %-20s @ %-20s |attr: %-20s", "ENTER", getTagNames((JavaScriptNode) c.getInstrumentedNode()),
                                        c.getInstrumentedNode().getClass().getSimpleName(), getAttributesDescription(c));
                        log(format);
                        depth++;

                    }

                    @TruffleBoundary
                    @Override
                    protected void onReturnValue(VirtualFrame frame, Object result) {
                        depth--;
                        String format = String.format("%-7s|tag: %-20s @ %-20s |rval: %-20s |attr: %-20s", "RETURN", getTagNames((JavaScriptNode) c.getInstrumentedNode()),
                                        c.getInstrumentedNode().getClass().getSimpleName(), getValueDescription(result), getAttributesDescription(c));
                        log(format);
                    }

                    @TruffleBoundary
                    @Override
                    protected void onReturnExceptional(VirtualFrame frame, Throwable exception) {
                        depth--;
                        String format = String.format("%-7s|tag: %-20s @ %-20s |rval: %-20s |attr: %-20s", "RET-EXC", getTagNames((JavaScriptNode) c.getInstrumentedNode()),
                                        c.getInstrumentedNode().getClass().getSimpleName(), exception.getClass().getSimpleName(), getAttributesDescription(c));
                        log(format);
                    }

                    private Object getAttributeFrom(EventContext cx, String name) {
                        try {
                            return InteropLibrary.getFactory().getUncached().readMember(((InstrumentableNode) cx.getInstrumentedNode()).getNodeObject(), name);
                        } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    private String appendAttributes(EventContext cx, String... attributeNames) {
                        StringBuilder sb = new StringBuilder();
                        for (String aName : attributeNames) {
                            sb.append(aName + "='" + getAttributeFrom(cx, aName) + "' ");
                        }
                        return sb.toString();
                    }

                    private String getAttributesDescription(EventContext cx) {
                        String extra = "";
                        JavaScriptNode n = (JavaScriptNode) cx.getInstrumentedNode();
                        if (n.hasTag(BuiltinRootTag.class)) {
                            extra += appendAttributes(cx, "name");
                        }
                        if (n.hasTag(ReadPropertyTag.class)) {
                            extra += appendAttributes(cx, "key");
                        }
                        if (n.hasTag(ReadVariableTag.class)) {
                            extra += appendAttributes(cx, "name");
                        }
                        if (n.hasTag(WritePropertyTag.class)) {
                            extra += appendAttributes(cx, "key");
                        }
                        if (n.hasTag(WriteVariableTag.class)) {
                            extra += appendAttributes(cx, "name");
                        }
                        if (n.hasTag(LiteralTag.class)) {
                            extra += appendAttributes(cx, LiteralTag.TYPE);
                        }
                        if (n.hasTag(DeclareTag.class)) {
                            extra += appendAttributes(cx, DeclareTag.NAME, DeclareTag.TYPE);
                        }
                        return extra;
                    }
                };
            }
        };
        return factory;
    }

    @SuppressWarnings("unchecked")
    public static final String getTagNames(JavaScriptNode node) {
        String tags = "";

        if (node.hasTag(StandardTags.StatementTag.class)) {
            tags += "STMT ";
        }
        if (node.hasTag(StandardTags.RootTag.class)) {
            tags += "ROOT ";
        }
        if (node.hasTag(StandardTags.RootBodyTag.class)) {
            tags += "BODY ";
        }
        for (Class<?> c : ALL) {
            if (node.hasTag((Class<? extends Tag>) c)) {
                tags += c.getSimpleName() + " ";
            }
        }
        return tags;
    }

}
