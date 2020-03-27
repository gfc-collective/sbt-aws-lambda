// Create an instance of Scala.js class
const handler = require("./example-fastopt.js").Handler();

exports.foo = function(event, context, callback) {
    const param = event.param;
    // Call Scala.js method
    const result = handler.foo();
    callback(null, result);
}
exports.bar = function(event, context, callback) {
    const param = event.param;
    // Call Scala.js method
    const result = handler.bar();
    callback(null, result);
}
