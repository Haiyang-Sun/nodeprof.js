
## NodeProf Tutorial

### Installation

Get the __mx__ build tool and put it in your __PATH__:

```
git clone https://github.com/graalvm/mx.git
```

Get dependent projects and build:

```
$ mkdir workspace-nodeprof;
$ cd workspace-nodeprof;
$ git clone https://github.com/Haiyang-Sun/nodeprof.js.git;
$ cd nodeprof;
$ mx sforceimports
$ mx build
```

Run tests:
```
$ mx test
```

### Commands

- ``` mx test ``` runs all test cases and compare the output with expected values

- ``` mx unittests ``` runs all unit tests

- ``` mx test-specific analysisName [benchmarkName] ``` will automatically search Jalangi analysis defined in ```src/ch.usi.inf.nodeprof/js/analysis/analysisName``` and load them. [click here for more details](https://github.com/Haiyang-Sun/nodeprof.js/tree/master/src/ch.usi.inf.nodeprof/js/analysis) and compare the output of the run with expected output.
If benchmarkName is specified, only test files inside nodeprof/src/ch.usi.inf.nodeprof.test/js/benchmarkName will be tested.

- ``` mx test-specific [analysisName] --all ``` will test all available test cases (to analysisName if analysisName is specified)

- ``` mx jalangi pathToAnalsisFile+ pathToTestJSFile ``` will run one or more jalangi analyses for the program specified with pathToTestJSFile.

### Run using SubstrateVM (SVM)

Nodeprof can be executed using a SubstrateVM-based Graal.js build (during your build, you have already cloned a dependent project called _graal_ and another project called _graaljs_ in workspace-nodeprof).

(To be added soon)
