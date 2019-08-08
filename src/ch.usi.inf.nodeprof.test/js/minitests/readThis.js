function Foo(){
    let self = this;
    self.a = 0;
}

let foo = new Foo();

function bar() {
  return this;
}
let baz = bar();
