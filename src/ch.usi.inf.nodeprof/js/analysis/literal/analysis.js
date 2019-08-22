// DO NOT INSTRUMENT

function Literal(){
  this.literal = function(iid, val, hasGetterSetter, type, fields){
    console.log(J$.iidToLocation(iid), typeof(val), hasGetterSetter, type, fields);
  }
}

J$.addAnalysis(new Literal());
