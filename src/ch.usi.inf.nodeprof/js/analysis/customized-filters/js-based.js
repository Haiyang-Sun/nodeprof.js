/*******************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 //DO NOT INSTRUMENT
((function(sandbox){
  function First() {
    const analysis = 'analysis 1';
    this.getField = function(iid, base, offset, val, isComputed, isOpAssign, isMethodCall) {
      console.log("%s: getField: %s / %s / %d", analysis, offset, J$.iidToLocation(iid), arguments.length);
    };
    this.functionEnter = function (iid, f, dis, args) {
      if (f.name == '')
        return;
      console.log("%s: functionEnter: %s / %s / %d", analysis, f.name, J$.iidToLocation(iid), arguments.length);
    };
  }
  sandbox.addAnalysis(new First(), function filter(source) {
    if (source.internal)
      return false;
    // instruments one file
    if (source.name.endsWith('enterExit.js'))
      return true;
    return false;
  });

  function Second() {
    const analysis = 'analysis 2';
    this.getField = function(iid, base, offset, val, isComputed, isOpAssign, isMethodCall) {
      console.log("%s: getField: %s / %s / %d", analysis, offset, J$.iidToLocation(iid), arguments.length);
    };
    this.functionEnter = function (iid, f, dis, args) {
      if (f.name == '')
        return;
      console.log("%s: functionEnter: %s / %s / %d", analysis, f.name, J$.iidToLocation(iid), arguments.length);
    };
  }
  sandbox.addAnalysis(new Second(), function filter(source) {
    if (source.internal)
      return false;
    // instruments one callback in one file
    if (source.name.endsWith('enterExit.js'))
      return ['functionEnter'];
    return false;
  });

  function Third() {
    const analysis = 'analysis 3';
    this.getField = function(iid, base, offset, val, isComputed, isOpAssign, isMethodCall) {
      console.log("%s: getField: %s / %s / %d", analysis, offset, J$.iidToLocation(iid), arguments.length);
    };
    this.functionEnter = function (iid, f, dis, args) {
      if (f.name == '')
        return;
      console.log("%s: functionEnter: %s / %s / %d", analysis, f.name, J$.iidToLocation(iid), arguments.length);
    };
  }
  sandbox.addAnalysis(new Third(), function filter(source) {
    // excludes everything
    return false;
  });
}
)(J$));
