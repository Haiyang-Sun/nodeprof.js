package ch.usi.inf.nodeprof.utils;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BuiltinRootTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBlockTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBranchTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowRootTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.EvalCallTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.FunctionCallExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ObjectAllocationExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadElementExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadVariableExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.UnaryExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteElementExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteVariableExpressionTag;
import com.oracle.truffle.js.runtime.builtins.JSFunction;

public class RawEventsTracingSupport {

    public static final Class<?>[] ALL = new Class[]{
                    ObjectAllocationExpressionTag.class,
                    BinaryExpressionTag.class,
                    UnaryExpressionTag.class,
                    ControlFlowRootTag.class,
                    WriteVariableExpressionTag.class,
                    ReadElementExpressionTag.class,
                    WriteElementExpressionTag.class,
                    ReadPropertyExpressionTag.class,
                    WritePropertyExpressionTag.class,
                    ReadVariableExpressionTag.class,
                    LiteralExpressionTag.class,
                    FunctionCallExpressionTag.class,
                    BuiltinRootTag.class,
                    EvalCallTag.class,
                    ControlFlowRootTag.class,
                    ControlFlowBlockTag.class,
                    ControlFlowBranchTag.class,
    };

    public static void enable(Instrumenter instrumenter) {
        SourceSectionFilter sourceSectionFilter = SourceSectionFilter.newBuilder().tagIs(ALL).build();
        SourceSectionFilter inputGeneratingObjects = SourceSectionFilter.newBuilder().tagIs(
                        StandardTags.ExpressionTag.class,
                        StandardTags.StatementTag.class).build();
        instrumenter.attachExecutionEventFactory(sourceSectionFilter, inputGeneratingObjects, getFactory());
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
                        System.out.println(p + s);
                    }

                    private String getValueDescription(Object inputValue) {
                        if (JSFunction.isJSFunction(inputValue)) {
                            return "JSFunction:'" + JSFunction.getName((DynamicObject) inputValue) + "'";
                        }
                        return inputValue != null ? inputValue.toString() : "null";
                    }

                    @Override
                    protected void onInputValue(VirtualFrame frame, EventContext i, int inputIndex, Object inputValue) {
                        String format = String.format("%-7s|tag: %-20s @ %-20s|val: %-25s|from: %-20s", "IN " + (1 + inputIndex) + "/" + getInputCount(),
                                        getTagNames((JavaScriptNode) c.getInstrumentedNode()),
                                        c.getInstrumentedNode().getClass().getSimpleName(), getValueDescription(inputValue), i.getInstrumentedNode().getClass().getSimpleName());
                        log(format);
                    }

                    @Override
                    public void onEnter(VirtualFrame frame) {
                        String format = String.format("%-7s|tag: %-20s @ %-20s |attr: %-20s", "ENTER", getTagNames((JavaScriptNode) c.getInstrumentedNode()),
                                        c.getInstrumentedNode().getClass().getSimpleName(), getAttributesDescription(c));
                        log(format);
                        depth++;
                    }

                    @Override
                    protected void onReturnValue(VirtualFrame frame, Object result) {
                        depth--;
                        String format = String.format("%-7s|tag: %-20s @ %-20s |rval: %-20s |attr: %-20s", "RETURN", getTagNames((JavaScriptNode) c.getInstrumentedNode()),
                                        c.getInstrumentedNode().getClass().getSimpleName(), result, getAttributesDescription(c));
                        log(format);
                    }

                    @Override
                    protected void onReturnExceptional(VirtualFrame frame, Throwable exception) {
                        depth--;
                        String format = String.format("%-7s|tag: %-20s @ %-20s |rval: %-20s |attr: %-20s", "RET-EXC", getTagNames((JavaScriptNode) c.getInstrumentedNode()),
                                        c.getInstrumentedNode().getClass().getSimpleName(), exception.getClass().getSimpleName(), getAttributesDescription(c));
                        log(format);
                    }

                    private String getAttributeFrom(EventContext cx, String name) {
                        try {
                            return (String) ForeignAccess.sendRead(Message.READ.createNode(), (TruffleObject) ((InstrumentableNode) cx.getInstrumentedNode()).getNodeObject(), name);
                        } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    private String getAttributesDescription(EventContext cx) {
                        String extra = "";
                        JavaScriptNode n = (JavaScriptNode) cx.getInstrumentedNode();
                        if (n.hasTag(BuiltinRootTag.class)) {
                            String tagAttribute = getAttributeFrom(cx, "name");
                            extra += tagAttribute;
                        }
                        if (n.hasTag(ReadPropertyExpressionTag.class)) {
                            String tagAttribute = "'" + getAttributeFrom(cx, "key") + "' ";
                            extra += tagAttribute;
                        }
                        if (n.hasTag(ReadVariableExpressionTag.class)) {
                            String tagAttribute = "'" + getAttributeFrom(cx, "name") + "' ";
                            extra += tagAttribute;
                        }
                        if (n.hasTag(WritePropertyExpressionTag.class)) {
                            String tagAttribute = "'" + getAttributeFrom(cx, "key") + "' ";
                            extra += tagAttribute;
                        }
                        if (n.hasTag(WriteVariableExpressionTag.class)) {
                            String tagAttribute = "'" + getAttributeFrom(cx, "name") + "' ";
                            extra += tagAttribute;
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
        for (Class<?> c : ALL) {
            if (node.hasTag((Class<? extends Tag>) c)) {
                tags += c.getSimpleName() + " ";
            }
        }
        return tags;
    }

}
