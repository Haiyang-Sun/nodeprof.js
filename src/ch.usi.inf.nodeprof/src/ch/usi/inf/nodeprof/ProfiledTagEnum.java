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
package ch.usi.inf.nodeprof;

import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;

import ch.usi.inf.nodeprof.utils.GlobalConfiguration;
import ch.usi.inf.nodeprof.utils.Logger;

public enum ProfiledTagEnum {
    UNARY(JSTags.UnaryExpressionTag.class),
    BINARY(JSTags.BinaryExpressionTag.class),
    CF_COND(JSTags.ControlFlowBranchTag.class),
    CF_BRANCH(JSTags.ControlFlowBlockTag.class),
    CF_ROOT(JSTags.ControlFlowRootTag.class),
    EVAL(JSTags.EvalCallTag.class),
    VAR_READ(JSTags.ReadVariableExpressionTag.class),
    VAR_WRITE(JSTags.WriteVariableExpressionTag.class),
    PROPERTY_READ(JSTags.ReadPropertyExpressionTag.class),
    PROPERTY_WRITE(JSTags.WritePropertyExpressionTag.class),
    ELEMENT_READ(JSTags.ReadElementExpressionTag.class),
    ELEMENT_WRITE(JSTags.WriteElementExpressionTag.class),
    INVOKE(JSTags.FunctionCallExpressionTag.class),
    ROOT(StandardTags.RootTag.class),
    BUILTIN(JSTags.BuiltinRootTag.class),
    LITERAL(JSTags.LiteralExpressionTag.class),
    NEW(JSTags.ObjectAllocationExpressionTag.class);

    private final Class<? extends Tag> clazz;

    /**
     * counting the instrumentation
     */
    public int usedAnalysis = 0;
    public double nodeCount = 0;
    public double preHitCount = 0;
    public double postHitCount = 0;
    public double exceptionHitCount = 0;

    ProfiledTagEnum(Class<? extends Tag> clazz) {
        this.clazz = clazz;
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
