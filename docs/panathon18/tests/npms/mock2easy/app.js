/* 
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 */

var fs = require('fs')

var request = require('request');
var mock2easy = require("mock2easy")("x", "a", function (app) {
  var server = app.listen(3000, function () {
    request({
      url: 'http://localhost:3000/modify',
      method: "POST",
      json: true,
      body: JSON.stringify({"interfaceUrl":"/http","requiredParameters":[], "responseParameters":[{"kind":"mock", "rule": "Test"}]})
    }, function (error, response, body) {
      if (error) {
	console.log(error);
      }

      //Clean up the mock2easy mess
      fs.unlinkSync('./mock2easy/do.js');
      fs.unlinkSync('./mock2easy/http');
      fs.rmdirSync('mock2easy');

      fs.unlinkSync('./doc/menu.md');
      fs.rmdirSync('doc');
      
      server.close();
      
      //For some reason, the server doesn't shut down after closing.
      process.exit(0)
    });
  });
});
