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
package ch.usi.inf.nodeprof.utils;

import java.io.File;
import java.io.PrintStream;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.runtime.GraalJSException;

public class Logger {
    private static PrintStream out = System.out;
    private static PrintStream err = System.err;

    public static void init(PrintStream _out, PrintStream _err) {
        out = _out;
        err = _err;
    }

    @TruffleBoundary
    private static void print(PrintStream stream, String tag, Object msg) {
        stream.printf("[%s] %s\n", tag, msg.toString());
    }

    @TruffleBoundary
    private static void print(PrintStream stream, String tag, SourceSection sourceSection, Object msg) {
        stream.printf("[%s] %s: %s\n", tag, getSourceSectionId(sourceSection).toString(), msg.toString());
    }

    public static void info(Object msg) {
        print(out, "i", msg);
    }

    public static void info(SourceSection sourceSection, Object msg) {
        print(out, "i", sourceSection, msg);
    }

    public static void info(int iid, Object msg) {
        info(SourceMapping.getSourceSectionForIID(iid), msg);
    }

    public static void debug(SourceSection sourceSection, Object msg) {
        if (GlobalConfiguration.DEBUG) {
            print(out, "d", sourceSection, msg);
        }
    }

    public static void debug(int iid, Object msg) {
        debug(SourceMapping.getSourceSectionForIID(iid), msg);
    }

    public static void debug(Object msg) {
        if (GlobalConfiguration.DEBUG) {
            print(out, "d", msg);
        }
    }

    public static void warning(SourceSection sourceSection, Object msg) {
        print(out, "w", sourceSection, msg);
    }

    public static void warning(int iid, Object msg) {
        warning(SourceMapping.getSourceSectionForIID(iid), msg);
    }

    public static void warning(Object msg) {
        print(out, "w", msg);
    }

    public static void error(SourceSection sourceSection, Object msg) {
        print(err, "e", sourceSection, msg);
    }

    public static void error(int iid, Object msg) {
        error(SourceMapping.getSourceSectionForIID(iid), msg);
    }

    public static void error(Object msg) {
        print(err, "e", msg);
    }

    @TruffleBoundary
    public static String getRelative(String path) {
        if (path.startsWith("/")) {
            String base = System.getProperty("user.dir");
            return new File(base).toURI().relativize(new File(path).toURI()).getPath();
        } else {
            return path;
        }
    }

    @TruffleBoundary
    public static StringBuilder getSourceSectionId(SourceSection sourceSection) {
        StringBuilder b = new StringBuilder();
        String fileName = sourceSection.getSource().getName();
        boolean isInternal = isInternal(sourceSection.getSource());
        b.append("(");
        if (isInternal) {
            b.append("*");
        }
        if (GlobalConfiguration.LOG_ABSOLUTE_PATH)
            b.append(fileName);
        else
            b.append(getRelative(fileName));
        b.append(":").append(sourceSection.getStartLine()).append(":").append(sourceSection.getStartColumn()).append(":").append(sourceSection.getEndLine()).append(":").append(
                        sourceSection.getEndColumn() + 1);
        b.append(")");
        return b;
    }

    private static boolean isInternal(Source src) {
        return src.isInternal() || src.getPath() == null || src.getPath().equals("");
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
    public static void dumpException(GraalJSException e) {
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
