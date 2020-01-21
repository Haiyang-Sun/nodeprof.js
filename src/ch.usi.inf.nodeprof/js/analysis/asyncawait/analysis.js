/*******************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
    function MyAnalysis() {
        let promises = new Map();

        function getPromiseId(p) {
            if (!(p instanceof Promise))
                return p;
            if (!promises.has(p)) {
                promises.set(p, '<p'+promises.size+'>');
            }
            return promises.get(p);
        }

        this.functionEnter = function (iid, f, base, args) {
            console.log("functionEnter@ " + J$.iidToLocation(iid));
        };
        this.functionExit = function (iid, result, exception) {
            console.log("functionExit@ " + J$.iidToLocation(iid), result, exception);
        };
        this.asyncFunctionEnter = function(iid) {
            console.log('asyncEnter', J$.iidToLocation(iid));
        }
        this.asyncFunctionExit = function(iid, returnVal, wrappedException) {
            console.log('asyncExit', J$.iidToLocation(iid), returnVal, wrappedException);
        }
        this.awaitPre = function(iid, valAwaited) {
            console.log('awaitPre', J$.iidToLocation(iid), getPromiseId(valAwaited));
        }
        this.awaitPost = function(iid, valAwaited, result, rejected) {
            console.log('awaitPost', J$.iidToLocation(iid), getPromiseId(valAwaited), getPromiseId(result), rejected?"rejected":"resolved");
        }
        this.binary = function(iid, op, left, right, result) {
            console.log('binary', left, op, right, '=', result);
        }
        this.write = function(iid, name, val) {
            console.log('write', J$.iidToLocation(iid), name, val);
        }
        this._return = function(iid, val) {
            console.log('return', J$.iidToLocation(iid), val);
        }
        this.conditional = function(iid, cond) {
            console.log('conditional', J$.iidToLocation(iid), cond);
        }
    }

    sandbox.analysis = new MyAnalysis();
})(J$);
