//DO NOT INSTRUMENT
(function(){
  function PromiseAnalysis(){
    this.promisePre = function(name, func){
      console.log("promise pre "+name);
    }
    this.asyncRootPost = function(iid, ret, p, isGenerator){
      console.log("async root post " + (p instanceof Promise));
    }
    this.awaitPre = function(iid, val) {
      console.log("await pre");
    }
  }
  J$.addAnalysis(new PromiseAnalysis());
})();
