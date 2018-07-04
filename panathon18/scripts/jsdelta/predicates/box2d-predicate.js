#!/usr/bin/env node

var execPred = function (filename) {
    var process = require("child_process");
    var output = null;
    const options = {
        stdio: "pipe"
    };

    try {
        output = process.execSync('node ' + filename, options);
    } catch (err) {
        return false;
    }

    if (output.indexOf("object") !== -1) {
        return true;
    }
    return false;
};

function main() {
   var arg = process.argv[2];
   if (execPred(arg)) {
      console.log("stop");
   } else {
   }
}

main();

