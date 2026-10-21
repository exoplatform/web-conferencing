<template>
  <div class="dropdown-vue">
    <dropdown-header
      ref="buttonsheader"
      :header="header"
      :showdropdowncomponent="showDropdownComponent"
      :passrefs="passRefs"
      :start-call="startCall" />
    <!-- The buttons can be rendered inside a link, such as a user card: no click in them may reach it -->
    <div 
      v-show="isopen" 
      ref="buttonsContainer" 
      :class="positionclass" 
      class="buttons-container"
      @click.stop.prevent
      @mouseleave="showDropdownComponent">
      <!-- TODO why we need IDs for them?? a class will not work? -->
      <div
        v-for="(button, index) in providersbutton"
        :key="index"
        :class="`call-button-container-${index} me-2`"
        :ref="`callbutton`"
        @click="selectProvider(index)"></div>
    </div>
  </div>
</template>

<script>
import DropdownHeader from './DropdownHeader.vue';

export default {
  name: 'Dropdown',
  components: {
    DropdownHeader
  },
  props: {
    providersbutton: {
      type: Array,
      required: true
    },
    isopen: {
      type: Boolean,
      required: true
    },
    header: {
      type: Object,
      required: true
    },
    positionclass: {
      type: String,
      required: true
    }
  },
  watch: {
    async isopen(value) {
      if (value) {
        await this.$nextTick();
        this.$emit('dropdownIsVisualized');
      }
    }
  },
  updated() {
    this.$emit('updated');
  },
  methods: {
    showDropdownComponent() {
      this.$emit('showDropdown');
    },
    startCall() {
      this.providersbutton[0].callSettings.onCallOpen();
    },
    passRefs() {
      this.$emit('getRefs', this.$refs);
    },
    selectProvider(index) {
      // The provider's own button stops its click, so this one comes from the rest of its line
      const callSettings = this.providersbutton[index] && this.providersbutton[index].callSettings;
      if (callSettings && callSettings.onCallOpen) {
        callSettings.onCallOpen();
      }
      this.$emit('selectedProvider');
    }
  }
};
</script>

<style scoped lang="less">
@import "../../../skin/less/variables.less";

.buttons-container {
  background-color: white;
  border: @defaultBorder;
  border-radius: 3px;
  margin-top: 3px;
  min-width: @width;
  max-width: @width + @width;
  box-shadow: @defaultShadow;
  position: fixed;
  [class^="call-button-container-"] {
    height: 36px;
    border-radius: 3px;
    display: flex;
    align-items: center;
    &:hover {
      background-position: 0 -45px;
      background-color: @primaryColor;
    }
  }
}
</style>
