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
package ch.usi.inf.nodeprof.jalangi;

import java.util.Arrays;

import org.graalvm.options.OptionDescriptor;
import org.graalvm.options.OptionValues;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

import ch.usi.inf.nodeprof.NodeProfCLI;
import ch.usi.inf.nodeprof.analysis.ProfilerExecutionEventNode;
import ch.usi.inf.nodeprof.utils.GlobalConfiguration;
import ch.usi.inf.nodeprof.utils.GlobalObjectCache;
import ch.usi.inf.nodeprof.utils.Logger;
import ch.usi.inf.nodeprof.utils.SourceMapping;

/**
 * Java class exposed to the Jalangi framework
 */
@ExportLibrary(InteropLibrary.class)
public class JalangiAdapter implements TruffleObject {
    private final NodeProfJalangi nodeprofJalangi;

    enum ApiMember {
        IIDTOLOCATION("iidToLocation"),
        IIDTOCODE("iidToCode"),
        IIDTOSOURCEOBJECT("iidToSourceObject"),
        NATIVELOG("nativeLog"),
        VALUEOF("valueOf"),
        ONREADY("onReady"),
        REGISTERCALLBACK("registerCallback"),
        INSTRUMENTATIONSWITCH("instrumentationSwitch"),
        GETCONFIG("getConfig");

        final String name;

        ApiMember(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }

    }

    @TruffleBoundary
    private static String[] getApiMembers() {
        return Arrays.stream(ApiMember.values()).map(ApiMember::toString).toArray(String[]::new);
    }

    final String[] members = getApiMembers();

    @TruffleBoundary
    public JalangiAdapter(NodeProfJalangi nodeprofJalangi) {
        this.nodeprofJalangi = nodeprofJalangi;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    final boolean hasMembers() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    final boolean isMemberInvocable(@SuppressWarnings("unused") String member) {
        return true;
    }

    @SuppressWarnings({"static-method", "unused"})
    @ExportMessage
    final Object getMembers(boolean includeInternal) {
        return JSArray.createConstantObjectArray(GlobalObjectCache.getInstance().getJSContext(), members);
    }

    @TruffleBoundary
    private static int convertIID(Object argument) {
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

    @TruffleBoundary
    private static boolean checkArguments(int count, Object[] arguments, String funcName) throws ArityException {
        if (arguments.length < count) {
            Logger.error("call to " + funcName + " expects " + count + " argument(s)");
            if (!GlobalConfiguration.IGNORE_JALANGI_EXCEPTION) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw ArityException.create(count, arguments.length);
            }
            return false;
        } else if (arguments.length > count) {
            Logger.warning("extra arguments passed to " + funcName);
        }
        return true;
    }

    @TruffleBoundary
    private Object getConfig() {
        JSContext ctx = GlobalObjectCache.getInstance().getJSContext();
        DynamicObject obj = JSUserObject.create(ctx);
        OptionValues opts = this.getNodeProfJalangi().getEnv().getOptions();
        for (OptionDescriptor o: NodeProfCLI.ods) {
            String shortKey = o.getName().replace("nodeprof.", "");
            JSObject.set(obj, shortKey, opts.get(o.getKey()));
        }
        return obj;
    }

    @ExportMessage
    final Object invokeMember(String identifier, Object[] arguments) throws ArityException, UnsupportedTypeException {
        ApiMember api;
        try {
            api = ApiMember.valueOf(identifier.toUpperCase());
        } catch (IllegalArgumentException e) {
            Logger.warning("Unsupported NodeProf-Jalangi operation " + identifier);
            return 0;
        }
        switch (api) {
            case IIDTOLOCATION: {
                if (checkArguments(1, arguments, identifier)) {
                    Object result = null;
                    try {
                        result = SourceMapping.getLocationForIID(convertIID(arguments[0]));
                    } catch (Exception e) {
                        Logger.error("iidToLocation failed for argument type " + arguments[0].getClass().getName());
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        throw UnsupportedTypeException.create(new Object[]{arguments[0]});
                    }
                    return result == null ? Undefined.instance : result;
                }
                break;
            }
            case IIDTOCODE: {
                if (checkArguments(1, arguments, identifier)) {
                    Object result = null;
                    try {
                        result = SourceMapping.getCodeForIID(convertIID(arguments[0]));
                    } catch (Exception e) {
                        Logger.error("iidToCode failed for argument type " + arguments[0].getClass().getName());
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        throw UnsupportedTypeException.create(new Object[]{arguments[0]});
                    }
                    return result == null ? Undefined.instance : result;
                }
                break;
            }
            case IIDTOSOURCEOBJECT: {
                if (checkArguments(1, arguments, identifier)) {
                    try {
                        return SourceMapping.getJSObjectForIID(convertIID(arguments[0]));
                    } catch (Exception e) {
                        Logger.error("iidToSourceObject failed for argument type " + arguments[0].getClass().getName());
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        throw UnsupportedTypeException.create(new Object[]{arguments[0]});
                    }
                }
                break;
            }
            case NATIVELOG: {
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
                }
                break;
            }
            case VALUEOF: {
                return "jalangi-adapter";
            }
            case ONREADY: {
                if (arguments.length == 1) {
                    this.getNodeProfJalangi().onReady(arguments[0]);
                } else if (arguments.length == 2) {
                    if (!(arguments[1] instanceof TruffleObject)) {
                        Logger.warning("The second argument for onReady should be an object");
                    } else {
                        this.getNodeProfJalangi().onReady(arguments[0], (TruffleObject) arguments[1]);
                    }
                } else {
                    Logger.warning("onReady should take 1 or 2 arguments");
                }
                break;
            }
            case REGISTERCALLBACK: {
                this.getNodeProfJalangi().registerCallback(arguments[0], arguments[1], arguments[2]);
                break;
            }
            case INSTRUMENTATIONSWITCH: {
                if (arguments.length >= 1) {
                    if (arguments[0] != null) {
                        boolean value = JSRuntime.toBoolean(arguments[0]);
                        ProfilerExecutionEventNode.updateEnabled(value);
                    }
                }
                return ProfilerExecutionEventNode.getEnabled();
            }
            case GETCONFIG: {
                return getConfig();
            }

            default: {
                Logger.warning("Unsupported NodeProf-Jalangi operation " + identifier);
            }
        }
        return 0;
    }

    public NodeProfJalangi getNodeProfJalangi() {
        return this.nodeprofJalangi;
    }
}
