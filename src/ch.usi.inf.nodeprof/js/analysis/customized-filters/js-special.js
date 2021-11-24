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
 //DO NOT INSTRUMENT
((function(sandbox){

  var assert = require("assert");
  var internals = new Set();
  var internalCalls = new Set();
  var builtins = new Set();
  var mute = false;

  function NodeInternal() {
    const analysis = 'node-module';
    this.functionEnter = function (iid, f, dis, args) {
      if (f.name == '' || f.name == 'readPackage' || mute)
        return;
      // Do not log Node.js internals in test output
      // console.log("%s: functionEnter: %s / %s / %d", analysis, f.name, J$.iidToLocation(iid).replace(/:.*[0-9]/,''), arguments.length);
      internalCalls.add(J$.iidToSourceObject(iid).name);
    };
    this.endExecution = function () {
      mute = true;
      console.log(analysis + ': internal source', [...internals].sort());
      assert(internalCalls.size >= 3, 'missing calls to internal functions');
    };
  }

  // run NodeInternal analysis only with one test
  if (process.argv[process.argv.length - 1].endsWith('donotinstrument.js')) {
    sandbox.addAnalysis(new NodeInternal(), function filter(source) {
      if (source.internal && source.name.includes('module')) {
        if (source.name.endsWith('transform_source.js')) {
          // XXX this source shows up only sometimes, is this a callback bug? exclude for now
        } else {
          internals.add(source.name);
        }
        return true;
      }
    });
  }

  function BI() {
    const analysis = 'builtin';
    this.builtinEnter = function(builtinName, func, base, args){
      if(builtinName){
        builtins.add(builtinName);
      }
    }
    this.endExecution = function () {
      mute = true;
      console.log([...builtins].filter(x => x.includes('create')));
    };
  }
  sandbox.addAnalysis(new BI(), function filter(source) {
    if (source.name === '<builtin>') {
      return true;
    }
  });
}
)(J$));
