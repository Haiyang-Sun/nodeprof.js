# NodeProf: An efficient instrumentation and profiling framework for [Graal.js](https://github.com/graalvm/graaljs).
The tool is open source. 
Please feel free to report [issues](https://github.com/Haiyang-Sun/nodeprof.js/issues) or [contribute directly](https://github.com/Haiyang-Sun/nodeprof.js/pulls).

## GraalJS version
The latest supported GraalVM version is 22.3.3.

## Getting Started
Get the [mx](https://github.com/graalvm/mx) build tool:

```
git clone https://github.com/graalvm/mx.git
```

Use mx to download a JDK for building GraalVM and set the JAVA_HOME environment variable accordingly:

```
mx fetch-jdk --java-distribution labsjdk-ce-11
export JAVA_HOME=PATH_TO_THE_DOWNLOADED_JDK
```

Get dependent projects and build:

```
mkdir workspace-nodeprof
cd workspace-nodeprof
git clone https://github.com/Haiyang-Sun/nodeprof.js.git
cd nodeprof.js
mx sforceimports
mx build
```

Run tests:
```
mx test-all
```

Detailed explanation can be found in the [Tutorial](https://github.com/Haiyang-Sun/nodeprof.js/blob/master/Tutorial.md);

## Goals
The goals of NodeProf are:

* Use AST-level instrumentation which can benefit from the partial evaluation of the Graal compiler and have a much lower overhead compared to source-code instrumentation framework such as Jalangi
* Compatible to analysis written in Jalangi [detail](https://github.com/Haiyang-Sun/nodeprof.js/blob/master/Difference.md).
* Comprehensive coverage for NPM modules and Node.js libraries.
* Compliant to the latest ECMAScript specification (thanks to Graal.js)

## Publication

[ACM](https://dl.acm.org/doi/10.1145/3178372.3179527) / [Research Gate](https://www.researchgate.net/publication/323329222_Efficient_dynamic_analysis_for_Nodejs)

## Licence

NodeProf is available under the following license:

* [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)

## Acknowledgement

Thanks to Daniele Bonetta, Alexander Jordan, and Christian Humer from Oracle Labs for the help during the implementation.

## Contributors
- [Haiyang Sun](https://github.com/Haiyang-Sun)
- Alexander Jordan
- Daniele Bonetta
- [BarrensZeppelin](https://github.com/BarrensZeppelin)
