# NodeProf on GraalVM -  Panathon 2018
Welcome to the NodeProf/GraalVM track of Panathon 2018!

## Table of Contents

   * [NodeProf on GraalVM -  Panathon 2018](#nodeprof-on-graalvm----panathon-2018)
      * [Overview](#overview)
      * [Installation](#installation)
         * [Basic installation - Running a pre-built version of NodeProf on GraalVM (Linux and MacOS)](#basic-installation---running-a-pre-built-version-of-nodeprof-on-graalvm-linux-and-macos)
         * [Advanced installation - Building NodeProf and GraalVM from source (Linux and MacOS)](#advanced-installation---building-nodeprof-and-graalvm-from-source-linux-and-macos)
      * [Implementing a NodeProf analysis](#implementing-a-nodeprof-analysis)
         * [Running the dynamic call graph analysis](#running-the-dynamic-call-graph-analysis)
         * [Viewing and interpreting the results](#viewing-and-interpreting-the-results)
      * [Advanced topics](#advanced-topics)
         * [NodeProf analysis hooks](#nodeprof-analysis-hooks)
         * [Selective instrumentation](#selective-instrumentation)

## Overview
In a nutshell [NodeProf](https://github.com/Haiyang-Sun/nodeprof.js) is an instrumentation and profiling framework for JavaScript, running on [GraalVM](https://www.graalvm.org/). NodeProf implements the [Jalangi](https://github.com/Samsung/jalangi2) API and enables the execution of Jalangi-style dynamic analyses on top of GraalVM.

## Installation

### Basic installation - Running a pre-built version of NodeProf on GraalVM (Linux and MacOS)
You can directly run NodeProf with a pre-built JAR file and the latest GraalVM.
1. Clone the `panathon` branch of the NodeProf [repository](https://github.com/Haiyang-Sun/nodeprof.js):
   `git clone -b panathon https://github.com/Haiyang-Sun/nodeprof.js.git`
2. Download the latest GraalVM [here](http://www.oracle.com/technetwork/oracle-labs/program-languages/downloads/index.html)
3. Decompress the GraalVM to a location of your choice (e.g. `/usr/local/graalvm-ee-<version>`) and export the path to Graal:
	* For Linux: `export GRAAL_HOME=/usr/local/graalvm-ee-<version>`
	* For MacOS: `export GRAAL_HOME=/usr/local/graalvm-ee-<version>/Contents/Home`
4. You can find a pre-built NodeProf JAR file here: `nodeprof.js/snapshot/nodeprof.jar`
5. Put the NodeProf JAR file in a location of your choice (e.g. `/usr/local/graalvm-ee-<version>/jre/tools/nodeprof/nodeprof.jar`) and 
	```
	export NODEPROF_HOME=/usr/local/graalvm-ee-<version>/jre/tools/nodeprof
	```
6. Copy the `nodeprof.js/src/ch.usi.inf.nodeprof/js/jalangi.js` file to the `NODEPROF_HOME` folder.
7. You can now run an analysis using the following command:
    ```
    $GRAAL_HOME/bin/node --jvm --jvm.Dtruffle.class.path.append=$NODEPROF_HOME/nodeprof.jar --nodeprof $NODEPROF_HOME/jalangi.js [--analysis analysisFile]* test-file
    ```

Congratulations, you're all set to run NodeProf. You can now try to implement [your first analysis](#implementing-a-nodeprof-analysis).

### Advanced installation - Building NodeProf and GraalVM from source (Linux and MacOS)
If you plan to customise NodeProf or the GraalVM, this is the option you need.
1. Ensure that you have JDK >= 8 installed and set in your **JAVA_HOME**. 
    You can find a GraalVM compatible JDK [here](http://www.oracle.com/technetwork/oracle-labs/program-languages/downloads/index.html).
2. Get the  **mx**  build tool and put it in your  **PATH**:
	```
	git clone https://github.com/graalvm/mx.git
	export PATH=$PATH:<path_to_mx_folder>
	```
3. Set up a NodeProf workspace and build NodeProf and its dependencies:
	```
	mkdir nodeprof-graalvm
	cd nodeprof-graalvm
	git clone -b panathon https://github.com/Haiyang-Sun/nodeprof.js.git
	cd nodeprof.js
	mx sforceimports
	mx build
	```
4. Run tests:
	```
	mx test
	```
5. Run an analysis with **mx**:
	```
	mx jalangi [--analysis pathToAnalsis]* pathToMainProgram [arg]*
	```
Congratulations, you're all set to run and extend NodeProf. You can now try to implement [your first analysis](#implementing-a-nodeprof-analysis).

## Implementing a NodeProf analysis
Let's assume that we are interested in an analysis that builds dynamic call graphs. The following callbacks implement a minimal NodeProf analysis that logs a context-insensitive dynamic call graph in [dot](http://www.graphviz.org/documentation/) format:
```
let cg = new Map();
let lastCallsite = undefined;

function addCallee(callsite, callee) {
  callsite = callsite === undefined ? "GraalVM" : callsite;
  cg.has(callsite) ? cg.get(callsite).add(callee) : cg.set(callsite, new Set([callee])); 
}

this.invokeFunPre = function(iid, f, base, args, isConstructor, isMethod) {
  iidToLocation.set(iid, J$.iidToLocation(iid));
  lastCallsite = iid;
};

this.invokeFun = function (iid, f, base, args, result, isConstructor, isMethod) {
  lastCallsite = undefined;
}

this.functionEnter = function (iid, func, receiver, args) {
  iidToLocation.set(iid, J$.iidToLocation(iid));
  addCallEdge(lastCallsite, iid);
};
    
this.endExecution = function() {
  //Output dynamic call graph in Dot format
  console.log("digraph {");
  cg.forEach(function (value, key) {
    value.forEach(function (callee) {
      console.log(`  ${key} -> ${callee};`);
    })
  });
  console.log("}");
};
```
The complete, executable dynamic call graph analysis can be found in `nodeprof.js/panathon18/sample-analyses/dynCallGraph.js`

### Running the dynamic call graph analysis
We will now run the dynamic call graph analysis on the `nodeprof.js/panathon18/tests/callMeMaybe.js` file.

If you followed the [basic installation](#basic-installation---running-a-pre-built-version-of-nodeprof-on-graalvm-linux-and-macos) instructions, you can run the dynamic call graph analysis with the following command:
```
$GRAAL_HOME/bin/node --jvm --jvm.Dtruffle.class.path.append=$NODEPROF_HOME/nodeprof.jar --nodeprof $NODEPROF_HOME/jalangi.js --analysis <path_to>/dynCallGraph.js <path_to>/callMeMaybe.js
```
If you followed the [advanced installation](#advanced-installation---building-nodeprof-and-graalvm-from-source-linux-only) instructions, you can run the dynamic call graph analysis with the following command:
```
mx jalangi --analysis <path_to>/dynCallGraph.js <path_to>/callMeMaybe.js
```

### Viewing and interpreting the results
The dynamic call graph analysis should produce an output in [dot](http://www.graphviz.org/documentation/) format, similar to the following:
```
// 1: (<path_to>/callMeMaybe.js:1:1:26:4)
// 2: (<path_to>/callMeMaybe.js:1:2:26:2)
// 3: (<path_to>/callMeMaybe.js:18:9:18:16)
// 4: (<path_to>/callMeMaybe.js:13:1:16:2)
// 5: (<path_to>/callMeMaybe.js:20:3:20:8)
// 6: (<path_to>/callMeMaybe.js:9:1:11:2)
// 7: (<path_to>/callMeMaybe.js:10:3:10:11)
// 8: (<path_to>/callMeMaybe.js:1:63:3:2)
// 9: (<path_to>/callMeMaybe.js:21:3:21:8)
// 10: (<path_to>/callMeMaybe.js:5:1:7:2)
digraph {
  GraalVM -> 1;
  GraalVM -> 2;
  3 -> 4;
  5 -> 6;
  7 -> 8;
  7 -> 10;
  9 -> 8;
  9 -> 10;
}
```
Where the header comments map each id to their source code location and lines containing an `->` map callers on the left-hand side to callees on the right-hand side. If saved to a file named `dyn.dot`, a visual representation of the call graph can be generated with the following command (assuming that `dot` is installed on your machine):
```
dot -Tpdf -o dyn.pdf dyn.dot
```
## Advanced topics

### NodeProf analysis hooks

An empty analysis template with all the available callbacks can be found here: `nodeprof.js/src/ch.usi.inf.nodeprof/js/analysis/trivial/emptyTemplate.js`

###  Selective instrumentation

One way to optimize analysis performance is to narrow the analysis scope by selective instrumentation. Selective instrumentation can be applied in different granularities. We have prepared different examples for you to familiarize yourself with selective instrumentation in NodeProf. To setup the benchmarks, you can run:
```
cd panathon18/scripts
./install-dependencies.sh
```

#### Coarse-grained selective instrumentation

##### `scope` command option

NodeProf allows to set a coarse-grained instrumentation scope for your analysis by passing a command line option. This option allows turning on analysis even on internal libraries in node
```
mx jalangi --scope=[app|module|all]
```
* `app`: only instruments the application code (without any code in the `node_modules` folder or internal libraries);
* `module`: instruments the application code plus the node modules. By default, this option is enabled.
* `all`: instruments all the code including internal library, node modules and application code.

###### Example 1
Run [panathon18/sample-analyses/TypedArray.js](./sample-analyses/TypedArray.js) (originally developed for [Jalangi](https://github.com/Samsung/jalangi2)) that tracks non-numeric stores into numeric arrays. This test is run on the [mock2easy](https://www.npmjs.com/package/mock2easy) NPM as a benchmark. First, we set `--scope=module` to instrument both application and module code.
```
mx jalangi --analysis panathon18/sample-analyses/Utils.js --analysis panathon18/sample-analyses/TypedArray.js panathon18/tests/npms/mock2easy/app.js --scope=module
```
Now, set `--scope` to `app` and see the difference.
```
mx jalangi --analysis panathon18/sample-analyses/Utils.js --analysis panathon18/sample-analyses/TypedArray.js panathon18/tests/npms/mock2easy/app.js --scope=app
```
When you set `--scope` to `all`, you also get results for internal libraries, while `app` scope does not report any results.

##### File-based exclusion

NodeProf also has the following file-based filtering options that make selective instrumentation slightly more flexible:
 * Specify an exclusion list with `mx jalangi --excl keyword1,keyword2 to avoid instrumenting any source code file whose full path contains one of these keywords.
 * Add `// DO NOT INSTRUMENT` to the beginning of the source code file
 
###### Example 2
Exclude all modules from `express` npm and run TypedArray.js analysis:

```
mx jalangi --analysis panathon18/sample-analyses/Utils.js --analysis panathon18/sample-analyses/TypedArray.js panathon18/tests/npms/mock2easy/app.js --excl express

``` 
You should no longer see the results for any of the files under `node_modules/express` dircetory.

#### Fine-grained instrumentation
While the coarse-grained selective instrumentation is handy for testing and debugging, a more fine-grained instrumentation that can be integrated to analyses is needed for complex performance optimizations. For instance, it would be useful to enable certain callbacks in an analysis as long as a given predicate holds. Furthermore, it would be useful to instrument selected lines in source code or AST nodes on-the-fly. The good news is that we already have a callback function that allows instrumenting selected analysis callbacks. But, the more exciting news is that we would like you to implement the source-line/AST node instrumentation as part of this hackathon.


##### Source code filtering using JavaScript API
Instrumentation can be controlled in different ways from within JavaScript code. This is described in the [NodeProf tutorial](../Tutorial.md#source-filters-and-selective-instrumentation-in-nodeprof).

Note that some filters extend the global options provided by `--scope` and `--excl`, while others replace them. 
You can use `mx jalangi --debug ...` to see information related to the source filter being applied. 

###### Example 3
 
Run [panathon18/sample-analyses/TraceAll.js](./sample-analyses/TraceAll.js), a Jalangi analysis to log analysis callbacks that get executed while running mock2easy:
 
```
mx jalangi --analysis panathon18/sample-analyses/SMemory.js --analysis panathon18/sample-analyses/TraceAll.js panathon18/tests/npms/mock2easy/app.js

``` 

and use the following predicate to only log the literals in sanitizing and encoding/decoding routines.
 
```

//Predicate for selective instrumentation
function filter(source){
  if(source.internal)
    return false;
  //Instruments and logs only literals in modules of interest
  if (source.name.includes('escape') || source.name.includes('sanitize') || source.name.includes('decode')|| source.name.includes('encode'))
    return ['literal'];
  return false;
}
```

##### Extending source code filters
To modify, extend or create your own source code filter in (the Java side of) NodeProf, refer to the code of the list-based and predicate-based filters that are already available:

* [AnalysisFilterSourceList.java](../src/ch.usi.inf.nodeprof/src/ch/usi/inf/nodeprof/analysis/AnalysisFilterSourceList.java)
* [AnalysisFilterJS.java](../src/ch.usi.inf.nodeprof/src/ch/usi/inf/nodeprof/analysis/AnalysisFilterJS.java)


## Panathon challenge
If you have followed the previous instructions, now you should be ready to extend selective instrumentation in NodeProf [source-code](../src) by yourself. If you already have an idea that you would like to experiment with, it would be amazing. Otherwise, you might want to explore the following scenario.

Use [JS Delta](https://github.com/wala/jsdelta),  a delta debugger for debugging JavaScript programs, as a slicing tool to reduce a benchmark given a predicate (note that JS Delta predicates are different from source code filtering predicates in NodeProf). Next, extend the source code filtering mechanism of NodeProf to only instrument the reduced program. We have already provided a simple JS Delta [predicate](./scripts/jsdelta/predicates/navier-strokes-predicate.js) for [panathon18/tests/jsdelta/navier-stokes.js](./tests/jsdelta/navier-stokes.js) from  the Octane benchmark. You can find the reduced file in [panathon18/tests/jsdelta/navier-stokes_smallest.js](./tests/jsdelta/navier-stokes_smallest.js) and the AST node locations in [panathon18/tests/jsdelta/navier-stokes_smallest_loc.js](./tests/jsdelta/navier-stokes_smallest_loc.js).

To run JS Delta by yourself, see the instructions [here](https://github.com/wala/jsdelta) to clone the repository and set it up. Next, from the `jsdelta` root directory, run the following command to apply patches that allow logging source code locations of AST nodes in the reduced JavaScript file: 

```
<path-to>/panathon18/scripts/jsdelta/applyPatches.sh
```
Finally, run JS Delta on `navier-stokes.js` benchmark:

```
node delta.js --cmd <path-to>/panathon18/scripts/jsdelta/predicates/navier-strokes-predicate.js --msg stop --out /tmp/navier-stokes_smallest.js <path-to>/panathon18/tests/jsdelta/navier-stokes.js
```
and the AST node locations for `navier-stokes_smallest.js` can be found in `/tmp/navier-stokes_smallest_loc.json`
