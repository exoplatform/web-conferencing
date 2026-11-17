<template>
  <!-- in a person's menu, the container is the call entry: the provider's controls inside it are hidden from the keyboard and the screen reader -->
  <div
    ref="singlebtn"
    :role="isMenu ? 'menuitem' : null"
    :tabindex="isMenu ? 0 : null"
    :aria-label="isMenu ? callHeaderLabel : null"
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
  computed: {
    callHeaderLabel() {
      return this.$t('webconferencing.callHeader') ? this.$i18n.t('webconferencing.callHeader') : 'Start Call';
    },
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
