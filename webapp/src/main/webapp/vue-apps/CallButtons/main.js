import callButtons from "./components/CallButtons.vue";

import Vuex from "vuex";
Vue.use(Vuex);
Vue.use(Vuetify);

export const store = new Vuex.Store({
  state: {
    // count: 0,
    callContext: {},
    // providersButton: []
  }, 
  mutations: {
    // increment(state) {
    //   state.count++
    // },
    switchRoom(state, context) {
      state.callContext = context
    }
  },
  actions: {
    // increment(context) {
    //   context.commit("increment")
    // },
    // createButtons(context) {
    //   context.commit()
    // },
    // setProvidersButtons(context, {callContext}) {
    //   if (callContext && callContext.details) {
    //     const callButtons = [];
    //     webConferencing.getAllProviders().then(providers => {
    //       providers.map(provider => {
    //         callButtons.push(provider.callButton(callContext));
    //       });
    //       Promise.allSettled(callButtons).then(resCallButtons => {
    //         resCallButtons.forEach((button) => {
    //           if (button.status === "fulfilled") {
    //             context.state.providersButton.push(button.value);
    //           }
    //         });
    //         callButtons.methods.createButtons();
    //       });
    //     });
    //   }
    // }
  }
})
// store.dispatch("increment")
const comp = Vue.component("call-button", callButtons);
const vuetify = new Vuetify({
  dark : true,
  iconfont : "",
});

// getting language of user
const lang =
  (eXo && eXo.env && eXo.env.portal && eXo.env.portal.language) || "en";
const localePortlet = "locale.webconferencing";
const resourceBundleName = "WebConferencingClient";
const url = `${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/${localePortlet}.${resourceBundleName}-${lang}.json`;
const log = webConferencing.getLog("webconferencing-call-buttons");

export function create(context, extensionContainer) {
  // const callContext = context;
  const result = new Promise((resolve, reject) => {
    if (extensionContainer && extensionContainer.length > 0) {
        exoi18n.loadLanguageAsync(lang, url).then((i18n) => {
        const vmComp =  new Vue({
          el : extensionContainer[0],
          store: store,
          // props: {
          //   callContext: {
          //     type: Object,
          //     default: context
          //   }},
          render : function (h) {
            return h(callButtons, {
                props : {callContext: context, i18n, language : lang, resourceBundleName},
              },
              i18n,
              vuetify
            );
          },
        });
        resolve({
          update : function (context) {
            // vmComp.callContext = context
            // vmComp.$options.callContxt = context
            // vmComp.$props.callContext = context
          }
        });
      });
      // resolve({
      //   update : function (context) {
      //     vmComp.then(vm => 
      //     vm._vnode.data.props.callContext = context)
      //   }
      // });
    } else {
      log.error("Error getting the extension container");
      reject(new Error("Error getting the extension container"));
    }
  });

  return result;
}