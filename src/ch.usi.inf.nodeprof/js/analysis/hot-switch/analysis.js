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
(function (sandbox) {
    const assert = require("assert");

    function argsToString(args) {
        console.log(args);
    }

    var flag = false;

    function MyAnalysis() {
        this.invokeFunPre = function (iid, f, base, args, isConstructor, isMethod, functionIid, functionSid) {
            console.log("invokeFunPre @ " + typeof(base) + " " + typeof(f) + " " + J$.iidToLocation(iid));
            argsToString(args);
        };

        this.invokeFun = function (iid, f, base, args, result, isConstructor, isMethod, functionIid, functionSid) {
            console.log("invokeFun @ " + typeof(base) + " " + typeof(f) + " " + J$.iidToLocation(iid));
            argsToString(args);
        };

        this._return = function(iid, val) {
            console.log('return', J$.iidToLocation(iid), 'val returns', val);

            // only one return in test thus _return executes once
            assert(flag === false, "_return must only execute once");
            flag = true;

            // disable Nodeprof until nextTick
            sandbox.disableAnalysis();
            process.nextTick(function(){
                sandbox.enableAnalysis();
            });

            // deactivate return upon first execution
            return { deactivate: true };
        }

        this.functionEnter = function (iid, f, dis, args) {
            console.log("functionEnter: %s / %s / %d", f.name, J$.iidToLocation(iid), arguments.length);

            // we expect a single enter for function foo
            if (f.name === 'foo') {
                return { deactivate: true };
            }
        };

        this.functionExit = function (iid, returnVal, wrappedExceptionVal) {
            // should never reach the exit foo and observe its return value
            assert(returnVal !== 'bar');
        };
    }

    sandbox.analysis = new MyAnalysis();
})(J$);
