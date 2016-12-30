var webpack = require("webpack");
var path = require("path");

var PACKAGE = require("./package.json");
var SRC_DIR = path.resolve(__dirname, "src/main/javascript");
var BUILD_DIR = path.resolve(__dirname, "target/mook-" + PACKAGE.version + "/js");


var config = {
    entry: SRC_DIR + "/index.js",
    output: {
        path: BUILD_DIR,
        filename: "bundle.js"
    },
    module : {
        loaders : [
            {
                test : /\.js/,
                include : SRC_DIR,
                loader : "babel"
            }
        ]
    }
};

module.exports = config;