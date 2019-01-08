// DO NOT INSTRUMENT

(function(){
    function SimpleAnalysis(){
        let self = this;
        if(J$.initParams.analysis) {
            J$.initParams.analysis.split(",").forEach(
                cb => {
                    self[cb] = function(iid){
                        if(!isNaN(iid)) {
                            J$.nativeLog(cb+" => "+J$.iidToLocation(iid));
                        }
                    }
                }
            );
        }
    }

    J$.addAnalysis(new SimpleAnalysis());
})();
