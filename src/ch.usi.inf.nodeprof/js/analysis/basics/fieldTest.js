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
((function(sandbox){
  function FieldTest() {
    var assert = require("assert");
    function getLocation(sid, iid) {
      if (typeof Graal === 'object')
        // Truffle-Jalangi has unique IIDs
        return  J$.iidToLocation(iid);
      else
        // Jalangi on V8/Node needs sid
        return J$.iidToLocation(sid, iid);
    }
    this.getField = function(iid, base, offset, val, isComputed, isOpAssign, isMethodCall) {
      console.log("getField @ ", getLocation(J$.sid, iid), " base type:",typeof(base), " offset type:", typeof(offset), " val type:", typeof(val), isComputed, J$.iidToCode(iid));
    };
    this.putField = function (iid, base, offset, val, isComputed, isOpAssign) {
      console.log("putField @ ", getLocation(J$.sid, iid)," base type:",typeof(base), " offset type:", typeof(offset), " val type:", typeof(val), isComputed, J$.iidToCode(iid));
    };
  }
  sandbox.analysis = new FieldTest();
}
)(J$));
