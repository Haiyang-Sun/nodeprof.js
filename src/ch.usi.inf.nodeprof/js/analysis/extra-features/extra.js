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
((function(sandbox){
  function TestEval() {
    this.cache = null;
    this.evalPre = function (iid, str) {
      console.log("pre "+str);
    }
    this.evalPost = function (iid, str) {
      console.log("post "+str);
    }
    this.evalFunctionPost= function(iid, func, receiver, args, ret){
      console.log("new Function body "+args[0]);
      console.log("result function: "+ret);
      this.cache = func;
    }
    this.invokeFun = function(iid, func) {
      if(this.cache == func) {
        console.log("found invocation of new Function at "+J$.iidToLocation(iid)+" of func "+func);
      }
    }
  };
  sandbox.analysis = new TestEval();
}
)(J$));
