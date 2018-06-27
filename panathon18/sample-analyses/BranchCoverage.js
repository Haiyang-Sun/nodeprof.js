// do not remove the following comment
// JALANGI DO NOT INSTRUMENT

/**
 * @author  Koushik Sen
 *
 */

(function (sandbox) {
    var trueBranches = {};
    var falseBranches = {};
    sandbox.log = console.log;

    function MyAnalysis() {

        this.conditional = function (iid, result) {
            var id = J$.getGlobalIID(iid);
            if (result)
                trueBranches[id] = (trueBranches[id]|0) + 1;
            else
                falseBranches[id] = (falseBranches[id]|0) + 1;
        };

        this.endExecution = function () {
            print(trueBranches, "True");
            print(falseBranches, "False");
        };
    }

    function print(map, str) {
        for (var id in map)
            if (map.hasOwnProperty(id)) {
                sandbox.log(str + " branch taken at " + J$.iidToLocation(id) + " " + map[id] + " times");
            }
    }

    sandbox.analysis = new MyAnalysis();
}(J$));

