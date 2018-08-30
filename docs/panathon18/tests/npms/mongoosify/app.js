/* 
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 */

var mongoosify = require('mongoosify');
var mySchema = mongoosify( {
  "type": "object",
  "properties": {
    "lastName": {"type": "String;" + "Test"}
  }
});
