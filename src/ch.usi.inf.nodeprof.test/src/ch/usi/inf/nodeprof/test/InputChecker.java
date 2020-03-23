/* *****************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package ch.usi.inf.nodeprof.test;

import com.oracle.truffle.js.runtime.builtins.JSFunction;

import ch.usi.inf.nodeprof.ProfiledTagEnum;
import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;

public class InputChecker {
    public static boolean checkInput(ProfiledTagEnum tag, BaseEventHandlerNode node, Object[] inputs) {
        boolean res = true;
        switch (tag) {
            case BINARY:
                /**
                 * 0 - left, 1 - right
                 *
                 * left op right
                 */
                if (inputs == null) {
                    return false;
                }
                if (inputs.length != 2) {
                    return false;
                }
                break;
            case UNARY:
                /**
                 * 0 - operand
                 *
                 * unaryop oprand
                 */
                if (inputs == null) {
                    return false;
                }
                boolean isDelete = node.getAttribute("operator").equals("delete");
                if (!isDelete) {
                    return inputs.length == 1;
                } else {
                    return inputs.length == 2;
                }
            case ELEMENT_READ:
                /**
                 * 0 - receiver, 1 - offset
                 *
                 * receiver[offset]
                 */
                if (inputs == null) {
                    return false;
                }
                if (inputs.length != 2) {
                    return false;
                }
                break;
            case ELEMENT_WRITE:
                /**
                 * 0 - receiver, 1 - offset, 2 - value
                 *
                 * receiver[offset] = value
                 */
                if (inputs == null) {
                    return false;
                }
                if (inputs.length != 3) {
                    return false;
                }
                break;
            case PROPERTY_READ:
                /**
                 * 0 - receiver
                 *
                 * receiver.property
                 */
                if (inputs == null) {
                    return false;
                }

                if (node.getAttribute("key") == null) {
                    return false;
                }
                /**
                 * TODO
                 *
                 * a.b() can have 2 input slots
                 */
                if (inputs.length < 1) {
                    return false;
                }
                break;
            case PROPERTY_WRITE:
                /**
                 * 0 - receiver, 1 - value read
                 */
                if (inputs == null) {
                    return false;
                }
                if (inputs.length != 2) {
                    return false;
                }
                break;
            case CF_BRANCH:
                /**
                 * no inputs needed
                 */
                break;
            case LITERAL:
                /**
                 * no inputs needed
                 */
                break;
            case VAR_READ:
                /**
                 * no inputs needed
                 */
                break;
            case VAR_WRITE:
                /**
                 * 0 - value written
                 *
                 * a = Expr.
                 */
                if (inputs == null) {
                    return false;
                }
                if (inputs.length != 1) {
                    return false;
                }
                break;
            case INVOKE:
                /**
                 * 0 - receiver, 1 - function, [2-*] arguments
                 *
                 * receiver.function(arguments) / [undefined.]function(arguments)
                 *
                 */
                if (inputs == null) {
                    return false;
                }
                if (inputs.length < 1) {
                    return false;
                }
                if ((Boolean) node.getAttribute("isNew")) {
                    if (!JSFunction.isJSFunction(inputs[0])) {
                        return false;
                    }
                } else {
                    if (!JSFunction.isJSFunction(inputs[1])) {
                        return false;
                    }
                }
                break;
            case ROOT:
            case EVAL:
            case BUILTIN:
                /**
                 * no inputs needed
                 */
                break;
            default:
                break;
        }
        return res;
    }
}
