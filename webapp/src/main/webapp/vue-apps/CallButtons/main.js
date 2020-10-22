import callButtons from "./components/CallButtons.vue";

import Vuex from "vuex";
Vue.use(Vuex);
Vue.use(Vuetify);

export const store = new Vuex.Store({
  state: {
    callContext: {},
  },
  mutations: {
    switchRoom(state, context) {
      state.callContext = context;
    },
  },
  actions: {
  },
});
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
  store.commit("switchRoom", context);
  const result = new Promise((resolve, reject) => {
    if (target && target.length > 0) {
      exoi18n.loadLanguageAsync(lang, url).then((i18n) => {
        const vmComp = new Vue({
          el: target[0],
          store: store,
          components: {
            "call-button": callButtons
          },
          render: function(h) {
            return h(
              callButtons,
              {
                props: {
                  // callContext: store.state.callContext,
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
            store.commit("switchRoom", context);
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