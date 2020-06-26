/*******************************************************************************
 * Copyright 2019 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
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

function Literal(){
  const astHelper = J$.getAstHelper();
  function skipModule(iid) {
    var locObj = J$.iidToSourceObject(iid);
    return (locObj.loc.start.line === 1 && locObj.loc.start.column === 1);
  }
  this.literal = function(iid, val, fakeHasGetterSetter, type){
    if (skipModule(iid)) return;
    let hasGetterSetter = false;
    let fields = undefined;
    if (type === 'ObjectLiteral') {
      ({ hasGetterSetter, fields } = astHelper.parseObjectLiteral(iid));
    }
    console.log(J$.iidToLocation(iid), typeof(val), hasGetterSetter, type, fields);
  }
}

J$.addAnalysis(new Literal());
