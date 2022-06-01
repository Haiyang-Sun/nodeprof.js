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

import ch.usi.inf.nodeprof.ProfiledTagEnum;
import ch.usi.inf.nodeprof.utils.GlobalObjectCache;
import com.oracle.truffle.api.strings.TruffleString;

/**
 * Abstract event handler for property write events, e.g., a.p = 1
 */
public abstract class PropertyWriteEventHandler extends BaseSingleTagEventHandler {
    private final TruffleString property;

    public PropertyWriteEventHandler(EventContext context) {
        super(context, ProfiledTagEnum.PROPERTY_WRITE);
        this.property = getAttributeTString("key");
    }

    public Object getReceiver(Object[] inputs) {
        Object result = assertGetInput(0, inputs, "receiver");
        GlobalObjectCache.getInstance().addObject(result);
        return result;
    }

    public Object getProperty() {
        return this.property;
    }

    public Object getValue(Object[] inputs) {
        assert (inputs.length <= 2);
        if (inputs.length > 2) {
            return assertGetInput(2, inputs, "value");
        } else {
            return assertGetInput(1, inputs, "value");
        }
    }

    public boolean isGlobal(Object[] inputs) {
        boolean result = getReceiver(inputs) == GlobalObjectCache.getInstance().getGlobal();
        assert (GlobalObjectCache.getInstance().getGlobal() != null);
        return result;
    }

    /**
     * TODO: unsupported yet
     *
     * @return True if the operation is of the form <code>o.p op= e</code>
     */
    public boolean isOpAssign() {
        return false;
    }

}
