/* *****************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package ch.usi.inf.nodeprof.handlers;

import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.objects.Undefined;

import ch.usi.inf.nodeprof.ProfiledTagEnum;

/**
 * Abstract event handler for binary events
 */
public abstract class BinaryEventHandler extends BaseSingleTagEventHandler {
    private final TruffleString op;
    private final boolean isLogic;

    public BinaryEventHandler(EventContext context) {
        super(context, ProfiledTagEnum.BINARY);
        String internalOp = getAttributeInternalString("operator");
        isLogic = internalOp.equals("||") || internalOp.equals("&&");
        op = Strings.fromJavaString(internalOp);
    }

    /**
     * @return the operator
     */
    public Object getOp() {
        return this.op;
    }

    /**
     * @return the left operand from inputs[0]
     */
    public Object getLeft(Object[] inputs) {
        return assertGetInput(0, inputs, "left");
    }

    /**
     * @return the right operand from inputs[1]
     */
    public Object getRight(Object[] inputs) {
        /**
         * TODO
         *
         * remove the check after bug fix
         */
        if (inputs.length < 2) {
            return Undefined.instance;
        }
        return assertGetInput(1, inputs, "right");
    }

    /**
     * the logic operator '||' and '&&' are two special binary operations.
     *
     * e.g., true || right, false && right would only evaluate the left operand
     *
     * @return true if the operator is '||' or '&&'
     */
    public boolean isLogic() {
        return this.isLogic;
    }

    /**
     * @return 0 for logical operations and 2 for others
     */
    @Override
    public int expectedNumInputs() {
        if (isLogic()) {
            return 0;
        }
        return 2;
    }
}
