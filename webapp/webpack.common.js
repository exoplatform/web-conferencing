const path = require("path");

let config = {
  context: path.resolve(__dirname, "."),
  // set the entry point of the application
  // can use multiple entry
  entry: {
    admin: "./src/main/webapp/vue-apps/Admin/main.js",
    callButtons: "./src/main/webapp/vue-apps/CallButtons/main.js",
    webConferencingNotificationExtension: "./src/main/webapp/vue-apps/Web-conferencing-notifications/main.js",
    webConferencing: './src/main/webapp/vue-apps/SpaceAdministrationVisioConference/main.js'
  },
  output: {
    filename: "js/[name].bundle.js",
    libraryTarget: "amd"
  },
  module: {
    rules: [
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: [
          "babel-loader",
          "eslint-loader"
        ]
      },
      {
        test: /\.vue$/,
        use: [
          "vue-loader",
          "eslint-loader"
        ]
      }
    ]
  },
  externals: {
    vue: "Vue",
    vuetify: "Vuetify",
  }
};

module.exports = config;