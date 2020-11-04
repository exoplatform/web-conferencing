Vue.config.devtools = true;

import CallButtons from "./components/CallButtons.vue";
Vue.use(Vuetify);
const vuetify = new Vuetify({
  dark: true,
  iconfont: "",
});


Vue.directive("click-outside", {
  priority: 700,
  bind: function(el, binding, vnode) {
    el.clickOutside = function(e) {
      if(!(el === e.target || el.contains(e.target))) {
        vnode.context[binding.expression](e);
      }
    }
    document.body.addEventListener("click", el.clickOutside);
  },
  unbind: function(el) {
    document.body.removeEventListener("click", el.clickOutside);
  },
});
// getting language of user
const lang = (eXo && eXo.env && eXo.env.portal && eXo.env.portal.language) || "en";
const localePortlet = "locale.webconferencing";
const resourceBundleName = "WebConferencingClient";
const url = `${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/${localePortlet}.${resourceBundleName}-${lang}.json`;
const log = webConferencing.getLog("webconferencing-call-buttons");

export function create(context, target) {
  const result = new Promise((resolve, reject) => {
    if (target) {
      if (target.classList.length) {
        target.appendChild(document.createElement("div"));// div for vue mounting
        target = target.firstElementChild;
      }

      exoi18n.loadLanguageAsync(lang, url).then((i18n) => {
        const vmComp = new Vue({
          el: target,
          components: {
            CallButtons,
          },
          data() {
            return {
              language: lang,
              resourceBundleName,
              callContext: {}
            }
          },
          mounted() {
            this.setCallContext(context, this);
          },
          methods: {
            setCallContext(context, vmcomp) {
              this.$set(vmcomp, "callContext", context);
            }
          },
          i18n,
          vuetify,  
          render: function(h) {
            return h(
              CallButtons,
              {
                props: {
                 language: lang,
                  resourceBundleName,
                  callContext: this.callContext
                },
                on: {
                  created: function() {
                    log.trace("Handle cull buttons creation: resolve");
                    resolve({
                      vm: vmComp,
                      update: function(context) {
                        vmComp.setCallContext(context, vmComp);
                      },
                      getElement: function() {
                        return this.vm.$el;
                      },
                    });
                  }
                }
              }
            );
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