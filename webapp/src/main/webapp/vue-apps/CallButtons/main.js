Vue.config.devtools = true;

import CallButtons from './components/CallButtons.vue';
Vue.use(Vuetify);
const vuetify = new Vuetify(eXo.env.portal.vuetifyPreset);

// getting language of user
const lang = (eXo && eXo.env && eXo.env.portal && eXo.env.portal.language) || 'en';
const localePortlet = 'locale.webconferencing';
const resourceBundleName = 'WebConferencingClient';
const url = `${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/${localePortlet}.${resourceBundleName}-${lang}.json`;
const log = webConferencing.getLog('webconferencing');

export function create(context, target) {
  const result = new Promise((resolve, reject) => {
    if (target) {
      const mountEl = document.createElement('div'); // div for vue mounting
      // we need the target as the parent container to use that classes for call button settings
      target.appendChild(mountEl);

      exoi18n.loadLanguageAsync(lang, url).then((i18n) => {
        const comp = new Vue({
          el: mountEl,
          components: {
            CallButtons
          },
          data() {
            return {
              language: lang,
              resourceBundleName,
              callContext: {}
            };
          },
          mounted() {
            this.setCallContext(context);
          },
          methods: {
            setCallContext(context) {
              Vue.set(this, 'callContext', context);
            }
          },
          i18n,
          vuetify,
          render: function(h) {
            return h(CallButtons, {
              props: {
                language: lang,
                resourceBundleName,
                callContext: this.callContext,
              },
              on: {
                created: function() {
                  resolve({
                    vm: comp,
                    update: function(context) {
                      const targetId = context.isUser ? context.userId : context.isSpace ? context.spaceId : context.isRoom ? context.roomName : null;
                      if (targetId) {
                        log.trace(`>> update target: ${  targetId}`);
                      } else {
                        log.trace(`>> update >> cannot find target from context: ${  JSON.stringify(context)}`);
                      }
                      comp.setCallContext(context);
                    },
                    getElement: function() {
                      return this.vm.$el;
                    }
                  });
                }
              }
            });
          }
        });
      }).finally(() => {
        Vue.prototype.$utils.includeExtensions('VisioConnector');
      });
    } else {
      const log = webConferencing.getLog('webconferencing');
      log.error('Error getting the extension container');
      reject(new Error('Error getting the extension container'));
    }
  });

  return result;
}
