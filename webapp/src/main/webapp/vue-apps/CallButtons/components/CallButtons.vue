<template>
  <div id="call-button-container"></div>
</template>

<script>
export default {
  components: {},
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
      error: null
    };
  },
  async created() {
    const thevue = this;
    this.log = webConferencing.getLog("webconferencing-call-buttons");

    try {
      const context = await webConferencing.getCallContext();
      thevue.callContext = await context;
      if (this.callContext) {
        try {
          const p = await webConferencing.getProvider("jitsi");
          this.providers.push(p);
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
  mounted() {
    // this.createButtons();
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
      for (const pb of this.providersButton) {
        if (pb instanceof Vue) {
          // add vue button
          pb.$mount("#call-button-container");
        } else {
          // add button from DOM Element
        }
      }
    }
  }
};
</script>

<style scoped lang="less">
</style>