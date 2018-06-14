/*******************************************************************************
 * Copyright [2018] [Haiyang Sun, Università della Svizzera Italiana (USI)]
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

import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.objects.Undefined;

import ch.usi.inf.nodeprof.utils.GlobalObjectCache;

/**
 * Abstract event handler for unary operations
 */
public abstract class UnaryEventHandler extends BaseEventHandlerNode {
    private final String op;
    private final boolean isDelete;
    private final boolean isVoid;

    public UnaryEventHandler(EventContext context) {
        super(context);
        op = (String) getAttribute("operator");
        this.isDelete = op.equals("delete");
        this.isVoid = op.equals("void");
    }

    public String getOp() {
        return this.op;
    }

    public boolean isDelete() {
        return this.isDelete;
    }

    public boolean isVoid() {
        return this.isVoid;
    }

    public Object getValue(Object[] inputs) {
        if (this.isVoid) {
            return Undefined.instance;
        } else if (isDelete) {
            Object target = assertGetInput(0, inputs, "delete target");
            Object key = assertGetInput(1, inputs, "delete key");
            return JSArray.createConstant(GlobalObjectCache.getInstance().getJSContext(), new Object[]{target, key});
        } else {
            return assertGetInput(0, inputs, "unaryValue");
        }
    }

    @Override
    public boolean isLastIndex(int inputCount, int index) {
        if (this.isDelete) {
            // delete object key need two arguments
            return index == 1;
        } else if (this.isVoid) {
            // void needs no argument because it always returns undefined
            return index == -1;
        } else {
            return index == 0;
        }
    }
}
