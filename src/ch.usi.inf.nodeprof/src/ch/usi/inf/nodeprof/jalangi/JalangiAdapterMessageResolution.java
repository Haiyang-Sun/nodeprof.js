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
package ch.usi.inf.nodeprof.jalangi;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.runtime.objects.Undefined;

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

    @Resolve(message = "INVOKE")
    abstract static class RunnableInvokeNode extends Node {
        @TruffleBoundary
        public Object access(JalangiAdapter adapter, String identifier, Object[] arguments) {
            if ("iidToLocation".equals(identifier)) {
                if (arguments.length == 1) {
                    Object result = null;
                    if (arguments[0] instanceof String) {
                        String iid = (String) arguments[0];
                        result = SourceMapping.getLocationForIID(Integer.parseInt(iid));
                    } else if (arguments[0] instanceof Long) {
                        Long iid = (Long) arguments[0];
                        result = SourceMapping.getLocationForIID(iid.intValue());
                    } else if (arguments[0] instanceof Integer) {
                        Integer iid = (Integer) arguments[0];
                        result = SourceMapping.getLocationForIID(iid);
                    } else {
                        try {
                            result = SourceMapping.getLocationForIID(Integer.parseInt(arguments[0].toString()));
                        } catch (Exception e) {
                            Logger.error("iidToLocation failed for argument type " + arguments[0].getClass().getName());
                            CompilerDirectives.transferToInterpreterAndInvalidate();
                            throw UnsupportedTypeException.raise(e, arguments);
                        }
                    }
                    return result == null ? Undefined.instance : result;
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
            } else {
                // unknown API
                Logger.warning("Unsupported NodeProf-Jalangi operation " + identifier);
            }
            return 0;
        }
    }

}
