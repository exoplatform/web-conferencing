const EVENT_ROOM_SELECTION_CHANGED = "exo-chat-selected-contact-changed";
let userProfilePopupButton;
let userProfileButton;

const webconferencingExts = [
  {
    target: "chat",
    type: "room-action-component",
    // configuration defined here is used in exo-addons\web-conferencing\webapp\src\main\webapp\vue-apps
    // \Call\components\CallButtons.vue with
    // exo-addons\chat-application\application\src\main\webapp\vue-app\components\ExoChatRoomDetail.vue and connects them
    // key should be unique and used in parent component as a ref to WebConferencingCall component
    key: "chatCallButton",
    rank: 20,
    // iconName is a name of the icon which is displayed on action button with 'onExecute' action
    // iconName should be one of the names, supported by vuetify 'v-icon' component (https://vuetifyjs.com/en/components/icons/)
    // if it should be custom icon that isn't supported by vuetify iconClass instead of iconName should be used
    iconName: "callButton",
    // appClass is a class of container which consist of action button and WebConferencingCall component
    appClass: "call-button",
    typeClass: "call-button--chat",
    // component has property which will be passed to dynamic component inside parent
    // (https://vuejs.org/v2/guide/components.html#Dynamic-Components)
    component: {
      // name should be the name registered via Vue.component (https://vuejs.org/v2/guide/components-registration.html#Component-Names)
      name: "call-button",
      // events are passed to custom DynamicEvents directive (https://vuejs.org/v2/guide/custom-directive.html)
      events: [],
      // props: {
      //   className: this.appClass
      // }
    },
    // init call button context in chat
    init: function(target, chat) {
      const typeClass = this.typeClass
      require([
        "SHARED/webConferencing",
        "SHARED/webConferencingCallButton",
      ], function(webConferencing, callButtons) {
        webConferencing.createChatContext(chat).then((context) => {
          callButtons.create(context, target, typeClass).then((button) => {
            // context.details().then(contex => console.log(contex, "context"));
            document.addEventListener(EVENT_ROOM_SELECTION_CHANGED, function(target) {
              webConferencing.createChatContext(chat, target).then((context) => {
                  button.update(context);
                });
            });
          });
        });
      });
    },
    // enabled just show that this extension is enabled, if enabled: false WebConferencingCallComponent will not appear on page
    enabled: true,
  },
  {
    target: "chat",
    type: "mini-chat-title-action-component",
    // configuration defined here is used in exo-addons\web-conferencing\webapp\src\main\webapp\vue-apps
    // \Call\components\CallButtons.vue with
    // exo-addons\chat-application\application\src\main\webapp\vue-app\components\modal\ExoChatDrawer.vue and connects them
    // key should be unique and used in parent component as a ref to WebConferencingCall component
    key: "miniChatCallButton",
    rank: 21,
    // iconName is a name of the icon which is displayed on action button with 'onExecute' action
    // iconName should be one of the names, supported by vuetify 'v-icon' component (https://vuetifyjs.com/en/components/icons/)
    // if it should be custom icon that isn't supported by vuetify iconClass instead of iconName should be used
    iconName: "callButton",
    // appClass is a class of container which consist of action button and WebConferencingCall component
    appClass: "call-button",
    typeClass: "call-button-mini call-button--chat-drawer",
    // component has property which will be passed to dynamic component inside parent
    // (https://vuejs.org/v2/guide/components.html#Dynamic-Components)
    component: {
      // name should be the name registered via Vue.component (https://vuejs.org/v2/guide/components-registration.html#Component-Names)
      name: "call-button",
      // events are passed to custom DynamicEvents directive (https://vuejs.org/v2/guide/custom-directive.html)
      events: [],
    },
    // init call button context in mini chat
    init: function(target, chat) {
      const typeClass = this.typeClass
      require([
        "SHARED/webConferencing",
        "SHARED/webConferencingCallButton",
      ], function(webConferencing, callButtons) {
        if (!(eXo.env.portal.selectedNodeUri === "chat")) {
          // don't init in chat
          webConferencing.createChatContext(chat).then((context) => {
            callButtons.create(context, target, typeClass).then((button) => {
              document.addEventListener(EVENT_ROOM_SELECTION_CHANGED, function(target) {
                webConferencing.createChatContext(chat, target).then((context) => {
                    button.update(context);
                  });
              });
            });
          });
        }
      });
    },
    // enabled just show that this extension is enabled, if enabled: false WebConferencingCallComponent will not appear on page
    enabled: true,
  },
  {
    target: "space-menu",
    type: "action-component",
    // configuration defined here is used in exo-addons\web-conferencing\webapp\src\main\webapp\vue-apps
    // \Call\components\CallButtons.vue with
    // exo-addons\chat-application\application\src\main\webapp\vue-app\components\ExoChatRoomDetail.vue and connects them
    // key should be unique and used in parent component as a ref to WebConferencingCall component
    key: "spaceMenuCallButton",
    rank: 22,
    // iconName is a name of the icon which is displayed on action button with 'onExecute' action
    // iconName should be one of the names, supported by vuetify 'v-icon' component (https://vuetifyjs.com/en/components/icons/)
    // if it should be custom icon that isn't supported by vuetify iconClass instead of iconName should be used
    iconName: "callButton",
    // appClass is a class of container which consist of action button and WebConferencingCall component
    appClass: "call-button",
    typeClass: "call-button--space",
    // component has property which will be passed to dynamic component inside parent
    // (https://vuejs.org/v2/guide/components.html#Dynamic-Components)
    component: {
      // name should be the name registered via Vue.component (https://vuejs.org/v2/guide/components-registration.html#Component-Names)
      name: "call-button",
      // events are passed to custom DynamicEvents directive (https://vuejs.org/v2/guide/custom-directive.html)
      events: [],
    },
    // init call button context in space
    init: function(target, spaceId) {
      const typeClass = this.typeClass
      require([
        "SHARED/webConferencing",
        "SHARED/webConferencingCallButton",
      ], function(webConferencing, callButtons) {
        webConferencing.createSpaceContext(spaceId).then((context) => {
          
          callButtons.create(context, target, typeClass).then((button) => {
            button.update(context);  // don't need
          });
        });
      });
    },
    // enabled just show that this extension is enabled, if enabled: false WebConferencingCallComponent will not appear on page
    enabled: true,
  },
  {
    target: "user-profile-popup",
    type: "exo-social-user-popup-component",
    // configuration defined here is used in exo-addons\web-conferencing\webapp\src\main\webapp\vue-apps
    // \Call\components\CallButtons.vue with
    // social\webapp\resources\src\main\webapp\javascript\eXo\UIUserProfilePopup.js and connects them
    // key should be unique and used in parent component as a ref to WebConferencingCall component
    key: "userProfilePopupCallButton",
    rank: 23,
    iconName: "callButton",
    appClass: "call-button",
    typeClass: "call-button-mini call-button--tiptip",
    component: {
      name: "call-button",
      events: [],
    },
    // init call button context in user profile popup
    init: function(target, userId) {
      const typeClass = this.typeClass
      require([
        "SHARED/webConferencing",
        "SHARED/webConferencingCallButton",
      ], function(webConferencing, callButtons) {
        if (userProfilePopupButton) {
          // TO DO delete old button
          //something like userProfilePopupButton.destroy();
        }
        webConferencing.createUserContext(userId).then((context) => {
          callButtons.create(context, target, typeClass).then((button) => {
            userProfilePopupButton = button;
            //button.update(context);  // don't need
          });
        });
      });
    },
    // enabled just show that this extension is enabled, if enabled: false WebConferencingCallComponent will not appear on page
    enabled: true,
  },
  {
    target : "profile-header",
    type : "action-component",
    // configuration defined here is used in exo-addons\web-conferencing\webapp\src\main\webapp\vue-apps
    // \Call\components\CallButtons.vue with
    // social\webapp\portlet\src\main\webapp\profile-header\components\ProfileHeaderActions.vue and connects them
    // key should be unique and used in parent component as a ref to WebConferencingCall component
    key : "userProfileCallButton",
    rank : 24,
    iconName : "callButton",
    appClass : "call-button",
    typeClass: "call-button--profile",
    component : {
      name : "call-button",
      events : [],
    },
    // init call button context in user profile
    init : function (target, userId) {
      const typeClass = this.typeClass
      require([
        "SHARED/webConferencing",
        "SHARED/webConferencingCallButton",
      ], function (webConferencing, callButtons) {
        if (userProfileButton) {
          // TO DO delete old button
          //something like userProfileButton.destroy();
        }

        webConferencing.createUserContext(userId).then((context) => {
          callButtons.create(context, target, typeClass).then((button) => {
            userProfileButton = button;
          });
        });
      });
    },
    // enabled just show that this extension is enabled, if enabled: false WebConferencingCallComponent will not appear on page
    enabled : true,
  }
  /*
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
}*/,
];

require(["SHARED/extensionRegistry"], function(extensionRegistry) {
  for (const extension of webconferencingExts) {
    extensionRegistry.registerExtension(
      extension.target,
      extension.type,
      extension
    );
  }
});
