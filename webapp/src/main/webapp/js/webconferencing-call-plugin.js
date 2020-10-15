const webconferencingExts = [{
  target : "chat",
  type : "room-action-component",
  // configuration defined here is used in exo-addons\web-conferencing\webapp\src\main\webapp\vue-apps
  // \Call\components\CallButtons.vue with
  // exo-addons\chat-application\application\src\main\webapp\vue-app\components\ExoChatRoomDetail.vue and connects them
  // key should be unique and used in parent component as a ref to WebConferencingCall component
  key : "callButton",
  rank : 20,
  // iconName is a name of the icon which is displayed on action button with 'onExecute' action
  // iconName should be one of the names, supported by vuetify 'v-icon' component (https://vuetifyjs.com/en/components/icons/)
  // if it should be custom icon that isn't supported by vuetify iconClass instead of iconName should be used
  iconName : "callButton",
  // appClass is a class of container which consist of action button and WebConferencingCall component
  appClass : "webconferencingCallButton",
  // component has property which will be passed to dynamic component inside parent
  // (https://vuejs.org/v2/guide/components.html#Dynamic-Components)
  component : {
    // name should be the name registered via Vue.component (https://vuejs.org/v2/guide/components-registration.html#Component-Names)
    name : "call-button",
    // events are passed to custom DynamicEvents directive (https://vuejs.org/v2/guide/custom-directive.html)
    events : []
  },
  // init call button context in chat
  init : function (chat) {
    require(["SHARED/webConferencing", "SHARED/webConferencingCallButton"], function (webConferencing, webConferencingCallButton) {
      webConferencing.initChatContext(chat);
      //var settings = {}; // TODO not good as we cheat ourselves by this here!
      // We know we are in the chat here, we can rely on its events and build specific logic  
      document.addEventListener(EVENT_ROOM_SELECTION_CHANGED, function(target) {
        // TODO add all buttons from here
        //addButton(target);
        //webConferencingCallButton.destroy();
        webConferencingCallButton.init(target);
      });
    });
  },
  // enabled just show that this extension is enabled, if enabled: false WebConferencingCallComponent will not appear on page
  enabled : true
}, {
  target : "chat",
  type : "mini-chat-title-action-component",
  // configuration defined here is used in exo-addons\web-conferencing\webapp\src\main\webapp\vue-apps
  // \Call\components\CallButtons.vue with
  // exo-addons\chat-application\application\src\main\webapp\vue-app\components\modal\ExoChatDrawer.vue and connects them
  // key should be unique and used in parent component as a ref to WebConferencingCall component
  key : "callButton",
  rank : 20,
  // iconName is a name of the icon which is displayed on action button with 'onExecute' action
  // iconName should be one of the names, supported by vuetify 'v-icon' component (https://vuetifyjs.com/en/components/icons/)
  // if it should be custom icon that isn't supported by vuetify iconClass instead of iconName should be used
  iconName : "callButton",
  // appClass is a class of container which consist of action button and WebConferencingCall component
  appClass : "webConferencingCallButton", // TODO seems here we should add a MINI-class?
  // component has property which will be passed to dynamic component inside parent
  // (https://vuejs.org/v2/guide/components.html#Dynamic-Components)
  component : {
    // name should be the name registered via Vue.component (https://vuejs.org/v2/guide/components-registration.html#Component-Names)
    name : "call-button",
    // events are passed to custom DynamicEvents directive (https://vuejs.org/v2/guide/custom-directive.html)
    events : []
  },
  // init call button context in mini chat
  init : function (chat) {
    require(["SHARED/webConferencing", "SHARED/webConferencingCallButton"], function (webConferencing, webConferencingCallButton) {
      webConferencing.initChatContext(chat); // TODO should we point to a context it's a mini-chat here?
      var settings = {};
      webConferencingCallButton.init(settings); // TODO not good as we cheat ourselves by this here!
    });
  },
  // enabled just show that this extension is enabled, if enabled: false WebConferencingCallComponent will not appear on page
  enabled : true
}, {
  target : "space-menu",
  type : "room-action-component",
  // configuration defined here is used in exo-addons\web-conferencing\webapp\src\main\webapp\vue-apps
  // \Call\components\CallButtons.vue with
  // exo-addons\chat-application\application\src\main\webapp\vue-app\components\ExoChatRoomDetail.vue and connects them
  // key should be unique and used in parent component as a ref to WebConferencingCall component
  key : "callButton",
  rank : 21,
  // iconName is a name of the icon which is displayed on action button with 'onExecute' action
  // iconName should be one of the names, supported by vuetify 'v-icon' component (https://vuetifyjs.com/en/components/icons/)
  // if it should be custom icon that isn't supported by vuetify iconClass instead of iconName should be used
  iconName : "callButton",
  // appClass is a class of container which consist of action button and WebConferencingCall component
  appClass : "webConferencingCallButton",
  // component has property which will be passed to dynamic component inside parent
  // (https://vuejs.org/v2/guide/components.html#Dynamic-Components)
  component : {
    // name should be the name registered via Vue.component (https://vuejs.org/v2/guide/components-registration.html#Component-Names)
    name : "call-button",
    // events are passed to custom DynamicEvents directive (https://vuejs.org/v2/guide/custom-directive.html)
    events : []
  },
  // enabled just show that this extension is enabled, if enabled: false WebConferencingCallComponent will not appear on page
  enabled : true
} /*
  // an example of the extension with DOM elements
,{
  target : "chat",
  type : "room-action-component",
  key : "element",
  rank : 22,

  // appClass is a class of container which consist of action button and WebConferencingCall component
  appClass : "element-cl",

  // html DOM element that will be added in the extension point
  element : testElement,

  // enabled just show that this extension is enabled, if enabled: false WebConferencingCallComponent will not appear on page
  enabled : true
},
  // an example of the extension with HTML elements
{
  target : "chat",
  type : "room-action-component",
  key : "html",
  rank : 23,

  // appClass is a class of container which consist of action button and WebConferencingCall component
  appClass : "html-cl",

  // html code that will be added in the extension point
  html : "<div><span>My test html</span></div>",

  // enabled just show that this extension is enabled, if enabled: false WebConferencingCallComponent will not appear on page
  enabled : true
}*/];

require(["SHARED/extensionRegistry", "SHARED/webConferencing"], function (extensionRegistry, webConferencing) {
  const log = webConferencing.getLog("webconferencing-call-plugin");
  for (const extension of webconferencingExts) {
    extensionRegistry.registerExtension(extension.target, extension.type, extension);
    log.trace(`Register extension type of ${extension.type} for ${extension.target}`);
  }

  // TODO it's not right thing to fire such events from not a Chat app!!
  // dispatch the event about adding an extension in the chat
  //document.dispatchEvent(new CustomEvent("chat-room-action-components-updated"));
  // dispatch the event about adding an extension in the mini chat
  //document.dispatchEvent(new CustomEvent("mini-chat-title-action-components-updated"));
});