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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.instrumentation.EventContext;

/**
 * Abstract event handler for variable writes
 */
public abstract class VarEventHandler extends BaseEventHandlerNode {
    private final String name;
    private final boolean isInternal;

    @TruffleBoundary
    public static boolean isIdentifierInternal(String id) {
        return id.equals(":switch") || (id.startsWith("<") && !id.equals("<this>"));
    }

    public VarEventHandler(EventContext context) {
        super(context);
        this.name = (String) getAttribute("name");
        this.isInternal = isIdentifierInternal(name);
    }

    public String getName() {
        return this.name;
    }

    public boolean isInternal() {
        return this.isInternal;
    }
}
