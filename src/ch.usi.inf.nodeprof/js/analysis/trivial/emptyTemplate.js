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
         *
         * @param fakeHasGetterSetter is a placeholder to be consistent with Jalangi's API. 
         * The value provided in the callback is always undefined while
         * the actual value should be computed lazily via J$.adapter.hasGetterSetter(code)
         *
         * @param literalType is a new argument provided by NodeProf showing the type of literal
         *
         **/
        this.literal = function (iid, val, /* hasGetterSetter should be computed lazily */ fakeHasGetterSetter, literalType) {
            return {result: val};
        };
        // optional literal type filter: by specifying the types in an array, only given types of literals will be instrumented
        this.literal.types = ["ObjectLiteral", "ArrayLiteral", "FunctionLiteral", "NumericLiteral", "BooleanLiteral", "StringLiteral",
            "NullLiteral", "UndefinedLiteral", "RegExpLiteral"];

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
        this.builtinExit = function (name, f, dis, args, returnVal, exceptionVal) {
            return {returnVal: returnVal};
        };

        /**
         * These callbacks are called before and after a binary operation.
         **/
        this.binaryPre = function (iid, op, left, right) {
            return {op: op, left: left, right: right, skip: false};
        };
        this.binary = function (iid, op, left, right, result) {
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
         * The callbacks are called before and after an expression
         * @param iid {integer} source code location id 
         * @param type {string} type of the expression, TODO: use some standard type names, e.g., ESTree
         * @param result {} the execution result of the expression
         **/
        this.startExpression = function (iid, type) {
        };

        this.endExpression = function (iid, type, result) {
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
            this.evalPost = function (iid, str, ret) {
            };

            /**
             *  These callabcks are called before and after body of functions defined with the Function constructor are executed.
             **/
            this.evalFunctionPre = function(args) {
            };
            this.evalFunctionPost = function(args, ret, exceptionVal) {
            };

            /**
             * This callback is called when new source code is encountered during instrumentation.
             *
             * @ param {object} source - object describing the source. contains {string} name and {boolean} internal properties.
             * @ param {string} code - the source code text.
             **/
            this.newSource = function(source, code) {
            };

            /**
             *  Declaration of a symbol, type can be `'const', 'let', 'var'`, kind is `'FunctionDeclaration'` or `undefined`.
             *  Jalangi version: this.declare = function (iid, name, val, isArgument, argumentIndex, isCatchParam) {
             **/
            this.declarePre = function (iid, name, type, kind) {
            };
            this.declare = function (iid, name, type, kind) {
            };

            /**
             * Callbacks triggered before and after an expression.
             * Note that callback behavior may depend on Graal.js internals and NodeProf cannot guarantee that type values will
             * remain stable over time.
             *
             * @param iid {integer} source code location id 
             * @param type {string} Graal.js internal AST type of the expression
             **/
            this.startExpression = function (iid, type) {
            };
            this.endExpression = function (iid, type) {
            };

            /**
             * Callbacks triggered before and after a statement.
             * Note that callback behavior may depend on Graal.js internals and NodeProf cannot guarantee that type values will
             * remain stable over time.
             *
             * @param iid {integer} source code location id 
             * @param type {string} Graal.js internal AST type of the stamenent
             **/
            this.startStatement = function (iid, type) {
            };
            this.endStatement = function (iid, type) {
            };

            /**
             *  forin or forof support
             *  the object being iterated can be known by checking the last expression's result (via endExpression)
             **/
            this.forObject = function (iid, isForIn) {
            }

            /**
             * This callback is called before a value is returned from a function using the <tt>return</tt> keyword.
             *
             * This does NOT mean the function is being exited. Functions can return 0, 1, or more times.
             * For example:
             * - <tt>void</tt> functions return 0 times
             * - functions that use the <tt>return</tt> keyword regularly return 1 time
             * - functions that return in both parts of a try/finally block can return 2 times
             *
             * To see when a function ACTUALLY exits, see the <tt>functionExit</tt> callback.
             *
             * @param {number} iid - Static unique instruction identifier of this callback
             * @param {*} val - Value to be returned
             */
            this._return = function (iid, val) {
            };

            this.asyncFunctionEnter = function (iid) {
            }
            this.asyncFunctionExit = function (iid, result, exceptionVal) {
            }
            this.awaitPre = function (iid, promiseOrValAwaited) {
            }
            this.awaitPost = function (iid, promiseOrValAwaited, valResolveOrRejected, isPromiseRejected) {
            }

        }

        if(false) {
            // replaced with forObject including support for forin and forof
            this.forinObject = function (iid, val) {
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
