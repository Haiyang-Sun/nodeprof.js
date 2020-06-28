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
    function MyAnalysis() {
        const assert = require('assert');
        const stmts = new Set();
        let declareIIDs = [];
        const analysis = this;

        function filter(iid, type) {
            // ignore the IIFE call wrappers in expressions.js
            const src = J$.iidToCode(iid);
            if (src.includes('testWrapper')) {
                return true;
            }

            // ignore Node.js module wrapper
            const loc = J$.iidToSourceObject(iid).loc;
            if (loc.start.line === 1 && loc.start.column === 1) {
                return true;
            }
            return false;
        }

        this.startExpression = function (iid, type) {
            if (filter(iid, type))
                return;
            console.log("expression starts:", J$.iidToLocation(iid), "type:", type);
        };

        this.endExpression = function (iid, type) {
            if (filter(iid, type))
                return;
            console.log("expression finishes:", J$.iidToLocation(iid), "type:", type);
        };

        this.endStatement = function (iid, type) {
            if (filter(iid, type))
                return;
            console.log("statement finishes:", J$.iidToLocation(iid), "stmt:", type);
            const line = J$.iidToSourceObject(iid).loc.end.line;
            if (typeof line !== "number" || stmts.has(line)) {
                throw Error("Analysis expects at most one statement per line");
            }
            stmts.add(line);
        };

        // we want declarePre/declare to wrap a function declaration
        this.declarePre = function (iid, name, type, kind) {
            declareIIDs.push(iid);
            if (kind !== 'FunctionDeclaration') {
                return;
            }
            console.log(`declarePre name=${name}, type=${type}`);
        };

        this.declare = function (iid, name, type, kind) {
            declareIIDs.pop();
            if (kind === 'FunctionDeclaration') {
                analysis.endStatement(iid, kind);
            }
        };

        this.literal = function (iid, val, hasGetterSetter, literalType, memberNames) {
            if (declareIIDs.length > 0) {
                assert(iid === declareIIDs[declareIIDs.length - 1]);
                console.log(`literal: literalType=${literalType}`);
            }
        };
        this.literal.types = ["FunctionLiteral"];

        // empty callbacks used to materialize nodes in the Graal.js AST
        this.binaryPre = function() {};
        this.unaryPre = function() {};
        this.write = function(iid, name) {
            console.log("write", J$.iidToLocation(iid), name);
        };

        this.endExecution = function () {
          console.log("Statements in lines:", "[ " + [...stmts].sort().join(", ") + " ]");
        };
    }

    sandbox.analysis = new MyAnalysis();
})(J$);
