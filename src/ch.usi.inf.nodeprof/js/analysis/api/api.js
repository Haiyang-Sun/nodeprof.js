/*******************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
(function(sandbox){
  J$.nativeLog('default');
  J$.nativeLog('debug', J$.nativeLog.DEBUG); // unchecked: debug off in tests
  J$.nativeLog('info', J$.nativeLog.INFO);
  J$.nativeLog('warning', J$.nativeLog.WARNING); // unchecked: emits to stderr
  J$.nativeLog('error', J$.nativeLog.ERROR); // unchecked: emits to stderr
  J$.nativeLog('neg level', -1);
  J$.nativeLog('string level', "1");

  function SourceObjTest() {
    this.functionEnter = function (iid, f, dis, args) {
      var locObj = J$.iidToSourceObject(iid);
      if (locObj.symbolic) {
        console.log(`symbolic location '${locObj.symbolic}':`, J$.iidToLocation(iid));
      }

      if (f.name !== 'foo')
        return;

      console.log('this is global:', this === global);
      console.log(global.__jalangiAdapter);
      console.log('num props:', Object.getOwnPropertyNames(locObj).length);
      console.log('name:', locObj.name);
      // do not log the range directly, it includes the Node.js module wrapper function as a prefix
      console.log('src length from range:', locObj.range[1] - locObj.range[0]);
      console.log('loc.start:', locObj.loc.start);
      console.log('loc.end:', locObj.loc.end);
      console.log('symbolic:', !!locObj.symbolic);
    };
  }

  sandbox.addAnalysis(new SourceObjTest());
}
)(J$);
