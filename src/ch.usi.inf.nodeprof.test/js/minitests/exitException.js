/*******************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

function foo(){
  throw "this is an exception";
};

function bar(){
  throw new Error("this is an error");
};

function baz(){
  throw undefined;
};

try {
  foo();
}catch(e){
  console.log("exception: "+e);
}

try {
  bar();
}catch(e){
  console.log("exception: "+e);
}

try {
  baz();
}catch(e){
  console.log("exception: "+e);
}

(function() {
 try {
   throw Error("with finally");
   return 'nope';
 } catch(e) {
   return 42;
 } finally {
   return 43;
 }
 return 'nope';
})();
