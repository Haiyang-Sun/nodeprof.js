/*
 * ReDoS
 * See: https://nodesecurity.io/advisories/100
 *      https://github.com/garycourt/uri-js/issues/12
 */

var uri = require('uri-js');

var p = uri.parse("/%c0%ae%c0%ae/%c0%ae%c0%ae/windows\\win.ini")

