
## NodeProf Tutorial

### Installation

Get the __mx__ build tool and put it in your __PATH__:

```
git clone https://github.com/graalvm/mx.git
```

Get dependent projects and build:

```
$ mkdir workspace-nodeprof
$ cd workspace-nodeprof
$ git clone https://github.com/Haiyang-Sun/nodeprof.js.git
$ cd nodeprof.js
$ mx sforceimports
$ mx build
```

Run tests:
```
$ mx test-all
```

### Run with 'mx'

- ``` mx test-all ``` runs all test cases and compare the output with expected values

- ``` mx test-unit``` runs all unit tests

- ``` mx test-specific analysisName [benchmarkName] ``` will automatically search Jalangi analysis defined in ```src/ch.usi.inf.nodeprof/js/analysis/analysisName``` and load them. [click here for more details](https://github.com/Haiyang-Sun/nodeprof.js/tree/master/src/ch.usi.inf.nodeprof/js/analysis) and compare the output of the run with expected output.
If benchmarkName is specified, only test files inside nodeprof/src/ch.usi.inf.nodeprof.test/js/benchmarkName will be tested.

- ``` mx test-specific [analysisName] --all ``` will test all available test cases (to analysisName if analysisName is specified)

- ``` mx jalangi [--analysis pathToAnalsis]* pathToMainProgram [arg]* ``` will run several Jalangi analyses for the program specified with pathToMainProgram with arguments.
  * You can set a coarse-grained instrumentation scope for your analysis: ``` mx jalangi --scope=[app|module|all] ```.
    - _app_: only the application code (without any code in the npm_modules folder or internal libraries);
    - _module_: _app_ code plus the node module code;
    - _all_: all the code including internal library, node modules and application code.
- You can exclude certain source code from instrumentation by
  * add a ```// DO NOT INSTRMENT``` at the beginning of the source code file
  * specify an exclusion list with ```mx jalangi --excl="keyword1,keyword2"``` so that any source code file whose full path containing one of these key words will be excluded from instrumentation.

- Attach a debugger ```mx jalangi -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000 ... ``` 
### Run using GraalVM

  You can directly run NodeProf with a pre-built jar file and the latest GraalVM.
  - Get nodeprof.jar
    * You can get the nodeprof.jar after running ```mx build```. The jar file will be generated in workspace-nodeprof/nodeprof.js/build
    * Or you can download a snapshot of the latest nodeprof [here](https://github.com/Haiyang-Sun/nodeprof.js/releases)

  - Download the latest GraalVM (e.g., graalvm-ce-linux-amd64-19.2.1.tar.gz) from [GraalVM on Github](https://github.com/oracle/graal/releases)

  - Decompress the package (e.g., to graalvm-ce-linux-amd64-19.2.1.tar.gz)

  - Run NodeProf with the following command:
    * ``` PATH_GRAALVM/bin/node --jvm --experimental-options --vm.Dtruffle.class.path.append=PATH_NODEPROF_JAR/nodeprof.jar --nodeprof PATH_NODEPROF/src/ch.usi.inf.nodeprof/js/jalangi.js [--analysis analysisFile]* test-file ``` (similar to ```mx jalangi```).
    * To specify the scope, add an option ```--nodeprof.Scope=[app|module|all]```
    * To add exclusion list, add an option ```--nodeprof.ExcludeSource="keyword1,keyword2"```

### Run using SubstrateVM (SVM)

Nodeprof can be executed using a SubstrateVM-based Graal.js build (during your build, you have already cloned a dependent project called _graal_ and another project called _graaljs_ in workspace-nodeprof).

(To be added soon)

### Source filters and selective instrumentation in NodeProf
#### Command-line options
NodeProf supports basic source selection on the command line. Coarse-grained inclusion is supported
via `--nodeprof.Scope=[app|module|all]` and explicit exclusion using `--nodeprof.ExcludeSource="keyword1,keyword2"`.

#### JavaScript API: source config object
More fine-grained control over instrumentation is provided by an API that is used inside JavaScript
(analysis) code. An analysis-specific filter can be installed by passing a configuration object
to NodeProf like this:

```
var sourceConfig = {excludes: 'badSource.js'}
sandbox.addAnalysis(new MyAnalysis(), sourceConfig);
```

The configuration object **is combined** with any global exclusions provided on the command line and
`DO NOT INSTRMENT` comments are always respected.
When passing a simple config object, three properties (`excludes`, `includes`, `internal`)
are used to control source selection.

```
var sourceConfig = {
    excludes: 'node_modules/some.module.dir,analysis', // similar to command line exclusion list above 
    internal: true || false, // optional: include internal files similar to --nodeprof.Scope=all option
    // OR
    includes: 'interesting.file.js,' // only include matching files (implies internal: true)
}
```

(Experimental) In order to instrument built-in code when using `includes`, add the special name `<builtin>` to the list. 

#### (Experimental) JavaScript API: instrumentation predicate

(Experimental) The most flexible and fine-grained level of control is provided by an **(experimental) instrumentation predicate**.
Instead of a configuration object, a predicate function can be passed to `addAnalysis()`:
  
```
var sourceConfig = function predicate(source) {
    if (/* test source obj */)
        return true; // instrument this source
    else if (/* other test */)
        return ['functionEnter', 'getField']; // instrument these callbacks only
    else
        return false; // do not instrument this source   
}
```

The instrumentation predicate **overrides all** command line options related to source selection. However,
`DO NOT INSTRMENT` comments are respected.
The `source` object passed to the instrumentation predicate has the following properties:

```
{
    name: "path/to/the/source" || "internal-name",
    internal: true || false
}
```

### Run ES6 modules
Use the flag ```--experimental-modules``` [detail](https://github.com/Haiyang-Sun/nodeprof.js/issues/50).
