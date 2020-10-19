// import Vuex from "vuex";
import callButtons from "./components/CallButtons.vue";

Vue.use(Vuetify);
// Vue.use(Vuex);
// console.log(Vuex)
const vm = Vue.component("call-button", callButtons);
const vuetify = new Vuetify({
  dark: true,
  iconfont: ""
});

// getting language of user
const lang = (eXo && eXo.env && eXo.env.portal && eXo.env.portal.language) || "en";
const localePortlet = "locale.webconferencing";
const resourceBundleName = "WebConferencingClient";
const url = `${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/${localePortlet}.${resourceBundleName}-${lang}.json`;

export function init(settings) {
  // getting locale ressources

  exoi18n.loadLanguageAsync(lang, url).then(i18n => {
    // init Vue app when locale ressources are ready
    // console.log(settings.detail.type, "gfdghjkl")
     //if (settings.detail.type && settings.detail.type === "u") {
       new Vue({
         render: function(h) {
           return h(callButtons, { props: {...settings, i18n, language: lang, resourceBundleName} })},
         i18n,
         vuetify
       }) 
    //}
  })
  // TODO return actually created Vue component here via promise or directly
  // return theCallButtons;
}