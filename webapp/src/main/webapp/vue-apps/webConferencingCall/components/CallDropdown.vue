<template>
  <div id="webConferencingCallDropdown">
    <button>Jitsi</button>
    <ul>
      <li v-for="b in providersButton" :key="b">
        {{ b }}
      </li>
    </ul>
  </div>
</template>

<script>

export default {
  components: {
  },
  props: {
    callContext: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      providers: [],
      providersButton: [],
      error: null
    };
  },
  async created() {
    const thevue = this;
    await webConferencing.getProvider("jitsi").done(jitsi => thevue.providers.push(jitsi)).fail(function(err) {
      console.log(`jitsi error: ${err}`);
    });
    console.log(`providers stringify: ${JSON.stringify(this.providers)}`);
    console.log(`jitsi supportedTypes: ${(this.providers.length)?this.providers[0].getSupportedTypes():"-"}`)

    this.initProvidersButton();
    console.log(`providers buttons: ${JSON.stringify(this.providersButton)}`);
    console.log(`context: ${JSON.stringify(this.callContext)}`);

  },
  methods: {
    initProvidersButton() {
      for(const p in this.providers) {
        if (p.isInitialized) {
          const b = p.callButton(this.callContext);
          this.providersButton.push(b);
        }
      }
    }
  }
};
</script>

<style scoped lang="less">

</style>