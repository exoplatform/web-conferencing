import './initComponents.js';
import './extensions.js';

const lang = eXo.env.portal.language;
const url = `${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/locale.portlet.notification.webconferencingNotification-${lang}.json`;

export function init() {
  return exoi18n.loadLanguageAsync(lang, url)
    .then(() => {
      Vue.createApp({
        i18n: exoi18n.i18n,
      });
    });
}