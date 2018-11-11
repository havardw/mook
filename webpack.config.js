module.exports = {
    entry: "./src/main/javascript/index.js",
    output: {
        filename: "bundle.js",
        path: __dirname + "/target/webpack/js"
    },

    module: {
        rules: [
            {
                test: /\.js$/,
                use: {
                    loader: "babel-loader",
                    options: {
                        presets: ["@babel/preset-env", "@babel/preset-react"]
                    }
                }
            }
        ]
    }
};