var Box2D = {};
Box2D.Dynamics = {};
(function () {
    function ea() {
    }
    Box2D.Dynamics.b2World = ea;
}());
function MakeNewWorld() {
    var World = Box2D.Dynamics.b2World;
    var world = new World();
    console.log(typeof world);
}
function runBox2D() {
    var world = MakeNewWorld();
}
runBox2D();
