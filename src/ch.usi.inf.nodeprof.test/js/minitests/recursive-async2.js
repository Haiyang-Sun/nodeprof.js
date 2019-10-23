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

async function fib(x) {
    console.log('in fib', x);
    process.nextTick(()=>{
        console.log('in fib', x);
    });
    if (x < 2) {
	      return x;
    } else {
        return await fib(x-1) + await fib(x-2);
    }
}

async function main() {
	  return await fib(4);
}

main().then(v => console.log("done! answer = " + v));

console.log('***************** event loop first tick ***********************');
