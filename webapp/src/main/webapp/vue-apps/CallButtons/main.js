import callButtons from "./components/CallButtons.vue";

Vue.use(Vuetify);
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

export function create(context, extensionContainer) {
  const callContext = context;
  const result = new Promise((resolve, reject) => {
    exoi18n.loadLanguageAsync(lang, url).then((i18n) => {
      const comp = Vue.component("call-button", {
        render : function (createElement) {
          return createElement(
            callButtons,
            {props : {...callContext, i18n, language : lang, resourceBundleName}} // tag name
          );
        },
      });
      const vmComp = new Vue({
        el : extensionContainer,
        comp,
        render : (h) => {
          return h(comp, {
              props : {...callContext, i18n, language : lang, resourceBundleName},
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
  });

  return result;
}
