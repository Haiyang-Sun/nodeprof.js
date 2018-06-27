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
package ch.usi.inf.nodeprof.handlers;

import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Abstract event handler for function call events
 */
public abstract class FunctionCallEventHandler extends BaseEventHandlerNode {
    private final boolean isInvoke;
    private final boolean isNew;

    public FunctionCallEventHandler(EventContext context) {
        super(context);
        this.isInvoke = (boolean) getAttribute("isInvoke");
        this.isNew = (boolean) getAttribute("isNew");
    }

    public Object getFunction(Object[] inputs) {
        Object result;
        if (isNew) {
            result = assertGetInput(0, inputs, "func");
        } else {
            result = assertGetInput(1, inputs, "func");
        }
        assert JSFunction.isJSFunction(result);
        // TODO, in some cases in module.js we got null, to be fixed
        if (result == null) {
            result = Undefined.instance;
        }
        return result;
    }

    public String getFunctionName(Object[] inputs) {
        Object result = getFunction(inputs);
        return JSFunction.getName((DynamicObject) result);
    }

    public Object getReceiver(Object[] inputs) {
        Object result = isNew ? Undefined.instance : assertGetInput(0, inputs, "receiver");
        // TODO, in some cases in module.js we got null, to be fixed
        if (result == null) {
            result = Undefined.instance;
        }
        return result;
    }

    public Object getArgument(Object[] inputs, int index) {
        return assertGetInput(getOffSet() + index, inputs, "arg");
    }

    /**
     * @return the index of the first argument
     */
    public int getOffSet() {
        return isNew ? 1 : 2;
    }

    /**
     * @return how many objects after the arguments in the inputs
     */
    public int getTillEnd() {
        return 0;
    }

    public boolean isNew() {
        return this.isNew;
    }

    public boolean isInvoke() {
        return this.isInvoke;
    }

    /**
     * TODO
     *
     * last index needs to be updated
     */
    @Override
    public boolean isLastIndex(int inputCount, int index) {
        return index == inputCount - 1;
    }
}
