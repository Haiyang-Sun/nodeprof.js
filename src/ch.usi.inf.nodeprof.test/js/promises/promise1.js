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

var p1 = new Promise(function(res, rej){
  res(1);
});

var p2 = Promise.resolve(1);

var p3 = Promise.reject(0);

var p4 = p1.then(v => { return v+1; });

var p5 = Promise.all([p1,p2]);

var p6 = Promise.race([p1,p2]);

p3.catch(err => {});


