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
package ch.usi.inf.nodeprof.jalangi;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.Undefined;

import ch.usi.inf.nodeprof.analysis.ProfilerExecutionEventNode;
import ch.usi.inf.nodeprof.utils.GlobalConfiguration;
import ch.usi.inf.nodeprof.utils.Logger;
import ch.usi.inf.nodeprof.utils.SourceMapping;

@MessageResolution(receiverType = JalangiAdapter.class)
public class JalangiAdapterMessageResolution {

    @CanResolve
    public abstract static class CanHandleTestMap extends Node {
        public boolean test(TruffleObject o) {
            return o instanceof JalangiAdapter;
        }
    }

    static private int convertIID(Object argument) {
        int iid;
        if (argument instanceof String) {
            String s = (String) argument;
            iid = Integer.parseInt(s);
        } else if (argument instanceof Long) {
            Long l = (Long) argument;
            iid = l.intValue();
        } else if (argument instanceof Integer) {
            iid = (Integer) argument;
        } else {
            iid = Integer.parseInt(argument.toString());
        }
        return iid;
    }

    static private boolean checkArguments(int count, Object[] arguments, String funcName) {
        if (arguments.length < count) {
            Logger.error("call to " + funcName + " expects " + count + " argument(s)");
            if (!GlobalConfiguration.IGNORE_JALANGI_EXCEPTION) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw ArityException.raise(count, arguments.length);
            }
            return false;
        } else if (arguments.length > count) {
            Logger.warning("extra arguments passed to " + funcName);
        }
        return true;
    }

    @Resolve(message = "INVOKE")
    abstract static class RunnableInvokeNode extends Node {
        @TruffleBoundary
        public Object access(JalangiAdapter adapter, String identifier, Object[] arguments) {
            if ("iidToLocation".equals(identifier)) {
                if (checkArguments(1, arguments, identifier)) {
                    Object result = null;
                    try {
                        result = SourceMapping.getLocationForIID(convertIID(arguments[0]));
                    } catch (Exception e) {
                        Logger.error("iidToLocation failed for argument type " + arguments[0].getClass().getName());
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        throw UnsupportedTypeException.raise(e, arguments);
                    }
                    return result == null ? Undefined.instance : result;
                }
            } else if (identifier.equals("iidToSourceObject")) {
                if (checkArguments(1, arguments, identifier)) {
                    try {
                        return SourceMapping.getJSObjectForIID(convertIID(arguments[0]));
                    } catch (Exception e) {
                        Logger.error("iidToSourceObject failed for argument type " + arguments[0].getClass().getName());
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        throw UnsupportedTypeException.raise(e, arguments);
                    }
                }
            } else if (identifier.equals("nativeLog")) {
                Logger.Level level = Logger.Level.INFO;
                if (arguments.length >= 2) {
                    int i = convertIID(arguments[1]);
                    Logger.Level[] enumValues = Logger.Level.values();
                    if (i >= 0 && i < enumValues.length) {
                        level = enumValues[i];
                    }
                }
                if (arguments.length > 0) {
                    Logger.log(arguments[0], level);
                    return 0;
                }
            } else if (identifier.equals("valueOf")) {
                return "jalangi-adapter";
            } else if (identifier.equals("onReady")) {
                if (arguments.length == 1) {
                    adapter.getNodeProfJalangi().onReady(arguments[0]);
                } else if (arguments.length == 2) {
                    if (!(arguments[1] instanceof TruffleObject)) {
                        Logger.warning("The second argument for onReady should be an object");
                    } else {
                        adapter.getNodeProfJalangi().onReady(arguments[0], (TruffleObject) arguments[1]);
                    }
                } else {
                    Logger.warning("onReady should take 1 or 2 arguments");
                }
            } else if (identifier.equals("registerCallback")) {
                adapter.getNodeProfJalangi().registerCallback(arguments[0], arguments[1], arguments[2]);
            } else if (identifier.equals("instrumentationSwitch")) {
                // update instrumentation using the first argument given and return the updated
                // status of the instrumentation (true for enabled and false for disabled)
                if (arguments.length >= 1) {
                    if (arguments[0] != null) {
                        boolean value = JSRuntime.toBoolean(arguments[0]);
                        ProfilerExecutionEventNode.updateEnabled(value);
                    }
                }
                return ProfilerExecutionEventNode.getEnabled();
            } else {
                // unknown API
                Logger.warning("Unsupported NodeProf-Jalangi operation " + identifier);
            }
            return 0;
        }
    }

}
