const WebConferencingCallPlugin = [{
  target : "chat",
  // configuration defined here is used in exo-addons\web-conferencing\webapp\src\main\webapp\vue-apps
  // \webConferencingCall\components\CallDropdown.vue with
  // exo-addons\chat-application\application\src\main\webapp\vue-app\components\ExoChatRoomDetail.vue and connects them
  // key should be unique and used in parent component as a ref to WebConferencingCall component
  key : "callNavigation",
  rank : 20,
  // iconName is a name of the icon which is displayed on action button with 'onExecute' action
  // iconName should be one of the names, supported by vuetify 'v-icon' component (https://vuetifyjs.com/en/components/icons/)
  // if it should be custom icon that isn't supported by vuetify iconClass instead of iconName should be used
  iconName : "callDropdown",
  // appClass is a class of container which cosist of action button and WebConferencingCall component
  appClass : "webConferencingCallDropdown",
  // component has property which will be passed to dynamic component inside parent
  // (https://vuejs.org/v2/guide/components.html#Dynamic-Components)
  component : {
    // name should be the name registered via Vue.component (https://vuejs.org/v2/guide/components-registration.html#Component-Names)
    name : "call-component",
    // events are passed to custom DynamicEvents directive (https://vuejs.org/v2/guide/custom-directive.html)
    events : []
  },
  // enabled just show that this extension is enabled, if enabled: false WebConferencingCallComponent will not appear on page
  enabled : true
}, {
  target : "space-menu",
  // configuration defined here is used in exo-addons\web-conferencing\webapp\src\main\webapp\vue-apps
  // \webConferencingCall\components\CallDropdown.vue with
  // exo-addons\chat-application\application\src\main\webapp\vue-app\components\ExoChatRoomDetail.vue and connects them
  // key should be unique and used in parent component as a ref to WebConferencingCall component
  key : "callNavigation",
  rank : 21,
  // iconName is a name of the icon which is displayed on action button with 'onExecute' action
  // iconName should be one of the names, supported by vuetify 'v-icon' component (https://vuetifyjs.com/en/components/icons/)
  // if it should be custom icon that isn't supported by vuetify iconClass instead of iconName should be used
  iconName : "callDropdown",
  // appClass is a class of container which cosist of action button and WebConferencingCall component
  appClass : "webConferencingCallDropdown",
  // component has property which will be passed to dynamic component inside parent
  // (https://vuejs.org/v2/guide/components.html#Dynamic-Components)
  component : {
    // name should be the name registered via Vue.component (https://vuejs.org/v2/guide/components-registration.html#Component-Names)
    name : "call-component",
    // events are passed to custom DynamicEvents directive (https://vuejs.org/v2/guide/custom-directive.html)
    events : []
  },
  // enabled just show that this extension is enabled, if enabled: false WebConferencingCallComponent will not appear on page
  enabled : true
}];

require(["SHARED/extensionRegistry", "SHARED/webConferencingCallButton"], function (extensionRegistry, webConferencingCallButton) {
  // init app only once with registering cloud drive extension
  var settings = {};
  webConferencingCallButton.init(settings);
  for (const extension of WebConferencingCallPlugin) {
    // connect extension to AttachmentsComposer, "attachments-composer-action" is extension type
    // composer and extension type should be the same as in extension.js inside ecm-wcm-extension
    extensionRegistry.registerExtension(extension.target, extension.component.name, extension);
  }

});