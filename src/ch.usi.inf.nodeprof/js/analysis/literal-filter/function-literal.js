// DO NOT INSTRUMENT

function LiteralFilter(){
  this.literal = function(iid, val, undef, type){
    console.log(J$.iidToLocation(iid), type, val);
  }
  this.literal.types = ["FunctionLiteral"];
}

J$.addAnalysis(new LiteralFilter());
