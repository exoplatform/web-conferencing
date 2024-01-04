import adminApp from './components/AdminApp.vue';
const components = {
  'admin-app': adminApp,
};

for (const key in components) {
  Vue.component(key, components[key]);
}


Vue.use(Vuetify);
const vuetify = new Vuetify(eXo.env.portal.vuetifyPreset);

// getting language of user
const lang = (eXo && eXo.env && eXo.env.portal && eXo.env.portal.language) || 'en';
const localePortlet = 'locale.webconferencing';
const resourceBundleName = 'WebConferencingAdmin';
const url = `${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/${localePortlet}.${resourceBundleName}-${lang}.json`;
const appId = 'webconferencingAdmin';

export function init() {
  // getting locale ressources
  exoi18n.loadLanguageAsync(lang, url).then(i18n => {

    // init Vue app when locale ressources are ready
    Vue.createApp({
      template: `<admin-app id="${appId}" />`,
      vuetify,
      i18n
    }, `#${appId}`, 'WebConferencingAdmin');
  }).finally(() => Vue.prototype.$utils.includeExtensions('VisioConnector'));
}
