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
var elems = [0, "0.0", "0.1", "a", undefined, null, Symbol("test"), true];
var vals = [0, Object.create(null), undefined, null, "abc", Symbol("t"), true];

var obj = {};

for(i in elems) {
  var elem = elems[i];
  var val = vals[i % vals.length];
  obj[elem] = val;
}
for(j in vals) {
  var elem = elems[j];
  console.log(obj[elem]);
}
