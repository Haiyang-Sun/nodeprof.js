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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import com.oracle.js.parser.Source;

import ch.usi.inf.nodeprof.jalangi.JalangiAnalysis;

public class JalangiTest {

    @Test
    public void testWithTemplate() throws IOException {
        Context context = Context.create("js");

        // TODO this walks to the NodeProf root to find emptyTemplate.js, is that how it's done?
        File dir = new File(new File(".").getAbsolutePath());
        do {
            dir = dir.getParentFile();
        } while (dir != null && dir.list() != null && !Arrays.asList(dir.list()).contains("mx.nodeprof"));
        assertNotNull(dir);
        assertTrue(dir.isDirectory());

        String templatePath = dir + "/src/ch.usi.inf.nodeprof/js/analysis/trivial/emptyTemplate.js";

        // minimal harness
        context.eval("js", "J$ = {};");

        // evaluate analysis template
        context.eval("js", Source.readFully(new FileReader(templatePath)));

        // retrieve properties (ie. the callbacks) defined in analysis object
        Value v = context.eval("js", "Object.getOwnPropertyNames(J$.analysis)");
        assertTrue(v.hasArrayElements());

        // test callbacks from template
        @SuppressWarnings("unchecked")
        List<String> callbacks = v.as(List.class);
        for (String cb : callbacks) {
            if (JalangiAnalysis.unimplementedCallbacks.contains(cb) || JalangiAnalysis.ignoredCallbacks.contains(cb)) {
                // nothing to test
                continue;
            }
            // for all other callbacks, check if they map to a tag
            assertNotNull("not in callback map: " + cb, JalangiAnalysis.callbackMap.get(cb));
            assertTrue("does not map to any tag: " + cb, JalangiAnalysis.callbackMap.get(cb).size() > 0);
        }
    }
}
