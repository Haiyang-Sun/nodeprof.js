/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 */
 // JALANGI DO NOT INSTRUMENT

//Predicate for selective instrumentation
function filter(source){
  if(source.internal)
    return false;
  //Instruments and logs only literals in modules of interest
  if (source.name.includes('escape') || source.name.includes('sanitize') || source.name.includes('decode')|| source.name.includes('encode'))
    return ['literal'];
  return false;
}

module.exports = filter;