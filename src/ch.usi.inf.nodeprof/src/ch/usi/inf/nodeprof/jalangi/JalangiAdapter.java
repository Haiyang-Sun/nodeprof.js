/*******************************************************************************
 * Copyright [2018] [Haiyang Sun, Università della Svizzera Italiana (USI)]
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

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

/**
 * Java class exposed to the Jalangi framework
 */
public class JalangiAdapter implements TruffleObject {
    private final NodeProfJalangi nodeprofJalangi;

    public JalangiAdapter(NodeProfJalangi nodeprofJalangi) {
        this.nodeprofJalangi = nodeprofJalangi;
    }

    public ForeignAccess getForeignAccess() {
        return JalangiAdapterMessageResolutionForeign.ACCESS;
    }

    public NodeProfJalangi getNodeProfJalangi() {
        return this.nodeprofJalangi;
    }
}
