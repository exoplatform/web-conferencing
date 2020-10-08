<template>
  <div id="call-button-container" ref="callbutton">
    <dropdown
      v-if="providersButton.length > 1"
      :providersbutton="providersButton"
      :isopen="isOpen"
      :createbuttons="createButtons"
      :placeholder="placeholder"
      @getRefs="getRef($event)"
      @openDropdown="openDropdown($event)"/>
  </div>
</template>
 <!-- @setSelectedProvider="setNewSelectedProvider($event)" -->
<script>
import dropdown from "./Dropdown.vue";

let vm = null;
export default {
  components: {
    dropdown
  },
  props: {
    services: {
      type: Object,
      required: true
    },
    i18n: {
      type: Object,
      required: true
    },
    language: {
      type: String,
      required: true
    },
    resourceBundleName: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      callContext: null,
      providers: [],
      providersButton: [],
      error: null,
      placeholder: "Start call",
      isOpen: false,
      childRef: null
    };
  },
  async beforeMount() {
    const thevue = this;
    this.log = webConferencing.getLog("webconferencing-call-buttons");
    try {
      const context = await webConferencing.getCallContext();
      thevue.callContext = context;
      // console.log(thevue.callContext, "context");
      if (this.callContext) {
        try {
          const p = await webConferencing.getProvider("jitsi");
          this.providers.push(p);

          // To test with several providers
          // const w = await webConferencing.getProvider("webrtc");
          // this.providers.push(w);
          // //
          await this.initProvidersButton();
          this.createButtons();
        } catch (err) {
          this.log.error("jitsi error", err);
        }
      }
    } catch (err) {
      this.log.error("Error getting call context", err);
    }
  },
  methods: {
    async initProvidersButton() {
      const thevue = this;
      await Promise.all(
        thevue.providers.map(async p => {
          if (await p.isInitialized) {
            const callButton = await p.callButton(this.callContext, "vue");
            thevue.providersButton.push(callButton);
          }
        })
      );
    },

    createButtons() {
      if (this.providersButton.length) {
        for (const [index, pb] of this.providersButton.entries()) {
          if (pb instanceof Vue) {
            // add vue button
            // console.log(pb);
            if (this.providersButton.length > 1) {
              if (this.isOpen) {
                const ref = this.childRef.callbutton[index];
                vm = pb.$mount();
                ref.appendChild(vm.$el);
              }
            } else {
              const callButton = this.$refs.callbutton;
              vm = pb.$mount();
              callButton.appendChild(vm.$el);
            }
          } else {
            // add button from DOM Element

          }
        }
      }
    },
    openDropdown() {
      this.isOpen = !this.isOpen;
    },
    getRef(ref) {
      // console.log(ref);
      this.childRef = ref;
    }
  }
};
</script>
<style scoped lang="less">
  @width: 100px;
  #call-button-container {
    position: absolute;
    left: @width + 60px;
    top: 2px;
    width: @width;
    // background-color: transparent;
    // border: 1px solid rgb(232, 238, 242);
    // border-radius: 3px;
  }
  [class^="uiIcon"] {
    font-size: 12px;
  }
</style>