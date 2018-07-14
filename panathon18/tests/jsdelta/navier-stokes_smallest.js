function setupNavierStokes() {
    FluidField();
}
function FluidField() {
    function reset() {
        size = (width + 2) * (height + 2);
        console.log("size:" + size);
    }
    this.setResolution = function (hRes, wRes) {
        {
            width = wRes;
            height = hRes;
            reset();
        }
    };
    this.setResolution(64, 64);
}
setupNavierStokes();
