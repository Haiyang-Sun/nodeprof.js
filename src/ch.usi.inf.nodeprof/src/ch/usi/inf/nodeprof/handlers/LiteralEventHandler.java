/*******************************************************************************
 * Copyright 2019 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
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
package ch.usi.inf.nodeprof.handlers;

import java.util.ArrayList;

import com.oracle.js.parser.ParserException;
import com.oracle.js.parser.ir.Expression;
import com.oracle.js.parser.ir.ObjectNode;
import com.oracle.js.parser.ir.PropertyNode;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralTag;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.objects.Undefined;

import ch.usi.inf.nodeprof.ProfiledTagEnum;
import ch.usi.inf.nodeprof.utils.GlobalObjectCache;
import ch.usi.inf.nodeprof.utils.Logger;

/**
 * Abstract event handler for literal events
 */
public abstract class LiteralEventHandler extends BaseSingleTagEventHandler {
    private final String literalType;
    // get it on-demand
    @CompilationFinal private Object literalMembers = null;
    @CompilationFinal private Object hasGetterSetter = null;

    public LiteralEventHandler(EventContext context) {
        super(context, ProfiledTagEnum.LITERAL);
        this.literalType = (String) getAttribute(LiteralTag.TYPE);
    }

    /**
     *
     * @return type of the literal, including ObjectLiteral, ArrayLiteral, FunctionLiteral,
     *         NumericLiteral, BooleanLiteral, StringLiteral, NullLiteral, UndefinedLiteral,
     *         RegExpLiteral,
     */
    public String getLiteralType() {
        return this.literalType;
    }

    public Object getObjectLiteralMembers(Object literalVal) {
        if (literalMembers != null) {
            return literalMembers;
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        if (this.literalType == LiteralTag.Type.ObjectLiteral.name()) {
            // use Graal.js parser
            try {
                JSContext jsContext = GlobalObjectCache.getInstance().getJSContext((DynamicObject) literalVal);
                Expression expression = jsContext.getEvaluator().parseExpression(jsContext, context.getInstrumentedSourceSection().getCharacters().toString());
                this.hasGetterSetter = false;
                ArrayList<Object> keys = new ArrayList<>();
                if (expression instanceof ObjectNode) {
                    ObjectNode objExpr = (ObjectNode) expression;
                    for (PropertyNode element : objExpr.getElements()) {
                        String flag = "";
                        if (element.getGetter() != null) {
                            flag += "getter";
                            hasGetterSetter = true;
                        }
                        if (element.getSetter() != null) {
                            flag += "setter";
                            hasGetterSetter = true;
                        }
                        String keyName = element.getKeyName();
                        keys.add(flag + "-" + keyName);
                    }
                }
                literalMembers = JSArray.createConstant(jsContext, keys.toArray());
            } catch (ParserException e) {
                // if the source section is wrong, the parser will fail
                Logger.warning(getSourceIID(), "Cannot parse object literal " + context.getInstrumentedSourceSection().getCharacters().toString());
                literalMembers = Undefined.instance;
                hasGetterSetter = false;
            }
            return literalMembers;
        } else {
            this.literalMembers = Undefined.instance;
            this.hasGetterSetter = false;
            return Undefined.instance;
        }

    }

    public Object hasGetterSetter(Object literalVal) {
        getObjectLiteralMembers(literalVal);
        assert (this.hasGetterSetter != null);
        return this.hasGetterSetter;
    }
}
