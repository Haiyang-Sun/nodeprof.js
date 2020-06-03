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

import java.io.PrintStream;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.runtime.GraalJSException;

public class Logger {
    public enum Level {
        DEBUG,   // 0
        INFO,    // 1
        WARNING, // 2
        ERROR    // 3
    }

    private static PrintStream outStream = System.out;
    private static PrintStream errStream = System.err;

    public static void init(PrintStream out, PrintStream err) {
        outStream = out;
        errStream = err;
    }

    @TruffleBoundary
    private static void print(PrintStream stream, String tag, Object msg) {
        stream.printf("[%s] %s\n", tag, msg.toString());
    }

    @TruffleBoundary
    private static void print(PrintStream stream, String tag, String sourceHint, Object msg) {
        stream.printf("[%s] %s: %s\n", tag, sourceHint, msg.toString());
    }

    private static void print(PrintStream stream, String tag, SourceSection sourceSection, Object msg) {
        print(stream, tag, SourceMapping.makeLocationString(sourceSection).toString(), msg.toString());
    }

    public static void log(Object msg, Level l) {
        switch (l) {
            case DEBUG:
                debug(msg);
                break;
            case INFO:
                info(msg);
                break;
            case WARNING:
                warning(msg);
                break;
            case ERROR:
                error(msg);
                break;
        }
    }

    public static void info(Object msg) {
        print(outStream, "i", msg);
    }

    public static void info(SourceSection sourceSection, Object msg) {
        print(outStream, "i", sourceSection, msg);
    }

    public static void info(int iid, Object msg) {
        info(SourceMapping.getSourceSectionForIID(iid), msg);
    }

    public static void debug(SourceSection sourceSection, Object msg) {
        if (GlobalConfiguration.DEBUG) {
            print(outStream, "d", sourceSection, msg);
        }
    }

    public static void debug(int iid, Object msg) {
        debug(SourceMapping.getSourceSectionForIID(iid), msg);
    }

    public static void debug(Object msg) {
        if (GlobalConfiguration.DEBUG) {
            print(outStream, "d", msg);
        }
    }

    public static void warning(SourceSection sourceSection, Object msg) {
        print(outStream, "w", sourceSection, msg);
    }

    public static void warning(int iid, Object msg) {
        warning(SourceMapping.getSourceSectionForIID(iid), msg);
    }

    public static void warning(Object msg) {
        print(errStream, "w", msg);
    }

    public static void error(SourceSection sourceSection, Object msg) {
        print(errStream, "e", sourceSection, msg);
    }

    public static void error(int iid, Object msg) {
        error(SourceMapping.getSourceSectionForIID(iid), msg);
    }

    public static void error(Object iidOrObject, Object msg) {
        if (iidOrObject instanceof Integer) {
            error((int) iidOrObject, msg);
        } else {
            print(errStream, "e", iidOrObject.toString(), msg);
        }
    }

    @TruffleBoundary
    public static void error(int iid, Object msg, Class<?> someClass) {
        error(SourceMapping.getSourceSectionForIID(iid), msg + "@class[" + someClass.getSimpleName() + "]");
    }

    public static void error(Object msg) {
        print(errStream, "e", msg);
    }

    /**
     *
     * print source section with code
     *
     * should not be used in performance-critical parts
     *
     * @param sourceSection
     * @return string builder for the code
     */
    @TruffleBoundary
    public static StringBuilder printSourceSectionWithCode(
                    SourceSection sourceSection) {
        StringBuilder b = new StringBuilder();
        if (sourceSection != null) {
            String fileName = sourceSection.getSource().getName();
            b.append(fileName.substring(fileName.lastIndexOf('/') + 1));
            b.append("(line ");
            b.append(sourceSection.getStartLine()).append("):");
            String code = sourceSection.getCharacters().toString().replaceAll("\\s", "");
            if (code.length() > 20) {
                code = code.substring(0, 20);
            }
            b.append(code);
        }
        return b;
    }

    @TruffleBoundary
    public static void reportJSException(GraalJSException e) {
        Logger.error(e.getMessage());
        GraalJSException.JSStackTraceElement[] trace = e.getJSStackTrace();
        if (GlobalConfiguration.IGNORE_JALANGI_EXCEPTION) {

            for (GraalJSException.JSStackTraceElement jsste : trace) {
                Logger.error(jsste);
            }
        } else {
            e.printJSStackTrace();
            System.exit(-1);
        }
    }
}
