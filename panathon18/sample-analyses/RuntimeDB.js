/*
 * Copyright 2014 University of California, Berkeley.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Author: Liang Gong

((function (sandbox) {
    function RuntimeDB() {
        // ---- DB library functions start ----
        var obj_id = 1;
        var analysisDB = {};
        var Constants = sandbox.Constants;
        var HOP = Constants.HOP;

        this.getNextObjId = function () {
            return obj_id++;
        }

        this.getByIndexArr = function (indexArr) {
            var curDB = analysisDB;
            for (var i = 0; i < indexArr.length; i++) {
                if (!HOP(curDB, indexArr[i] + "")) {
                    return undefined;
                }
                curDB = curDB[indexArr[i] + ""];
            }
            return curDB;
        }

        this.setByIndexArr = function (indexArr, data) {
            var curDB = analysisDB;
            for (var i = 0; i < indexArr.length - 1; i++) {
                if (!HOP(curDB, indexArr[i] + "")) {
                    curDB[indexArr[i] + ""] = {};
                }
                curDB = curDB[indexArr[i] + ""];
            }

            curDB[indexArr[indexArr.length - 1] + ""] = data;
        }

        this.addCountByIndexArr = function (indexArr) {
            var metaData = this.getByIndexArr(indexArr);
            if (typeof metaData === 'undefined') {
                this.setByIndexArr(indexArr, {'count': 1});
            } else {
                metaData.count++;
            }
        }

        this.getCountByIndexArr = function (indexArr) {
            var metaData = this.getByIndexArr(indexArr);
            if (typeof metaData === 'undefined') {
                return undefined;
            } else {
                return metaData.count;
            }
        }

        // ---- DB library functions end ----
    }

    sandbox.RuntimeDB = RuntimeDB;

})(J$));


