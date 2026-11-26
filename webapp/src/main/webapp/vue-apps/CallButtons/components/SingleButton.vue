<template>
  <!-- in a person's menu, the container is the call entry: the provider's controls inside it are hidden from the keyboard and the screen reader -->
  <div
    ref="singlebtn"
    :role="isMenu ? 'menuitem' : null"
    :tabindex="isMenu ? 0 : null"
    :aria-label="isMenu ? providerLabel || callHeaderLabel : null"
    class="single-btn-container"
    @click="selectProvider"
    @keydown.enter="startCallFromKeyboard"
    @keydown.space="startCallFromKeyboard">
  </div>
</template>

<script>
export default {
  props: {
    isMenu: {
      type: Boolean,
      default: false
    }
  },
  data: () => ({
    providerLabel: null,
    labelObserver: null,
  }),
  computed: {
    callHeaderLabel() {
      return this.$t('webconferencing.callHeader') ? this.$i18n.t('webconferencing.callHeader') : 'Start Call';
    },
  },
  beforeDestroy() {
    if (this.labelObserver) {
      this.labelObserver.disconnect();
    }
  },
  methods: {
    selectProvider(event) {
      if (event && event.target && event.target.children[0]) {
        event.target.children[0].click(); // fire click on the provider button
      }
    },
    hideProviderControls() {
      if (!this.isMenu) {
        return;
      }
      [...this.$refs.singlebtn.children].forEach(child => child.setAttribute('aria-hidden', 'true'));
      this.$refs.singlebtn.querySelectorAll('button, a[href], [tabindex]').forEach(control => control.setAttribute('tabindex', '-1'));
      this.followProviderLabel();
    },
    // The entry takes the name of the provider's control it stands for, which names the call's state (start, join, in
    // progress) and changes with it; a control with no name of its own leaves the generic one
    followProviderLabel() {
      const control = this.$refs.singlebtn.querySelector('button, a[href]');
      if (!control) {
        return;
      }
      const readLabel = () => this.providerLabel = control.getAttribute('aria-label') || null;
      readLabel();
      if (this.labelObserver) {
        this.labelObserver.disconnect();
      }
      this.labelObserver = new MutationObserver(readLabel);
      this.labelObserver.observe(control, {attributes: true, attributeFilter: ['aria-label']});
    },
    startCallFromKeyboard(event) {
      if (!this.isMenu || event.target !== this.$refs.singlebtn) {
        return;
      }
      event.preventDefault();
      event.stopPropagation();
      // the provider's own button starts the call, and its click closes the menu holding this entry
      const control = this.$refs.singlebtn.querySelector('button, a[href]');
      if (control) {
        control.click();
      }
    }
  }
};
</script>

<style>
</style>
