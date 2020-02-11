/*******************************************************************************
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
//DO NOT INSTRUMENT

const path = require('path');

((function(sandbox){
  function TestEval() {
    this.cache = null;
    this.evalPre = function (iid, str) {
      console.log("pre "+str);
    }
    this.evalPost = function (iid, str) {
      console.log("post "+str);
    }
    this.evalFunctionPost = function(args, ret){
      console.log("new Function body "+args[0]);
      console.log("result function: "+ret.name);
      this.cache = ret;
    }
    this.invokeFun = function(iid, func, base, args, result) {
      console.log("invoking func: "+func.name);
      if (func.name == 'foo') {
        var match = J$.iidToLocation(iid).match(/eval(?:func)?[0-9\.]+js:([0-9]+):/);
        if (match) {
          var line = match[1];
          console.log("location inside eval on line: %d", line);
        } else {
          console.log("failed to find source line in: ", J$.iidToLocation(iid));
        }
      }
      if(this.cache == func) {
        console.log("at end of invocation at %s, result: %d", J$.iidToLocation(iid), result);
      }
    }

    this.newSource = function (source, code) {
      // XXX skip eval for now
      if (source.name.startsWith('eval'))
        return;
      // mangle absolute path name for test output
      const name = path.parse(source.name).base;
      // count something in source contents to make sure it's there
      const re = /foo|var/g;
      console.log("newSource: %s / %d", name, arguments.length);
      console.log("newSource matches: %d", (code.match(re) || []).length);
    }
  };
  sandbox.addAnalysis(new TestEval(), {includes: 'eval.js,eval2.js,evalfunc.js'});
}
)(J$));
