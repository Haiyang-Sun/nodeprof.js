/* *****************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package ch.usi.inf.nodeprof.utils;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.objects.JSObject;

/**
 *
 * To store global objects such as the global instance, Array constructor and so on We need a
 * dynamic object in order to get the JSContext and further get these global objects
 *
 */
public class GlobalObjectCache extends Node {
    @CompilationFinal private DynamicObject global = null;
    @CompilationFinal private JSContext jscontext = null;
    @CompilationFinal private DynamicObject arrayConstructor = null;
    @CompilationFinal private DynamicObject emptyWrappedException = null;

    private static GlobalObjectCache cache = new GlobalObjectCache();

    public static GlobalObjectCache getInstance() {
        return cache;
    }

    public static void reset() {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        cache = new GlobalObjectCache();
    }

    public DynamicObject getGlobal() {
        return global;
    }

    public DynamicObject getArrayConstructor(DynamicObject option) {
        if (jscontext == null) {
            addDynamicObject(option);
        }
        return arrayConstructor;
    }

    public JSContext getJSContext() {
        return jscontext;
    }

    /**
     *
     * @param object the dynamic object which could be used to find out the JSContext
     * @return The js context
     *
     */
    public JSContext getJSContext(DynamicObject object) {
        if (jscontext == null) {
            addDynamicObject(object);
        }
        return jscontext;
    }

    /**
     * @param someObj a dynamic object which could tell us the jsContext information
     */
    public void addDynamicObject(DynamicObject someObj) {
        if (jscontext == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            jscontext = JSObject.getJSContext(someObj);
            if (global == null) {
                global = JSRealm.get(null).getGlobalObject();
            }
            if (arrayConstructor == null) {
                arrayConstructor = JSRealm.get(null).getArrayConstructor();
            }
        }
    }

    /**
     * @param someObj an object which could tell us the jsContext information
     */
    public void addObject(Object someObj) {
        if (someObj instanceof DynamicObject) {
            addDynamicObject((DynamicObject) someObj);
        }
    }

    /**
     * @return Singleton object for empty wrapped exception
     */
    public Object getEmptyWrappedException() {
        if (this.emptyWrappedException == null) {
            assert (jscontext != null);
            CompilerDirectives.transferToInterpreterAndInvalidate();
            this.emptyWrappedException = JSOrdinary.create(jscontext, JSRealm.get(null));
        }
        return this.emptyWrappedException;
    }
}
