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

import java.util.Arrays;
import java.util.Iterator;

import org.graalvm.options.OptionDescriptor;
import org.graalvm.options.OptionDescriptors;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

public class NodeProfOptionsDescriptors implements OptionDescriptors {

    @Override
    @TruffleBoundary
    public OptionDescriptor get(String optionName) {
        Iterator<OptionDescriptor> iter = iterator();
        while (iter.hasNext()) {
            OptionDescriptor od = iter.next();
            if (od.getName().equals(optionName)) {
                return od;
            }
        }
        return null;
    }

    @Override
    @TruffleBoundary
    public Iterator<OptionDescriptor> iterator() {
        return Arrays.asList(NodeProfCLI.ods).iterator();
    }
}
