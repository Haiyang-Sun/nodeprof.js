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

import java.util.ArrayList;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralExpressionTag;
import com.oracle.truffle.js.runtime.objects.Undefined;

import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.LiteralEventHandler;
import ch.usi.inf.nodeprof.utils.Logger;

public class LiteralFactory extends AbstractFactory {
    // an array of filters
    final private ArrayList<String> types;

    @TruffleBoundary
    public LiteralFactory(Object jalangiAnalysis, DynamicObject post) {
        super("literal", jalangiAnalysis, null, post, -1, 4);

        /*
         * as hasGetterSetter is a not always used feature and can be computed from the returned
         * literal object we don't pass any hasGetterOrSetter but for compatibility to Jalangi, we
         * still keep this argument in the hook.
         */
        setPostArguments(2, Undefined.instance);

        if (isPropertyUndefined(post, "types")) {
            types = null;
        } else {
            String literalTypes = readString(post, "types"); // get types filter separated by ','
            if (literalTypes == null || literalTypes.isEmpty()) {
                types = null;
            } else {
                types = new ArrayList<String>();
                for (String type : literalTypes.split(",")) {
                    if (type.isEmpty())
                        continue;
                    try {
                        LiteralExpressionTag.Type.valueOf(type);
                        types.add(type);
                    } catch (IllegalArgumentException e) {
                        Logger.warning("Ignored invalid type " + type + "given for the literal callback");
                    }
                }
            }
        }
    }

    @TruffleBoundary
    protected boolean isValidType(String someType) {
        // always return true if no type filter is given
        if (types == null || types.size() == 0)
            return true;
        return types.indexOf(someType) >= 0;
    }

    @Override
    public BaseEventHandlerNode create(EventContext context) {
        return new LiteralEventHandler(context) {
            private final boolean skip = !isValidType(getLiteralType());
            @Child DirectCallNode postCall = skip ? null : createPostCallNode();

            @Override
            public void executePost(VirtualFrame frame, Object result,
                            Object[] inputs) {
                if (post != null && !skip) {
                    setPostArguments(0, getSourceIID());
                    setPostArguments(1, convertResult(result));
                    setPostArguments(3, getLiteralType());
                    directCall(postCall, false, getSourceIID());
                }
            }

        };
    }

}
