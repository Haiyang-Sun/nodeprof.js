/*******************************************************************************
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
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

    var allFuncs = new Set();
    const funcNameFilter = new Set(['foo', 'Object.create']);

    this.functionEnter = function (iid, f, dis, args) {
      if (funcNameFilter.has(f.name))
        console.log("functionEnter: %s / %s / %d", f.name, J$.iidToLocation(iid), arguments.length);
      allFuncs.add(f.name);
    };

    this.functionExit = function (iid, returnVal, wrappedExceptionVal) {
      if(!wrappedExceptionVal) {
        console.log("functionExit: %s / %d", J$.iidToLocation(iid), arguments.length);
      }else {
        console.log('functionExit with exception "%s(%s)": %s / %d', wrappedExceptionVal, typeof(wrappedExceptionVal),  J$.iidToLocation(iid), arguments.length);
      }
    };

    this.builtinEnter = function (name, f, dis, args) {
      if (funcNameFilter.has(name))
        console.log("builtinEnter: %s / %d", name, arguments.length);
      allFuncs.add(name);
    };

    this.builtinExit = function (name, returnVal) {
      if (funcNameFilter.has(name))
        console.log("builtinExit: %s / %d", name, arguments.length);
      allFuncs.add(name);
    };

    this.endExecution = function () {
      console.log([...allFuncs].filter(x => funcNameFilter.has(x)).sort());
    };
  }
  sandbox.addAnalysis(new MyAnalysis(), {includes:"<builtin>,enterExit.js,exitException"});
})(J$);
