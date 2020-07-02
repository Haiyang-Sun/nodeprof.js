## API Differences compared to Jalangi

NodeProf supports most of the callbacks of Jalangi and can run existing Jalangi analysis using these callbacks without modification.
NodeProf also supports several features not supported by Jalangi. Details can be found in the following sections.

### Extra features supported in NodeProf

#### Callbacks

_evalPre_ and _evalPost_ for the 'eval' operation in JavaScript.

```
/**
 *  str is the code to be executed by eval
 */
this.evalPre = function (iid, str) {
}
this.evalPost = function (iid, str) {
}
```

_evalFunctionPre_ and _evalFunctionPost_ callbacks for function defined using the ```new Function("XXX")```:

```
this.evalFunctionPre= function(iid, func, receiver, args){
}
/**
 * ret will be the function object created
 */
this.evalFunctionPost= function(iid, func, receiver, args, ret){
}
```

An extra pair of callbacks for entering built-in functions:

```
/**
 * for built-in functions, their `name` is passed instead of an `iid`.
 */
this.builtinEnter = function (name, f, dis, args) {
};
this.builtinExit = function (name, returnVal) {
	return {returnVal: returnVal};
};
```

A callback similar to `instrumentCodePre`, but limited to observing code as it is encountered by NodeProf:

```
this.newSource = function (name, source) {
};
```

Examples can be found:
[1](https://github.com/Haiyang-Sun/nodeprof.js/blob/master/src/ch.usi.inf.nodeprof/js/analysis/extra-features/extra.js), [2](https://github.com/Haiyang-Sun/nodeprof.js/blob/master/src/ch.usi.inf.nodeprof/js/analysis/builtin-feature/analysis.js)

#### Expressions and Statements (endExpression replacement)
NodeProf adds the following callbacks in favor of `endExpression`:

```
this.startExpression = function (iid, type) {
};
this.endExpression = function (iid, type) {
};
this.startStatement = function (iid, type) {
};
this.endStatement = function (iid, type) {
};
```

Note that the callback behavior may depend on Graal.js internals and NodeProf 
cannot guarantee that type values will remain stable over time. Analyses
relying on these callbacks should be defensive and check that their behavior
and `type` values are consistent with expectations.


#### Source selection (selective instrumentation)

NodeProf supports selecting source files for instrumentation if their name is matched by a string
in an inclusion (or exclusion) list provided when registering the analysis object:

```
sandbox.addAnalysis(new MyAnalysis(), {excludes: 'badSource.js'});
```

Details can be found in the [tutorial](Tutorial.md).

### Jalangi features not yet supported

#### Changing the return value:
Jalangi can change the return value of an event by returning a specific object, e.g., {result: XXX} in the callback. This is not yet supported in NodeProf.


#### Callbacks to be added:

- ``` this.forinObject = function (iid, val)  ```

- ``` this.declare = function (iid, name, val, isArgument, argumentIndex, isCatchParam) ```

- ``` this._return = function (iid, val) ```

- ``` this._throw = function (iid, val)  ```

- ``` this._with = function (iid, val) ```

- ``` this.onReady = function (cb) ```

####  Discarded callbacks due to the difference in instrumentation:

- ``` this.scriptEnter = function (iid, instrumentedFileName, originalFileName) ```

- ``` this.scriptExit = function (iid, wrappedExceptionVal) ```

- ``` this.instrumentCodePre ```

- ``` this.instrumentCode ```

- ``` this.runInstrumentedFunctionBody ```
