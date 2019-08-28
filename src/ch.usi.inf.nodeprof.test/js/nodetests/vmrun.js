/*******************************************************************************
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

const assert = require('assert');
const vm = require('vm');

let isDone = false;
let sandbox = {
  done: function done() { isDone = true; }
};

vm.createContext(sandbox);
try {
  let res  = vm.runInContext(
      'var res; function insideVM(){ res = 42; }; insideVM(); done(); res',
      sandbox,
      { timeout: 1000 });
  console.log(res);
} catch (err) {
  console.log('runInContext failed (as expected):', err.message);
}

function foo() {}
foo();
// whether done() is called seems to depend on running w/ or w/o compilation,
// so we no longer check for it here
