/*******************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
J$={};
(function (sandbox) {
  if(typeof Graal !== 'object') {
    console.error("NodeProf only works with GraalVM")
    process.exit(1);
  }
  try {
    sandbox.adapter = __jalangiAdapter;
    sandbox.deprecatedIIDUsed = false;
    /*
     * J$.nativeLog(msg, logLevel)
     * - print the message string using the logger (i.e., System.out/err) inside the engine
     * - default log level is INFO
     * - consider using this function instead of console.log in the analysis in case you
     *   want to dump messages while instrumenting some internal library or builtins
     */
    sandbox.nativeLog = function(str, logLevel) {
      __jalangiAdapter.nativeLog(...arguments);
    }
    sandbox.nativeLog.DEBUG = 0;
    sandbox.nativeLog.INFO = 1;
    sandbox.nativeLog.WARNING = 2;
    sandbox.nativeLog.ERROR = 3;
    sandbox.iidToLocation = function(iid, _deprecatedIID){
      if(_deprecatedIID) {
        if(!sandbox.deprecatedIIDUsed){
          sandbox.deprecatedIIDUsed = true;
          console.trace("Warning! In NodeProf, iidToLocation only needs the iid (without sid). The iids as you get from the callbacks are unique across files.");
        }
        return sandbox.adapter.iidToLocation(_deprecatedIID);
      }
      return sandbox.adapter.iidToLocation(iid);
    };
    sandbox.iidToSourceObject = function(iid) {
      return sandbox.adapter.iidToSourceObject(iid);
    }
    sandbox.iidToCode = function(iid) {
      return sandbox.adapter.iidToCode(iid);
    }
    sandbox.getGlobalIID = function(iid) {
      return iid;
    };
    sandbox.enableAnalysis = function() {
      return sandbox.adapter.instrumentationSwitch(true);
    }
    sandbox.disableAnalysis = function() {
      return sandbox.adapter.instrumentationSwitch(false);
    }
  }catch (e){
    console.log("cannot load nodeprof jalangi adapter");
  }

  // Print NodeProf config when config says we are in debug mode
  const nodeprofConfig = sandbox.adapter.getConfig();
  if (nodeprofConfig.Debug) {
    console.log('[jalangi.js] NodeProf config:', nodeprofConfig);
  }

  sandbox.analyses=[];
  sandbox.enabledCBs = [];
  if(process.env.ENABLED_JALANGI_CBS && process.env.ENABLED_JALANGI_CBS.length > 0){
    sandbox.enabledCBs = process.env.ENABLED_JALANGI_CBS.split(",");
  }
  sandbox.addAnalysis = function(analysis, filterConfig){
    if(!analysis)
      return;
    sandbox.analyses.push(analysis);
    for(key in analysis){
      if(typeof analysis[key] == 'function' && ( (J$.enabledCBs.length == 0) || (J$.enabledCBs.indexOf(key)>-1))){
        sandbox.adapter.registerCallback(analysis, key, analysis[key]);
      }
    }
    if(!filterConfig) {
      sandbox.adapter.onReady(analysis);
    }else{
      sandbox.adapter.onReady(analysis, filterConfig);
    }
  }
  sandbox.endExecution = function(){
    for(var i = 0; i < sandbox.analyses.length; i++){
      var analysis = sandbox.analyses[i];
      if(analysis.endExecution && (typeof analysis.endExecution == 'function')){
        analysis.endExecution();
      }
    }
  }
  Object.defineProperty(sandbox, 'analysis', {
    get:function () {
      return sandbox.analyses;
    },
    set:function (a) {
      sandbox.addAnalysis(a);
    }
  });

  sandbox.startupPromises = [];
  sandbox.addStartupPromise = function(p) {
    if (p instanceof Promise) {
      sandbox.startupPromises.push(p);
    } else {
      console.log('addStartupPromise() should be used with Promise objects');
      process.exit(1);
    }
  }

  sandbox.getAstHelper = function() {
    const assert = require('assert');
    const util = require('util');
    const { esprima, estraverse } = require('./bundle.js');

    const helperObject = {
      logObjectLiteral(iid) {
        console.error('parsed ObjectLiteral:',
          util.inspect(
            esprima.parseScript(`(${J$.iidToCode(iid)})`),
            false, // showHidden
            8 // depth
          ));
      },
      parseObjectLiteral(iid) {
        // assumed to be ObjectLiteral, parse as expression wrapped in `(...)`
        const ast = esprima.parseScript(`(${J$.iidToCode(iid)})`);

        let gs = false;
        let f = [];

        estraverse.traverse(ast, {
          enter: function(node) {
            if (node.type === 'Property') {
              if (node.computed) {
                f.push('computed');
              } else {
                let prefix = '';
                if (node.kind === 'get' || node.kind === 'set') {
                  gs = true;
                  prefix = node.kind + 'ter';
                } else {
                  assert(node.kind === 'init');
                }
                assert(node.key.type === 'Literal' || node.key.type == 'Identifier');
                const name = node.key.type === 'Literal' ? node.key.value : node.key.name;
                f.push(`${prefix}-${name}`);
              }
              // don't parse any child nodes below this
              this.skip();
            }
          }
        });
        return { hasGetterSetter: gs, fields: f};
      }
    }

    return helperObject;
  }
}(J$));

/**
 * @Deprecated J$ fields from Jalangi
 * Should try to avoid using them
 *
 */
(function (sandbox) {
  /* Constant.js */
  var Constants = sandbox.Constants = {};
  Constants.isBrowser = !(typeof exports !== 'undefined' && this.exports !== exports);
  var APPLY = Constants.APPLY = Function.prototype.apply;
  var CALL = Constants.CALL = Function.prototype.call;
  APPLY.apply = APPLY;
  APPLY.call = CALL;
  CALL.apply = APPLY;
  CALL.call = CALL;
  var HAS_OWN_PROPERTY = Constants.HAS_OWN_PROPERTY = Object.prototype.hasOwnProperty;
  Constants.HAS_OWN_PROPERTY_CALL = Object.prototype.hasOwnProperty.call;
  var PREFIX1 = Constants.JALANGI_VAR = "J$";
  Constants.SPECIAL_PROP = "*" + PREFIX1 + "*";
  Constants.SPECIAL_PROP2 = "*" + PREFIX1 + "I*";
  Constants.SPECIAL_PROP3 = "*" + PREFIX1 + "C*";
  Constants.SPECIAL_PROP4 = "*" + PREFIX1 + "W*";
  Constants.SPECIAL_PROP_SID = "*" + PREFIX1 + "SID*";
  Constants.SPECIAL_PROP_IID = "*" + PREFIX1 + "IID*";
  Constants.UNKNOWN = -1;
  var HOP = Constants.HOP = function (obj, prop) {
    return (prop + "" === '__proto__') || CALL.call(HAS_OWN_PROPERTY, obj, prop); //Constants.HAS_OWN_PROPERTY_CALL.apply(Constants.HAS_OWN_PROPERTY, [obj, prop]);
  };
  Constants.hasGetterSetter = function (obj, prop, isGetter) {
    if (typeof Object.getOwnPropertyDescriptor !== 'function') {
      return true;
    }
    while (obj !== null) {
      if (typeof obj !== 'object' && typeof obj !== 'function') {
        return false;
      }
      var desc = Object.getOwnPropertyDescriptor(obj, prop);
      if (desc !== undefined) {
        if (isGetter && typeof desc.get === 'function') {
          return true;
        }
        if (!isGetter && typeof desc.set === 'function') {
          return true;
        }
      } else if (HOP(obj, prop)) {
        return false;
      }
      obj = obj.__proto__;
    }
    return false;
  };
  Constants.debugPrint = function (s) {
    if (sandbox.Config.DEBUG) {
      console.log("***" + s);
    }
  };
  Constants.warnPrint = function (iid, s) {
    if (sandbox.Config.WARN && iid !== 0) {
      console.log("        at " + iid + " " + s);
    }
  };
  Constants.seriousWarnPrint = function (iid, s) {
    if (sandbox.Config.SERIOUS_WARN && iid !== 0) {
      console.log("        at " + iid + " Serious " + s);
    }
  };

  var Config = sandbox.Config = {};

  /* Config.js */
  Config.DEBUG = false;
  Config.WARN = false;
  Config.SERIOUS_WARN = false;
  Config.MAX_BUF_SIZE = 64000;
  Config.LOG_ALL_READS_AND_BRANCHES = false;
  Config.ENABLE_SAMPLING = false;

})(J$);



process.on('SIGINT', function(){
  process.exit();
});

process.on('exit', function () { J$.endExecution(); });

path=require('path');


// jalangi.js [--analysis XXX]* [--initParam key:value]* [testFile [testArgs]*]
function loadAnalysis(){
  var arg0 = process.argv[0];
  var i = 2; // start from 3rd arg: 0 => node, 1 => jalangi.js
  var analyses = [];
  var execPath;
  var length = process.argv.length;
  
  // read global options
  const optStrings = ['--analysis', '--exec-path'];
  for (; i < length && optStrings.includes(process.argv[i]); i++) {
    const optString = process.argv[i];
    if (++i >= length)
      throw 'missing option argument';
    
    switch (optString) {
      case '--analysis':
        analyses.push(process.argv[i]);
        break;
      case '--exec-path':
        execPath = process.argv[i];
        break;
    }
  }
  
  // read initParam args
  J$.initParams = {};
  for (; i < length && process.argv[i] == '--initParam'; ++i) {
    if (++i >= length)
      throw 'missing key:value pair'
    
    // key:value pair
    var pair = process.argv[i]
    // take the first ':' to allow more occurrences in the value string
    var separatorIndex = pair.indexOf(':');
    var key = pair.substring(0, separatorIndex);
    var value = pair.substring(separatorIndex+1);
    
    J$.initParams[key] = value;
  }
  
  // the real program to run  
  if (i == process.argv.length)
    throw 'no main program is given';
  process.argv[i] = path.resolve(process.argv[i]);
  
  // load analyses
  analyses.forEach(analysis => {
    try {
      require(path.resolve(analysis));
    } catch(e) {
      console.log('error while loading analysis %s', analysis);
      console.log(e);
      process.exit(-1);
    }
  });

  // remove the analysis part in the argv
  process.argv = process.argv.slice(i);

  // override with --exec-path
  if (execPath) {
    process.execPath = execPath;
    arg0 = execPath;
  }

  // put back the entry program
  process.argv.unshift(arg0);
}

loadAnalysis();
if (J$.startupPromises.length === 0) {
  require('module').runMain();
} else {
  Promise.all(J$.startupPromises).then( () => {
    require('module').runMain();
  }, error => {
    console.log('Analysis startup promise failed:', error);
    process.exit(-1);
  });
}
