function foo() {
  this.bar = function(){}
}

foo(1,2,3)

var f = new foo(4,5,6);

f.bar(7,8,9)
