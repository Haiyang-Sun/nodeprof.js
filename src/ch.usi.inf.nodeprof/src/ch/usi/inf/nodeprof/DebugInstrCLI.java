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
package ch.usi.inf.nodeprof;

import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptor;
import org.graalvm.options.OptionKey;

import com.oracle.truffle.api.Option;

@Option.Group(DebugInstrument.ID)
public class DebugInstrCLI {

    static final String enabledHelp = "Enable the Nodeprof instrumentation agent.";
    @Option(name = "", help = enabledHelp, category = OptionCategory.USER)//
    static final OptionKey<Boolean> ENABLED = new OptionKey<>(true);

    static final String debugHelp = "Enable verbose debugging information.";
    @Option(name = "Debug", help = debugHelp, category = OptionCategory.USER)//
    public static final OptionKey<Boolean> DEBUG = new OptionKey<>(false);

    static final String traceEventsHelp = "Enable low-level instrumentation tracing (expert).";
    @Option(name = "TraceEvents", help = traceEventsHelp, category = OptionCategory.USER)//
    public static final OptionKey<Boolean> TRACE_EVENTS = new OptionKey<>(false);

    static final String analysisHelp = "Analysis to be enabled in nodeprof (separated by ','). By default NodeProfJalangi";
    @Option(name = "Analysis", help = analysisHelp, category = OptionCategory.USER)//
    public static final OptionKey<String> ANALYSIS = new OptionKey<>("NodeProfJalangi");

    static final String exclHelp = "Exclusion list of source code files (separated by ',').";
    @Option(name = "ExcludeSource", help = exclHelp, category = OptionCategory.USER)//
    public static final OptionKey<String> EXCLUDE_SOURCE = new OptionKey<>("");

    static final String scopeHelp = "Instrumentation scope: 'app', 'module', or 'all'.";
    @Option(name = "Scope", help = scopeHelp, category = OptionCategory.USER)//
    public static final OptionKey<String> SCOPE = new OptionKey<>("module");

    static final String ignoreJExpHelp = "Keep going even if a JS exception was thrown in a Jalangi callback handler";
    @Option(name = "IgnoreJalangiException", help = ignoreJExpHelp, category = OptionCategory.USER)//
    public static final OptionKey<Boolean> IGNORE_JALANGI_EXCEPTION = new OptionKey<>(false);

    static final String logAbsPathHelp = "Use absolute instead of relative source path in logs";
    @Option(name = "LogAbsolutePath", help = logAbsPathHelp, category = OptionCategory.USER)//
    public static final OptionKey<Boolean> LOG_ABSOLUTE_PATH = new OptionKey<>(false);

    public static OptionDescriptor[] ods = {
                    OptionDescriptor.newBuilder(ENABLED, "debugi").deprecated(false).help(enabledHelp).category(OptionCategory.USER).build(),
                    OptionDescriptor.newBuilder(DEBUG, "debugi.Debug").deprecated(false).help(debugHelp).category(OptionCategory.USER).build(),
                    OptionDescriptor.newBuilder(TRACE_EVENTS, "debugi.TraceEvents").deprecated(false).help(traceEventsHelp).category(OptionCategory.USER).build(),
                    OptionDescriptor.newBuilder(ANALYSIS, "debugi.Analysis").deprecated(false).help(analysisHelp).category(OptionCategory.USER).build(),
                    OptionDescriptor.newBuilder(EXCLUDE_SOURCE, "debugi.ExcludeSource").deprecated(false).help(exclHelp).category(OptionCategory.USER).build(),
                    OptionDescriptor.newBuilder(SCOPE, "debugi.Scope").deprecated(false).help(scopeHelp).category(OptionCategory.USER).build(),
                    OptionDescriptor.newBuilder(IGNORE_JALANGI_EXCEPTION, "debugi.IgnoreJalangiException").deprecated(false).help(ignoreJExpHelp).category(OptionCategory.USER).build(),
                    OptionDescriptor.newBuilder(LOG_ABSOLUTE_PATH, "debugi.LogAbsolutePath").deprecated(false).help(logAbsPathHelp).category(OptionCategory.USER).build(),
    };
}
