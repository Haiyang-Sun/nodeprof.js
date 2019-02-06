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

// DO NOT INSTRUMENT
((function(sandbox){
  function TestBuiltin() {
    function getLocation(iid) {
      if (typeof Graal === 'object')
        return sandbox.iidToLocation(iid);
      else
        return sandbox.iidToLocation(sandbox.sid, iid);
    }
    this.builtinEnter = function(builtinName, func, base, args){
      if(builtinName && builtinName.indexOf("Promise") > -1){
        J$.nativeLog("builtin used "+builtinName);
      }
    }
    this.invokeFunPre = function(iid, func, base, args){
      J$.nativeLog("invoking "+getLocation(iid)+" "+func.name);
    }
  };
  sandbox.addAnalysis(new TestBuiltin(), {includes:"builtin"});
}
)(J$));
