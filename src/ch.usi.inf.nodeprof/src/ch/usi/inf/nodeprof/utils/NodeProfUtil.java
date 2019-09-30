/*******************************************************************************
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package ch.usi.inf.nodeprof.utils;

import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.nodes.access.GlobalScopeVarWrapperNode;
import com.oracle.truffle.js.nodes.instrumentation.JSInputGeneratingNodeWrapper;
import com.oracle.truffle.js.nodes.instrumentation.JSTaggedExecutionNode;

public class NodeProfUtil {
    public static Node getParentSkipWrappers(Node n) {
        assert n != null;
        Node parent = n.getParent();
        while (parent != null && (isWrapperNode(parent) || parent.getSourceSection() == null)) {
            parent = parent.getParent();
        }
        return parent;
    }

    public static boolean isWrapperNode(Node n) {
        return n instanceof JSInputGeneratingNodeWrapper || n instanceof InstrumentableNode.WrapperNode ||
                n instanceof GlobalScopeVarWrapperNode || n instanceof JSTaggedExecutionNode;
    }
}
