/*******************************************************************************
 * Copyright [2018] [Haiyang Sun, Università della Svizzera Italiana (USI)]
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
(function (sandbox) {
    function MyAnalysis() {
        this.invokeFunPre = function (iid, f, base, args, isConstructor, isMethod, functionIid, functionSid) {
            return {f: f, base: base, args: args, skip: false};
        };
        this.invokeFun = function (iid, f, base, args, result, isConstructor, isMethod, functionIid, functionSid) {
            return {result: result};
        };
        this.literal = function (iid, val, hasGetterSetter) {
            return {result: val};
        };
        //not supported yet
        this.forinObject = function (iid, val) {
            return {result: val};
        };
        //not supported yet
        this.declare = function (iid, name, val, isArgument, argumentIndex, isCatchParam) {
            return {result: val};
        };
        this.getFieldPre = function (iid, base, offset, isComputed, isOpAssign, isMethodCall) {
            return {base: base, offset: offset, skip: false};
        };
        this.getField = function (iid, base, offset, val, isComputed, isOpAssign, isMethodCall) {
            return {result: val};
        };
        this.putFieldPre = function (iid, base, offset, val, isComputed, isOpAssign) {
            return {base: base, offset: offset, val: val, skip: false};
        };
        this.putField = function (iid, base, offset, val, isComputed, isOpAssign) {
            return {result: val};
        };
        this.read = function (iid, name, val, isGlobal, isScriptLocal) {
            return {result: val};
        };
        this.write = function (iid, name, val, lhs, isGlobal, isScriptLocal) {
            return {result: val};
        };
        //not supported yet
        this._return = function (iid, val) {
            return {result: val};
        };
        //not supported yet
        this._throw = function (iid, val) {
            return {result: val};
        };
        //not supported yet
        this._with = function (iid, val) {
            return {result: val};
        };
        this.functionEnter = function (iid, f, dis, args) {
        };
        this.functionExit = function (iid, returnVal, wrappedExceptionVal) {
            return {returnVal: returnVal, wrappedExceptionVal: wrappedExceptionVal, isBacktrack: false};
        };
        //not supported yet
        this.scriptEnter = function (iid, instrumentedFileName, originalFileName) {
        };

        //not supported yet
        this.scriptExit = function (iid, wrappedExceptionVal) {
            return {wrappedExceptionVal: wrappedExceptionVal, isBacktrack: false};
        };

        this.binaryPre = function (iid, op, left, right, isOpAssign, isSwitchCaseComparison, isComputed) {
            return {op: op, left: left, right: right, skip: false};
        };

        this.binary = function (iid, op, left, right, result, isOpAssign, isSwitchCaseComparison, isComputed) {
            return {result: result};
        };

        this.unaryPre = function (iid, op, left) {
            return {op: op, left: left, skip: false};
        };

        this.unary = function (iid, op, left, result) {
            return {result: result};
        };

        this.conditional = function (iid, result) {
            return {result: result};
        };

        //not supported yet
        this.instrumentCodePre = function (iid, code, isDirect) {
            return {code: code, skip: false};
        };

        //not supported yet
        this.instrumentCode = function (iid, newCode, newAst, isDirect) {
            return {result: newCode};
        };

        //not supported yet
        this.endExpression = function (iid) {
        };

        this.endExecution = function () {
        };

        //not supported yet
        this.runInstrumentedFunctionBody = function (iid, f, functionIid, functionSid) {
            return false;
        };

        //not supported yet
        this.onReady = function (cb) {
            cb();
        };
    }

    sandbox.analysis = new MyAnalysis();
})(J$);
