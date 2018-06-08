var p = new Promise(
  function(resolve, reject)
  {
    resolve(10);
  }
);
p.then(v => console.log(v));
