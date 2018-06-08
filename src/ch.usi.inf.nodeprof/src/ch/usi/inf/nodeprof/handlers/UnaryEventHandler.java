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

import ch.usi.inf.nodeprof.utils.GlobalObjectCache;

/**
 * Abstract event handler for unary operations
 */
public abstract class UnaryEventHandler extends BaseEventHandlerNode {
    private final String op;
    private final boolean isDelete;

    public UnaryEventHandler(EventContext context) {
        super(context);
        op = (String) getAttribute("operator");
        this.isDelete = op.equals("delete");
    }

    public String getOp() {
        return this.op;
    }

    public boolean isDelete() {
        return this.isDelete;
    }

    public Object getValue(Object[] inputs) {
        if (!isDelete) {
            return assertGetInput(0, inputs, "unaryValue");
        } else {
            Object target = assertGetInput(0, inputs, "delete target");
            Object key = assertGetInput(1, inputs, "delete key");
            return JSArray.createConstant(GlobalObjectCache.getInstance().getJSContext(), new Object[]{target, key});
        }
    }

    @Override
    public boolean isLastIndex(int inputCount, int index) {
        return isDelete ? index == 1 : index == 0;
    }
}
