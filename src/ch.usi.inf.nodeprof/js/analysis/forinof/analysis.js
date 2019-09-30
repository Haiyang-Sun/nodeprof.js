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
    const assert = require('assert');
    function MyAnalysis() {

        // last expression before for-in/for-of is the iteration object
        let lastExprResult;

        // control flow tracking
        let cfRoots = new Map;
        let cfBlockStack = [];
        // ignore control flow other that for-in/for-of
        let ignoredIIDs = new Set;

        // keep track of user-defined iterators
        let iteratorFuncs = new Set;
        let iteratorIIDs = new Set;
        let nextFuncs = new Set;
        let rwTrackStack = [];

        function logLoc(cbName, iid, ...extra) {
            if (ignoredIIDs.has(iid)) {
                return;
            }
            console.log('%s@%s', cbName, J$.iidToLocation(iid), ...extra);
        }
        function storeIterator(obj) {
            let proto = obj;
            while (proto != null) {
                let iterator = proto[Symbol.iterator];
                if (iterator) {
                    iteratorFuncs.add(iterator);
                }
                proto = Object.getPrototypeOf(proto);
            }
        }

        this.cfRootEnter = function (iid, type) {
            if (type === J$.cf.IF) {
                ignoredIIDs.add(iid);
            }
            logLoc('cfRootEnter', iid, type);
            if (type === J$.cf.FOR_OF || type === J$.cf.FOR_IN) {
                const o = lastExprResult;
                console.log('iteration obj:', nextFuncs.has(o.next) ? '<iter w/ next()>' : o);
            }
            cfRoots.set(iid, type);
        }
        this.cfRootExit = function (iid, type) {
            logLoc('cfRootExit', iid, type);
        }
        this.cfBlockEnter = function(iid, iidParent) {
            cfBlockStack.push(iid);
        }
        this.cfBlockExit = function(iid, iidParent) {
            assert(cfBlockStack.pop() === iid);
            if (!ignoredIIDs.has(iidParent)) {
                console.log('cfRoot %s @ %s', cfRoots.get(iidParent), J$.iidToLocation(iidParent));
                console.log('  \\-cfBlock @ %s',  J$.iidToLocation(iid));
            }
        }
        this.read = function(iid, name, value) {
            if (rwTrackStack.length) {
                console.log('read@', J$.iidToLocation(iid), name); // only inside iter-next()
            }
        }
        this.write = function(iid, name, value) {
            if (rwTrackStack.length) {
                console.log('write@', J$.iidToLocation(iid), name); // only inside iter-next()
            }
        }
        this.functionEnter = function (iid, f, dis, args) {
            if (iteratorFuncs.has(f)) {
                console.log("functionEnter: %s / %s / %d", f.name, J$.iidToLocation(iid), arguments.length);
                iteratorIIDs.add(iid);
            }
            if (nextFuncs.has(f) || rwTrackStack.length) {
                rwTrackStack.push(iid); // stack length > 0 when inside next()
            }
        }
        this.functionExit = function (iid, returnVal) {
            if (iteratorIIDs.has(iid)) {
                nextFuncs.add(returnVal.next);
            }
            rwTrackStack.pop();
        }
        this.invokeFun = function (iid, f, base, args, result, isConstructor, isMethod) {
            if (typeof result === 'object') {
                storeIterator(result);
            }
        }
        this.endExpression = function (iid, type, result) {
            lastExprResult = result;
        }
    }

    sandbox.analysis = new MyAnalysis();
})(J$);
