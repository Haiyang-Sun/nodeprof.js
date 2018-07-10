import os, zipfile, re, shutil
from os.path import join, exists, isdir, getmtime

import mx, mx_sdk
from mx_unittest import unittest
import mx_unittest
from mx import BinarySuite, VC
from contextlib import contextmanager

import sys
_suite = mx.suite('nodeprof')

def prepareJalangiCmdLine(args):
    from mx_graal_nodejs import _setEnvVar, setupNodeEnvironment
    _node = mx.suite('graal-nodejs')
    mode, vmArgs, progArgs = setupNodeEnvironment(args)
    _setEnvVar('NODE_JVM_CLASSPATH', mx.classpath(['NODEPROF']))
    _setEnvVar('NODE_JVM_OPTIONS', ' '.join(vmArgs))
    return [join(_node.dir, 'out', mode, 'node')] + progArgs

class OutputCapture:
    def __init__(self, outFile):
        self.outFile = outFile;
        if self.outFile:
            self.fout = open(self.outFile, 'w')
        else:
            self.fout = None;
    def __call__(self, data):
        if self.fout :
            self.fout.write(data);

def _runJalangi(args, svm=False, debug=False, outFile=None):
    from mx_graal_nodejs import run_nodejs
    jalangiArgs = ['--nodeprof', '--nodeprof.Analysis=NodeProfJalangi']
    if debug:
        jalangiArgs += ["--nodeprof.Debug"];
    cmdArgs = [];
    if svm:
        from subprocess import call
        if outFile:
            return call(["../graal/vm/mxbuild/linux-amd64/GRAALVM_CMP_GU_GVM_INS_JS_NJS_NP_POLYNATIVE_PRO_RGX_SVM_TFL_LIBPOLY_POLY/graalvm-1.0.0-rc2-dev/bin/node"]+ jalangiArgs+args, stdout=open(outFile,'w'));
        else:
            return call(["../graal/vm/mxbuild/linux-amd64/GRAALVM_CMP_GU_GVM_INS_JS_NJS_NP_POLYNATIVE_PRO_RGX_SVM_TFL_LIBPOLY_POLY/graalvm-1.0.0-rc2-dev/bin/node"]+ jalangiArgs+args);
    else:
        cmdArgs = prepareJalangiCmdLine(['--jvm']+jalangiArgs + args);
        if outFile:
            out=OutputCapture(outFile);
        else:
            out=None;
        ret = mx.run(cmdArgs, nonZeroIsFatal=True, out=out);
        return ret;

def _testJalangi(args, analysisHome, analysis, force=False, testsuites=[]):
    analysisOpt = [];
    if os.path.exists (join(analysisHome, "config")):
        config = open (join(analysisHome, "config"));
        for line in config:
            line = line.rstrip()
            f = line.split(" ")[0];
            if not os.path.exists (join(analysisHome, f)):
                if len(line.split(" ")) == 1:
                    print("analysis file "+f + " doesn't exist!")
                    continue;
                elif len(line.split(" ")) > 1:
                    url = line.split(" ")[-1]
                    mx.download(join(analysisHome, f), [url])
                    if not os.path.exists (join(analysisHome, f)):
                        print("analysis file "+f + " doesn't exist and cannot download it from "+url)
                        continue;
                    else:
                        print("downloaded analysis file "+f + " from "+url)
                else:
                    continue;
            analysisOpt += ["--analysis"];
            analysisOpt += [join(analysisHome, f)];
    else:
        for analysisJS in os.listdir(analysisHome):
            if analysisJS.endswith(".js"):
                analysisOpt += ["--analysis"];
                analysisOpt += [join(analysisHome, analysisJS)];
    if not analysisOpt:
        return;
    testdir = join(_suite.dir, 'src/ch.usi.inf.nodeprof.test/js')
    if not testsuites:
        testsuites = os.listdir(testdir)
    for testSuite in testsuites:
        testHome = join(testdir, testSuite);
        for testfile in os.listdir(testHome):
            if testfile.endswith('.js') and not(testfile.endswith('_jalangi_.js')):

                fn = join(analysisHome, testSuite+"."+testfile+".expected");
                expectExist = os.path.exists(fn);
                if not force and not expectExist:
                    continue;
                print('Testing ' + testfile + " in " + testSuite+" with analysis "+analysis)
                outFile = join(analysisHome,testSuite+"."+testfile+".output");
                runJalangi(args + analysisOpt+[join(testHome,testfile)], outFile=outFile);
                if not expectExist:
                    print("Ignored @"+analysis);
                    continue;
                with open(fn) as fexp:
                    with open(outFile) as foutput:
                        if(foutput.read() == fexp.read()):
                            print("Pass @"+analysis);
                        else:
                            print("Fail @"+analysis);
                            if not force:
                                sys.exit(1);

def testJalangi(args):
    import sys
    import os.path
    """test jalangi"""
    print ("Testing NodoProf Jalangi API");

    analysisdir = join(_suite.dir, 'src/ch.usi.inf.nodeprof/js/analysis');
    testdir = join(_suite.dir, 'src/ch.usi.inf.nodeprof.test/js');
    vmArgs = [];
    all = False;
    analyses = [];
    force = False;
    testsuites = [];
    for arg in args:
        if arg == "--all":
            all = True;
            continue;
        elif arg == "--force":
            force = True;
            continue;
        elif os.path.exists (join(analysisdir, arg)) :
            analyses += [arg];
            print ("Adding analysis "+arg)
        elif os.path.exists (join(testdir, arg)) :
            testsuites += [arg];
            print ("Adding testsuite "+arg)
        else:
            vmArgs += [arg];

    if all:
        for analysis in sorted(os.listdir(analysisdir)):
            _testJalangi(vmArgs, join(analysisdir, analysis), analysis, force, testsuites);
    elif analyses:
        for analysis in analyses:
            _testJalangi(vmArgs, join(analysisdir, analysis), analysis, force, testsuites);
    else:
        print ("Usage: mx test-specific [analysis-names...] [--all]")

def runJalangi(args, excl="", outFile=None):
    """run jalangi"""
    jalangiAnalysisArg = []
    jalangiArgs = [join(_suite.dir, "src/ch.usi.inf.nodeprof/js/jalangi.js")]
    e_flag=False;
    a_flag=False;
    debug=False;
    scope='module';
    svm=False;
    for arg in args:
        if e_flag:
            excl += ","+arg;
            e_flag = False;
        elif arg == "--svm":
            svm = True;
        elif arg == "--excl":
            e_flag = True;
        elif arg == "--log":
            jalangiArgs += ['--nodeprof.Debug'];
        elif arg == "--debug":
            debug = True;
        elif arg.startswith('--scope'):
            scope = arg.split('=')[1]
        else:
            jalangiAnalysisArg += [arg];
            # exclude analysis file from instrumentation by default
            if arg == "--analysis":
                a_flag = True;
            elif a_flag:
                analysisPath = os.path.abspath(arg);
                if not os.path.exists (arg):
                    print "analysis file "+arg+"("+analysisPath+") does not exist"
                    sys.exit(1);
                excl += ","+analysisPath;
                a_flag = False;
    jalangiArgs = ["--nodeprof.Scope="+scope, "--nodeprof.ExcludeSource="+excl] + jalangiArgs + jalangiAnalysisArg;
    _runJalangi(jalangiArgs, outFile=outFile, svm=svm, debug=debug);

def unitTests(args):
    """run tests for the example analysis"""
    print("Starting JUnit Test")
    uArgs = [];
    commonArgs = uArgs + ['-ea', 'ch.usi.inf.nodeprof.test']
    unittest(commonArgs)
    print("JUnit Test Finishes")

def test(args):
    unitTests(args)
    testJalangi(args +["--all"]);

@contextmanager
def _import_substratevm():
    try:
        import mx_substratevm
    except:
        mx.abort("Cannot import 'mx_substratevm'. Did you forget to dynamically import SubstrateVM?")
    yield mx_substratevm

mx_sdk.register_graalvm_component(mx_sdk.GraalVmTool(
    suite=_suite,
    name='NodeProf',
    short_name='np',
    dir_name='nodeprof',
    license_files=[],
    third_party_license_files=[],
    truffle_jars=['nodeprof:NODEPROF'],
    support_distributions=['nodeprof:NODEPROF_GRAALVM_SUPPORT'],
    include_by_default=True,
))

def mx_post_parse_cmd_line(args):
    try:
        import mx_substratevm
        mx_substratevm.tools_map['nodeprof'] = mx_substratevm.ToolDescriptor(image_deps=['nodeprof:NODEPROF'])
    except:
        pass # SubstrateVM is not available

mx.update_commands(_suite, {
    'test-all': [test, ''],
    'test-unit': [unitTests, ''],
    'test-specific': [testJalangi, ''],
    'jalangi': [runJalangi, ''],
})
