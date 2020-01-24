/*******************************************************************************
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
  function TestApply() {
    this.invokeFunPre = function (iid, f, base, args, isConstructor, isMethod) {
      funclog = f !== undefined ? f.name : 'undefined';
      baselog = typeof base === 'function' ? 'func: ' + base.name : String(base);
      console.log("invokeFunPre: %s / %s / %s / method: %s", funclog, baselog, J$.iidToLocation(iid), isMethod);
    };
  };
  sandbox.addAnalysis(new TestApply());
}
)(J$));
