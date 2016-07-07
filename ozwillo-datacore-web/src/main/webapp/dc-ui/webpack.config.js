var webpack = require('webpack');
module.exports = {
  entry: ["./semantic/dist/semantic.js", "./src/index.js"],
  output: {
    filename: "bundle.js"
  },
  module: {
    loaders: [
      {
        test: /\.js$/,
        loader: 'babel-loader',
        exclude: [/node_modules/,/semantic/],
        query: {
          presets: [
            "es2015",
            "react",
            "stage-0",
          ],
          plugins: ["transform-object-rest-spread"]
        }
      },
    ]
  },
  resolve: {
    extensions: ['', '.js', '.json']
  },
  devServer: {
    port: 3000,
    historyApiFallback: {
      index: 'index.html',
    }
  },
  plugins:[
    new webpack.DefinePlugin({
      'process.env':{
        'NODE_ENV': JSON.stringify('production')
      }
    })
  ],
}
