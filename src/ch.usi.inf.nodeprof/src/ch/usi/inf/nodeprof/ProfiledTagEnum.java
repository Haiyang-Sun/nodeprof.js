/*******************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package ch.usi.inf.nodeprof;

import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;

import ch.usi.inf.nodeprof.utils.GlobalConfiguration;
import ch.usi.inf.nodeprof.utils.Logger;

public enum ProfiledTagEnum {
    UNARY(JSTags.UnaryExpressionTag.class, -1), // have multiple case
    BINARY(JSTags.BinaryExpressionTag.class, 2),
    CF_COND(JSTags.ControlFlowBranchTag.class, -1), // to be checked
    CF_BRANCH(JSTags.ControlFlowBlockTag.class, -1), // to be checked
    CF_ROOT(JSTags.ControlFlowRootTag.class, 0), // to be checked
    EVAL(JSTags.EvalCallTag.class, 2),
    VAR_READ(JSTags.ReadVariableExpressionTag.class, 0),
    VAR_WRITE(JSTags.WriteVariableExpressionTag.class, 1),
    PROPERTY_READ(JSTags.ReadPropertyExpressionTag.class, 1),
    PROPERTY_WRITE(JSTags.WritePropertyExpressionTag.class, 2),
    ELEMENT_READ(JSTags.ReadElementExpressionTag.class, 2),
    ELEMENT_WRITE(JSTags.WriteElementExpressionTag.class, 3),
    INVOKE(JSTags.FunctionCallExpressionTag.class, -1), // any number of inputs for arguments
    ROOT(StandardTags.RootTag.class, 0),
    BUILTIN(JSTags.BuiltinRootTag.class, 0),
    LITERAL(JSTags.LiteralExpressionTag.class, 0),
    NEW(JSTags.ObjectAllocationExpressionTag.class, -1),
    ASYNC_ROOT(JSTags.AsyncRootTag.class, 1), // TODO
    AWAIT(JSTags.AwaitTag.class, 1); // TODO

    // the corresponding JSTags class
    private final Class<? extends Tag> clazz;

    // the number of input arguments the tagged node always expects
    // -1 means unknown
    private final int expectedNumInputs;

    public int getExpectedNumInputs() {
        return expectedNumInputs;
    }

    /**
     * counting the instrumentation
     */
    public int usedAnalysis = 0;
    public double nodeCount = 0;
    public double preHitCount = 0;
    public double postHitCount = 0;
    public double exceptionHitCount = 0;

    ProfiledTagEnum(Class<? extends Tag> clazz, int expectedNumInputs) {
        this.clazz = clazz;
        this.expectedNumInputs = expectedNumInputs;
    }

    public Class<? extends Tag> getTag() {
        return this.clazz;
    }

    public static void dump() {
        if (GlobalConfiguration.DEBUG) {
            for (ProfiledTagEnum cb : ProfiledTagEnum.values()) {
                if (cb.usedAnalysis > 0) {
                    Logger.debug("Callback registered times: " + cb.toString() + " " + cb.usedAnalysis);
                    if (cb.nodeCount > 0) {
                        Logger.debug("InstrumentedNodes: " + cb.toString() + " " + cb.nodeCount);
                    }
                    if (cb.preHitCount > 0) {
                        Logger.debug("CounterPre: " + cb.toString() + " " + cb.preHitCount);
                    }
                    if (cb.postHitCount > 0) {
                        Logger.debug("CounterPost: " + cb.toString() + " " + cb.postHitCount);
                    }
                }
            }
        }
    }

    public static Class<?>[] getTags() {
        Class<?>[] res = new Class<?>[ProfiledTagEnum.values().length];
        for (int i = 0; i < ProfiledTagEnum.values().length; i++) {
            res[i] = ProfiledTagEnum.values()[i].getTag();
        }
        return res;
    }
}
