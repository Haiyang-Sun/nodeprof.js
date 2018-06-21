function foo(...args) {
  return bar(...args);
}
function bar() {
  return arguments;
}
console.log(foo('bar'));
console.log(foo(42, 'baz'));
