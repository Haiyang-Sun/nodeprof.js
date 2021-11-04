import os, zipfile, re, shutil
from os.path import join, exists, isdir, getmtime
from argparse import ArgumentParser, RawDescriptionHelpFormatter

import mx, mx_sdk
from mx_unittest import unittest
import mx_unittest
from mx import BinarySuite, VC
from contextlib import contextmanager
from mx_graal_nodejs import npm

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

def runJNode(args):
    cmdArgs = prepareJalangiCmdLine(['--jvm']+args);
    mx.run(cmdArgs, nonZeroIsFatal=True);


def _runJalangi(args, svm=False, debug=False, outFile=None, trace=False):
    from mx_graal_nodejs import run_nodejs
    jalangiArgs = ['--vm.ea', '--experimental-options', '--engine.InstrumentExceptionsAreThrown=true', '--nodeprof', '--nodeprof.Analysis=NodeProfJalangi']
    if debug:
        jalangiArgs += ["--nodeprof.Debug"];
    if trace:
        jalangiArgs += ["--nodeprof.TraceEvents"];

    cmdArgs = [];
    if svm:
        from subprocess import call
        if outFile:
            return call(["./svm.sh"]+ jalangiArgs+args, stdout=open(outFile,'w'));
        else:
            return call(["./svm.sh"]+ jalangiArgs+args);
    else:
        cmdArgs = prepareJalangiCmdLine(['--jvm']+jalangiArgs + args);
        if outFile:
            out=OutputCapture(outFile);
        else:
            out=None;
        ret = mx.run(cmdArgs, nonZeroIsFatal=True, out=out);
        return ret;

def _testJalangi(args, analysisHome, analysis, force=False, testsuites=[], keepGoing=True):
    analysisOpt = [];
    if os.path.exists (join(analysisHome, "config")):
        config = open (join(analysisHome, "config"));
        for line in config:
            line = line.rstrip()
            words = line.split(" ");
            f = words[0];
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

            mx.logv("analysis parameters from config: " + str(words[1:]))

            # read analysis parameters from config
            for w in words[1:]:
                # ignore download URLs
                if w.startswith('http'):
                    continue
                # detect --nodeprof.Foo=bar style parameters
                if w.startswith('--nodeprof'):
                    args += [w]
                # otherwise treat as analysis (jalangi.js) parameters
                else:
                    analysisOpt += [w]

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
                runJalangi(args + analysisOpt+[join(testHome,testfile)], outFile=outFile, tracable=False);
                if not expectExist:
                    print("Ignored @"+analysis);
                    continue;
                with open(fn) as fexp:
                    with open(outFile) as foutput:
                        if(foutput.read() == fexp.read()):
                            print("Pass @"+analysis);
                        else:
                            print("Fail @"+analysis);
                            from subprocess import call
                            call(["diff", fn, outFile])
                            if not (force or keepGoing):
                                sys.exit(1);

def testJalangi(args):
    import sys
    import os.path
    """test jalangi"""
    print ("Testing NodoProf Jalangi API");

    analysisdir = join(_suite.dir, 'src/ch.usi.inf.nodeprof/js/analysis');
    testdir = join(_suite.dir, 'src/ch.usi.inf.nodeprof.test/js');
    vmArgs = ["--symbolic-locs"];
    all = False;
    analyses = [];
    force = False;
    keepGoing = False;
    testsuites = [];
    for arg in args:
        if arg == "--all":
            all = True;
            continue;
        elif arg == "--force":
            force = True;
            continue;
        elif arg == "--keep-going":
            keepGoing = True;
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
            _testJalangi(vmArgs, join(analysisdir, analysis), analysis, force, testsuites, keepGoing);
    elif analyses:
        for analysis in analyses:
            _testJalangi(vmArgs, join(analysisdir, analysis), analysis, force, testsuites, keepGoing);
    else:
        print ("Usage: mx test-specific [analysis-names...] [--all]")

def runJalangi(args, excl="", outFile=None, tracable=True):
    """run jalangi"""
    jalangiArgs = [join(_suite.dir, "src/ch.usi.inf.nodeprof/js/jalangi.js")]

    if not os.path.exists(join(_suite.dir, "src/ch.usi.inf.nodeprof/js/bundle.js")):
        npmDeps(args);

    # jalangi arg parser (accepts GraalVM '--nodeprof.' options for convenience)
    parser = ArgumentParser(prog="mx jalangi", description="Run NodeProf-Jalangi")
    # analysis (multiple arguments allowed)
    parser.add_argument("--analysis", "--nodeprof.Analysis", help="Jalangi analysis", action="append", default=[])
    # options
    parser.add_argument("--debug", "--nodeprof.Debug", help="enable NodeProf debug logging", action="store_true")
    parser.add_argument("--debugger", action="store_true")
    parser.add_argument("--excl", "--nodeprof.ExcludeSource", help="exclude sources", default="")
    parser.add_argument("--scope", "--nodeprof.Scope", help="instrumentation scope", choices=["app", "module", "all"], default="module")
    parser.add_argument("--symbolic-locs", "--nodeprof.SymbolicLocations", help="symbolic locations", action="store_true")
    # mx-only options
    parser.add_argument("--svm", help="enable SubstrateVM", action="store_true")
    parser.add_argument("--trace", help="enable instrumentation API tracing", action="store_true")

    parsed, args = parser.parse_known_args(args)

    # process analysis args
    for analysis_arg in parsed.analysis:
        # check if analysis file exists
        analysisPath = os.path.abspath(analysis_arg);
        if not os.path.exists(analysis_arg):
            mx.log_error("analysis file "+analysis_arg+" ("+analysisPath+") does not exist")
            sys.exit(1);

    if len(parsed.analysis) == 0:
        mx.log("Warning: no Jalangi --analysis specified")

    # append analyses and unparsed args
    jalangiAnalysisArg = [j for i in parsed.analysis for j in ['--analysis', i]] + args
    # exclude analyses by default
    excl = ','.join([i for i in parsed.excl.split(',') if i != ''] + [os.path.abspath(i) for i in parsed.analysis])

    jalangiArgs = (["--vm.Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000"] if parsed.debugger else []) + ["--nodeprof.Scope="+parsed.scope] + (["--nodeprof.ExcludeSource="+excl] if len(excl) else []) + (["--nodeprof.SymbolicLocations=true"] if parsed.symbolic_locs else []) + jalangiArgs + jalangiAnalysisArg
    _runJalangi(jalangiArgs, outFile=outFile, svm=parsed.svm, debug=parsed.debug, trace=(tracable and parsed.trace));

def unitTests(args):
    """run tests for the example analysis"""
    print("Starting JUnit Test")
    uArgs = [];
    commonArgs = uArgs + ['-ea', 'ch.usi.inf.nodeprof.test']
    unittest(commonArgs)
    print("JUnit Test Finishes")

class TestDownload:
    npmDir = join(_suite.dir, 'test')
    def __init__(self, name, url, pathPrefix):
        self.name = name
        self.url = url
        self.downloadRoot = join(self.npmDir, name)
        self.testRoot = join(self.npmDir, name, pathPrefix)
        if os.path.exists(join(self.npmDir, name, pathPrefix)):
            mx.log('Found %s, skipping download.' % self.testRoot)
        else:
            self.downloadSource()
            mx.log('Running NPM install for %s in %s' % (self.name, self.testRoot))
            npm(['install'], cwd=self.testRoot)
    def downloadSource(self):
        download = join(self.npmDir, self.name + '.zip')
        mx.download(download, [self.url])
        with zipfile.ZipFile(download) as zf:
            zf.extractall(self.downloadRoot)
    def run(self, args, testPath):
        runJalangi(args + ['--analysis', 'test/npm-analysis.js', join(self.testRoot, testPath)])

def testNpm(args):
    lodash = TestDownload('lodash', 'https://github.com/lodash/lodash/archive/4.17.21.zip', 'lodash-4.17.21')
    lodash.run(args, join('test', 'test-fp.js'))

def test(args):
    unitTests(args)
    testJalangi(args +["--all"]);

def checkCopyrightHeaders(args):
    # create list of Java sources to check
    with open('files_to_check', 'w') as f:
        mx.run(['git', 'ls-files', '--', '*.java'], out=f)
    
    # create an overrides file so that NodeProf copyright can be checked
    with open(join(_suite.dir, 'mx.nodeprof/copyrights/overrides'), 'w') as f:
        mx.run(['awk', '{print $0",nodeprof.copyright"}', 'files_to_check'], out=f)

    # run the actual check (overrodes file needs to be passed explicitly)
    return mx.checkcopyrights(['--primary', '--', '--file-list', 'files_to_check'])

def npmDeps(args):
    npm(['install'], cwd=join(_suite.dir, 'src/ch.usi.inf.nodeprof/js/npm-deps'))
    npm(['run', 'bundle'], cwd=join(_suite.dir, 'src/ch.usi.inf.nodeprof/js/npm-deps'))

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
    'test-npm': [testNpm, ''],
    'jalangi': [runJalangi, ''],
    'jnode': [runJNode, ''],
    'checkcopyrights-nodeprof': [checkCopyrightHeaders, ''],
    'npm-deps': [npmDeps, ''],
})
