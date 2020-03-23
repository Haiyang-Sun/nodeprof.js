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
package ch.usi.inf.nodeprof.utils;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Env;

import ch.usi.inf.nodeprof.NodeProfCLI;

public class GlobalConfiguration {

    /**
     * global instrumentation scope: "app", "module", and "all".
     *
     * app => excluding node_modules and internal modules; module => excluding only internal
     * modules; all => not excluding any modules
     *
     * could still be further filtered by additional exclusion list @See GlobalConfiguration.excl
     */
    @CompilationFinal public static String SCOPE;

    /**
     * excluding code files
     */
    @CompilationFinal public static String EXCL;

    /**
     * class names of enabled analysis separated by "," e.g., "jalangi.JalangiTruffle"
     */
    @CompilationFinal public static String ANALYSIS = System.getProperty("nodeprof.analysis");

    /**
     * keep going even if a JS exception was thrown in a Jalangi callback handler
     */
    @CompilationFinal public static boolean IGNORE_JALANGI_EXCEPTION;

    /**
     * enable all logging
     */
    @CompilationFinal public static boolean DEBUG;

    /**
     * trace all runtime events as produced by Graal.js
     */
    @CompilationFinal public static boolean DEBUG_TRACING;

    /**
     * use absolute instead of relative path in logs
     */
    @CompilationFinal public static boolean LOG_ABSOLUTE_PATH;

    /**
     * use synthetic locations for modules, wrappers in iidToLocation
     */
    @CompilationFinal public static boolean SYMBOLIC_LOCATIONS;

    @TruffleBoundary
    public static void setup(Env env) {
        DEBUG_TRACING = env.getOptions().get(NodeProfCLI.TRACE_EVENTS);
        DEBUG = env.getOptions().get(NodeProfCLI.DEBUG);
        ANALYSIS = env.getOptions().get(NodeProfCLI.ANALYSIS);
        SCOPE = env.getOptions().get(NodeProfCLI.SCOPE);
        EXCL = env.getOptions().get(NodeProfCLI.EXCLUDE_SOURCE);
        IGNORE_JALANGI_EXCEPTION = env.getOptions().get(NodeProfCLI.IGNORE_JALANGI_EXCEPTION);
        LOG_ABSOLUTE_PATH = env.getOptions().get(NodeProfCLI.LOG_ABSOLUTE_PATH);
        SYMBOLIC_LOCATIONS = env.getOptions().get(NodeProfCLI.SYMBOLIC_LOCATIONS);
    }
}
