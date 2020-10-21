import callButtons from "./components/CallButtons.vue";

Vue.use(Vuetify);
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
  const result = new Promise((resolve, reject) => {
    if (extensionContainer && extensionContainer.length > 0) {
      exoi18n.loadLanguageAsync(lang, url).then((i18n) => {
        const vmComp = new Vue({
          el : extensionContainer[0],
          components : {
            "call-button" : callButtons
          },
          render : (h) => {
            return h(callButtons, {
                props : {callContext : context, i18n, language : lang, resourceBundleName},
              },
              i18n,
              vuetify
            );
          },
        });

        resolve({
          update : function (context) {
            vmComp._vnode.data.props.callContext = context;
          }
        });
      });
    } else {
      log.error("Error getting the extension container");
    }
  });

  return result;
}
