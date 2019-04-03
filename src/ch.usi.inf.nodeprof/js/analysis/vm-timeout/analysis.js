/*******************************************************************************
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
// DO NOT INSTRUMENT
(function (sandbox) {
  function MyAnalysis() {
    const funcNameFilter = new Set(['foo', 'insideVM'])
    this.functionEnter = function (iid, f, dis, args) {
      if (!funcNameFilter.has(f.name))
        return
      console.log("functionEnter: %s / %s / %d", f.name, J$.iidToLocation(iid), arguments.length);
      if (f.name === 'insideVM') {
        // run until runInContext times out
        (function () { let i = 0; while (true) i++; })();
      }
    };
  }
  sandbox.addAnalysis(new MyAnalysis());
})(J$);
