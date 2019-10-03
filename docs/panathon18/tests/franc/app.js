/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 */

const franc = require('franc');

const str = 'NodeProf ist ein Werkzeug zur Analyse von Programmen.';
console.log('Running franc on "%s"', str);
console.log('Top 3 language guesses:', franc.all(str).map(x => x[0]).slice(0,3));
