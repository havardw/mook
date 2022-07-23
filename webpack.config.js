module.exports = {
    entry: "./src/main/javascript/index.tsx",
    output: {
        filename: "bundle.js",
        path: __dirname + "/target/classes/META-INF/resources/js"
    },

    module: {
        rules: [
            {
                test: /\.tsx?$/,
                use: "ts-loader",
                exclude: /node_modules/
            }
        ]
    },
    resolve: {
        extensions: [".tsx", ".ts", ".js"]
    }
};