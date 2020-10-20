import callButtons from "./components/CallButtons.vue";

Vue.use(Vuetify);
// const comp = Vue.component("call-button", callButtons);
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

export function create(context) {
  const callContext = { ...context };
  console.log(callContext, "context in main");
  const vmComp = exoi18n.loadLanguageAsync(lang, url).then((i18n) => {
    const comp = Vue.component("call-button", {
      render: function(createElement) {
        return createElement(
          callButtons,
          { props: { callContext, i18n, language: lang, resourceBundleName } } // tag name
        );
      },
    });
    // console.log(callButtons)
    const vm = new Vue({
      el: "#call-button-container",
      // data,
      comp,
      render: (h) =>
        h(callButtons, {
          props: { callContext, i18n, language: lang, resourceBundleName },
        }),
    });
    return vm;
  });
  return vmComp;
  // return {
  //   update: function(context) {
  //     vmComp.then(vm => {
  //     })
  //   }}
  // return vmComp;
}
