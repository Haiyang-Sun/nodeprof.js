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
package ch.usi.inf.nodeprof.jalangi.factory;

import java.util.EnumSet;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralTag;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.objects.Undefined;

import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.LiteralEventHandler;
import ch.usi.inf.nodeprof.utils.GlobalObjectCache;
import ch.usi.inf.nodeprof.utils.Logger;

public class LiteralFactory extends AbstractFactory {
    // enabled literal types (all by default)
    final private EnumSet<LiteralTag.Type> types = EnumSet.allOf(LiteralTag.Type.class);

    @TruffleBoundary
    public LiteralFactory(Object jalangiAnalysis, DynamicObject post) {
        super("literal", jalangiAnalysis, null, post, -1, 5);

        if (!isPropertyUndefined(post, "types")) {
            Object[] literalTypes = readArray(post, "types");
            if (literalTypes != null) {
                // filter property has been set, start with empty set and add only specified types
                types.removeAll(EnumSet.allOf(LiteralTag.Type.class));
                for (Object elem : literalTypes) {
                    try {
                        LiteralTag.Type enumType = LiteralTag.Type.valueOf(elem.toString());
                        types.add(enumType);
                    } catch (IllegalArgumentException e) {
                        Logger.warning("Ignored invalid type " + elem.toString() + " given for the literal callback");
                    }
                }
            }
        }
    }

    @Override
    public BaseEventHandlerNode create(EventContext context) {
        return new LiteralEventHandler(context) {
            private final boolean skip = !types.contains(LiteralTag.Type.valueOf(getLiteralType()));
            @Child DirectCallNode postCall = skip ? null : createPostCallNode();

            @Override
            public void executePost(VirtualFrame frame, Object result,
                            Object[] inputs) {
                if (post != null && !skip) {
                    /**
                     * TODO, move constant argument setting to Node constructor and see if the
                     * performance is better e.g., getSourceIID, hasGetterSetter, literalType,
                     * literalMembers are all constant specific to the current node
                     **/
                    setPostArguments(0, getSourceIID());
                    setPostArguments(1, convertResult(result));
                    setPostArguments(2, hasGetterSetter(result));
                    setPostArguments(3, getLiteralType());
                    setPostArguments(4, getObjectLiteralMembers(result));
                    directCall(postCall, false, getSourceIID());
                }
            }

        };
    }

}
