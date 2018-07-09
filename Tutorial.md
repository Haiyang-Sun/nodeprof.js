
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

### Run using GraalVM

  You can directly run NodeProf with a pre-built jar file and the latest GraalVM.
  - Get nodeprof.jar
    * You can get the nodeprof.jar after running ```mx build```. The jar file will be generated in workspace-nodeprof/nodeprof.js/build
    * Or you can download a snapshot of the latest nodeprof [here](https://github.com/Haiyang-Sun/nodeprof.js/tree/master/snapshot/nodeprof.jar)

  - Download the latest GraalVM (e.g., graalvm-ee-1.0.0-rc2-linux-amd64.tar.gz) [here](http://www.oracle.com/technetwork/oracle-labs/program-languages/downloads/index.html)

  - Decompress the package (e.g., to graalvm-ee-1.0.0-rc2)

  - Run NodeProf with the following command:
    * ``` PATH_GRAALVM/bin/node --jvm --jvm.Dtruffle.class.path.append=PATH_NODEPROF_JAR/nodeprof.jar --nodeprof PATH_NODEPROF/src/ch.usi.inf.nodeprof/js/jalangi.js [--analysis analysisFile]* test-file ``` (similar to ```mx jalangi```).
    * To specify the scope, add an option ```--nodeprof.Scope=[app|module|all]```
    * To add exclusion list, add an option ```--nodeprof.ExcludeSource="keyword1,keyword2"```

### Run using SubstrateVM (SVM)

Nodeprof can be executed using a SubstrateVM-based Graal.js build (during your build, you have already cloned a dependent project called _graal_ and another project called _graaljs_ in workspace-nodeprof).

(To be added soon)
