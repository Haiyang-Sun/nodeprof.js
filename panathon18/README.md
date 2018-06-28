# NodeProf on GraalVM -  Panathon 2018
Welcome to NodeProf/GraalVM track of Panathon 2018!

## Table of Contents

   * [NodeProf on GraalVM -  Panathon 2018](#nodeprof-on-graalvm----panathon-2018)
      * [Overview](#overview)
      * [Installation](#installation)
         * [Basic installation - Running a pre-built version of NodeProf on GraalVM (Linux and MacOS)](#basic-installation---running-a-pre-built-version-of-nodeprof-on-graalvm-linux-and-macos)
         * [Advanced installation - Building NodeProf and GraalVM from source (Linux only)](#advanced-installation---building-nodeprof-and-graalvm-from-source-linux-only)
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
TBA