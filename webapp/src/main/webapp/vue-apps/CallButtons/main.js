Vue.config.devtools = true

import callButtons from "./components/CallButtons.vue";


import Vuex from "vuex";
Vue.use(Vuex);
Vue.use(Vuetify);

 export const store = new Vuex.Store({
   state: {
     callContext: {
        // "app": {},
        // "space": {},
        // "mini": {},
        // "popup": {},
        "isUser": {},
        "isGroup": {},
     },
     mini: false,
     location: ""
   },
   mutations: {
     initButton(state, payload) {
       if (state.location === "isUser") {
        state.callContext.isUser = payload.context
       }
       if (state.location === "isGroup") {
         state.callContext.isGroup = payload.context
       }
      //   state.callContext[payload.location] = payload.context;
     },
     toggleMini(state, condition) {
       state.mini = condition ?  true : false;
     },
     defineLocation(state, location) {
      state.location = location ? "isUser" : "isGroup"
     }
   },
   actions: {
   },
 });
const comp = Vue.component("call-button", callButtons);
// const comp = Vue.component("call-button", () => import("./components/CallButtons.vue"));
const vuetify = new Vuetify({
  dark: true,
  iconfont: "",
});

// getting language of user
const lang =
  (eXo && eXo.env && eXo.env.portal && eXo.env.portal.language) || "en";
const localePortlet = "locale.webconferencing";
const resourceBundleName = "WebConferencingClient";
const url = `${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/${localePortlet}.${resourceBundleName}-${lang}.json`;
const log = webConferencing.getLog("webconferencing-call-buttons");

export function create(context, target) {
  this.store.commit("defineLocation", context.isUser)
  this.store.commit("initButton", {context});
  const result = new Promise((resolve, reject) => {
    if (target) {
      exoi18n.loadLanguageAsync(lang, url).then((i18n) => {
        // if (this.store.state.callContext[loc] && JSON.stringify(this.store.state.callContext[loc]) !== JSON.stringify(context)) {
        // }
        const vmComp = new Vue({
          el: target,
          store: store,
          render: function(h) {
            return h(
              callButtons,
              {
                props: {
                  i18n,
                  language: lang,
                  resourceBundleName,
                },
              },
              i18n,
              vuetify
            );
          },
        });
        resolve({
          update: function(context) {
            // Vue.set(vmComp, "loc", context.isUser ? "isUser" : "isGroup")
            store.commit("defineLocation", context.isUser)
            store.commit("initButton", {context});
          },
        });
        
      });
    } else {
      log.error("Error getting the extension container");
      reject(new Error("Error getting the extension container"));
    }
  });

  return result;
}