import callButtons from "./components/CallButtons.vue";

import Vuex from "vuex";
Vue.use(Vuex);
Vue.use(Vuetify);

 export const store = new Vuex.Store({
   state: {
     callContext: {
        "app": {},
        "space": {},
        "mini": {},
        "popup": {}
     },
     mini: false
   },
   mutations: {
     initRoom(state, payload) {
        state.callContext[payload.location] = payload.context;
     },
     switchRoom(state, payload) {
        state.callContext[payload.location] = payload.context;
     },
     toggleMini(state, condition) {
       state.mini = condition ?  true : false;
     }
   },
   actions: {
   },
 });
const comp = Vue.component("call-button", callButtons);
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

export function create(context, target, loc) {
  // console.log(this.store.state)
  this.store.commit("initRoom", {context, location: loc});
  const result = new Promise((resolve, reject) => {
    if (target) {
      exoi18n.loadLanguageAsync(lang, url).then((i18n) => {
        // if (this.store.state.callContext[loc] && JSON.stringify(this.store.state.callContext[loc]) !== JSON.stringify(context)) {
        //   // console.log(this.store.state.callContext[loc] === context)
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
                  loc: loc
                },
              },
              i18n,
              vuetify
            );
          },
        });
        resolve({
          update: function(context) {
            store.commit("switchRoom", {context, location: loc});
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