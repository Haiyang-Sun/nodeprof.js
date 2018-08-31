/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 */

// do not remove the following comment
// JALANGI DO NOT INSTRUMENT

(function(sandbox){  
  function DynCG() {
    
    let cg = new Map();
    let iidToLocation = new Map();
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
      addCallee(lastCallsite, iid);
    };

    this.endExecution = function() {
      //Output location information
      iidToLocation.forEach(function (value, key) {
        console.log(`// ${key}: ${value}`);
      });

      //Output dynamic call graph in Dot format
      console.log("digraph {");
      cg.forEach(function (value, key) {
        value.forEach(function (callee) {
          console.log(`  ${key} -> ${callee};`);
        })
      });
      console.log("}");
    };

  }
  sandbox.analysis = new DynCG();
})(J$);
