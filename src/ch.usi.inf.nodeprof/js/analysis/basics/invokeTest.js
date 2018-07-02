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
    function getLocation(sid, iid) {
        if (process.config.variables.graalvm)
            // Truffle-Jalangi has unique IIDs
            return  J$.iidToLocation(iid);
        else
            // Jalangi on V8/Node needs sid
            return J$.iidToLocation(sid, iid);
    }

    function argsToString(args) {
        console.log(args);
    }

    function MyAnalysis() {
        this.invokeFunPre = function (iid, f, base, args, isConstructor, isMethod, functionIid, functionSid) {
            console.log("invokeFunPre @ " + typeof(base) + " " + typeof(f) + " " + getLocation(J$.sid, iid));
            argsToString(args);
        };

        this.invokeFun = function (iid, f, base, args, result, isConstructor, isMethod, functionIid, functionSid) {
            console.log("invokeFun @ " + getLocation(J$.sid, iid));
        };
    }

    sandbox.analysis = new MyAnalysis();
})(J$);
