/* *****************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.objects.JSObject;

public class ObjectHelper {

    public static void setConfigProperty(DynamicObject obj, String key, Object value) {
        Object safeValue = value instanceof String ? Strings.fromJavaString((String) value) : value;
        JSObject.set(obj, Strings.fromJavaString(key), safeValue);
    }

    public static void setConfigProperty(DynamicObject obj, String key, String value) {
        JSObject.set(obj, Strings.fromJavaString(key), Strings.fromJavaString(value));
    }

    public static void setConfigProperty(DynamicObject obj, String key, DynamicObject value) {
        JSObject.set(obj, Strings.fromJavaString(key), value);
    }

    public static void setConfigProperty(DynamicObject obj, String key, int value) {
        JSObject.set(obj, Strings.fromJavaString(key), value);
    }
}
