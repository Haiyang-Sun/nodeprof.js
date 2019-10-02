/*******************************************************************************
 * Copyright 2019 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
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
let obj = {
    x:'valueX', 
    y:'valueY',
};

for (let key in obj) {
}

let arr = [41,42,43];

for (let val of arr) {
}

const itemA = 42;
class Foo {
  constructor() {
    this.itemB = 'bar';
  }
  [Symbol.iterator]() {
    let moreToIterate = 2;
    return {
      next: () => {
        if (moreToIterate) {
          const ret = { value: moreToIterate == 2 ? itemA : this.itemB, done: false };
          moreToIterate--;
          return ret;
        } else {
          return { done: true }
        }
      }
    }
  }
}

let o = new Foo();

for (i of o) {
}
