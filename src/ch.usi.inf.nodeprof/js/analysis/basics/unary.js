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
    function getLocation(sid, iid) {
      if (typeof Graal === 'object')
        return  J$.iidToLocation(iid);
      else
        return J$.iidToLocation(sid, iid);
    }
    this.unary = function (iid, op, val) {
      console.log("unary "+getLocation(J$.sid, iid)+" "+op+" "+typeof val);
      if(op == "delete"){
        console.log("delete key: "+val[1]);
      }
    };
  }
  sandbox.analysis = new FieldTest();
}
)(J$));
