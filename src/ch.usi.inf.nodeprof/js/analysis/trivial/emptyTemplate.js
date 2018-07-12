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
// DO NOT INSTRUMENT
(function (sandbox) {
    function MyAnalysis() {

        /**
         * These callbacks are called before and after a function, method, or constructor invocation.
         **/
        this.invokeFunPre = function (iid, f, base, args, isConstructor, isMethod, functionIid, functionSid) {
            return {f: f, base: base, args: args, skip: false};
        };
        this.invokeFun = function (iid, f, base, args, result, isConstructor, isMethod, functionIid, functionSid) {
            return {result: result};
        };

        /**
         * This callback is called after the creation of a literal. A literal can be a function
         * literal, an object literal, an array literal, a number, a string, a boolean, a regular
         * expression, null, NaN, Infinity, or undefined.
         **/
        this.literal = function (iid, val, hasGetterSetter) {
            return {result: val};
        };

        /**
         * These callbacks are called before and after a property of an object is accessed.
         **/
        this.getFieldPre = function (iid, base, offset, isComputed, isOpAssign, isMethodCall) {
            return {base: base, offset: offset, skip: false};
        };
        this.getField = function (iid, base, offset, val, isComputed, isOpAssign, isMethodCall) {
            return {result: val};
        };

        /**
         * These callbacks are called before a property of an object is written
         **/
        this.putFieldPre = function (iid, base, offset, val, isComputed, isOpAssign) {
            return {base: base, offset: offset, val: val, skip: false};
        };
        this.putField = function (iid, base, offset, val, isComputed, isOpAssign) {
            return {result: val};
        };

        /**
         * These callbacks are called after a variable is read or written.
         **/
        this.read = function (iid, name, val, isGlobal, isScriptLocal) {
            return {result: val};
        };
        this.write = function (iid, name, val, lhs, isGlobal, isScriptLocal) {
            return {result: val};
        };

        /**
         * These callbacks are called before the execution of a function body starts and after it completes.
         **/
        this.functionEnter = function (iid, f, dis, args) {
        };
        this.functionExit = function (iid, returnVal, wrappedExceptionVal) {
            return {returnVal: returnVal, wrappedExceptionVal: wrappedExceptionVal, isBacktrack: false};
        };

        /**
         * These callbacks are called before the execution of a builtin function body starts and after it completes.
         **/
        this.builtinEnter = function (name, f, dis, args) {
        };
        this.builtinExit = function (name, returnVal) {
            return {returnVal: returnVal};
        };

        /**
         * These callbacks are called before and after a binary operation.
         **/
        this.binaryPre = function (iid, op, left, right, isOpAssign, isSwitchCaseComparison, isComputed) {
            return {op: op, left: left, right: right, skip: false};
        };
        this.binary = function (iid, op, left, right, result, isOpAssign, isSwitchCaseComparison, isComputed) {
            return {result: result};
        };

        /**
         * These callbacks are called before and after a unary operation.
         **/
        this.unaryPre = function (iid, op, left) {
            return {op: op, left: left, skip: false};
        };
        this.unary = function (iid, op, left, result) {
            return {result: result};
        };

        /**
         * This callback is called after a conditional expression has been evaluated
         **/
        this.conditional = function (iid, result) {
            return {result: result};
        };

        /**
         * This callback is called when an execution terminates in node.js.
         **/
        this.endExecution = function () {
        };

        //for callbacks that are new or different from Jalangi
        var extraFeatures = true;
        if(extraFeatures) {
            /**
             *  These callbacks are called before and after code is executed by eval.
             **/
            this.evalPre = function (iid, str) {
            };
            this.evalPost = function (iid, str) {
            };

            /**
             *  These callabcks are called before and after body of functions defined with the Function constructor are executed.
             **/
            this.evalFunctionPre = function(iid, f, base, args) {
            };
            this.evalFunctionPost = function(iid, f, base, args, ret) {
            };
        }

        var notSupported = false;

        if(notSupported) {
            //not supported yet
            this.forinObject = function (iid, val) {
            };

            //not supported yet
            this.declare = function (iid, name, val, isArgument, argumentIndex, isCatchParam) {
            };

            //not supported yet
            this._return = function (iid, val) {
            };

            //not supported yet
            this._throw = function (iid, val) {
            };

            //not supported yet
            this._with = function (iid, val) {
            };

            //not supported yet
            this.scriptEnter = function (iid, instrumentedFileName, originalFileName) {
            };

            //not supported yet
            this.scriptExit = function (iid, wrappedExceptionVal) {
            };
            //not supported yet
            this.endExpression = function (iid) {
            };

            //not supported yet
            this.runInstrumentedFunctionBody = function (iid, f, functionIid, functionSid) {
            };

            //not supported yet
            this.onReady = function (cb) {
                cb();
            };

            //not supported yet
            this.instrumentCodePre = function (iid, code, isDirect) {
            };

            //not supported yet
            this.instrumentCode = function (iid, newCode, newAst, isDirect) {
            };
        }
    }

    sandbox.analysis = new MyAnalysis();
})(J$);
