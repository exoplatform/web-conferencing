<template>
  <div id="dropdown-vue">
    <dropdown-header
      :header="header"
      :showdropdowncomponent="showDropdownComponent"
      :passrefs="passRefs"/>
    <div v-if="isopen" class="buttons-container">
      <!-- TODO why we need IDs for them?? a class will not work? -->
      <div
        v-for="(button, index) in providersbutton"
        :key="index"
        :class="`call-button-container-${index}`"
        :ref="`callbutton`"></div>
    </div>
  </div>
</template>

<script>
import DropdownHeader from "./DropdownHeader.vue";

export default {
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
      type: String,
      required: true
    }
  },
  updated() {
    this.$emit("updated")
  },
  methods: {
    showDropdownComponent(isOpen) {
      this.$emit("showDropdown", isOpen);
    },
    passRefs() {
      this.$emit("getRefs", this.$refs);
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
  width: @width + 30px;
  box-shadow: @defaultShadow;
  position: absolute;
  top: 38px;
  left: 0;
  [class^="call-button-container-"] {
    padding: 0 10px;
    height: 36px;
    border-radius: 3px;
    &:hover {
      background-color: @primaryColor;
    }
  }
}
</style>