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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.instrumentation.EventContext;

import ch.usi.inf.nodeprof.ProfiledTagEnum;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Strings;

/**
 * Abstract event handler for expression events
 */
public abstract class ExpressionEventHandler extends BaseSingleTagEventHandler {
    final TruffleString expressionType;

    @TruffleBoundary
    public ExpressionEventHandler(EventContext context, ProfiledTagEnum tag) {
        super(context, tag);
        String nodeName = context.getInstrumentedNode().getClass().getSimpleName();
        // TODO, use more abstract types instead in future, e.g., similar to ESTree
        // currently, use the instrumented node's type (slightly trimmed)
        if (nodeName.lastIndexOf("Node") > 0) {
            this.expressionType = Strings.fromJavaString(nodeName.substring(0, nodeName.lastIndexOf("Node")));
        } else {
            this.expressionType = Strings.fromJavaString(nodeName);
        }
    }

    public Object getExpressionType() {
        return this.expressionType;
    }
}
