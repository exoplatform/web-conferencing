<template>
  <div id="dropdown-vue">
    <dropdown-header
      ref="buttonsheader"
      :header="header"
      :showdropdowncomponent="showDropdownComponent"
      :passrefs="passRefs"/>
    <div 
      v-show="isopen" 
      :class="positionclass" 
      class="buttons-container">
      <!-- TODO why we need IDs for them?? a class will not work? -->
      <div
        v-for="(button, index) in providersbutton"
        :key="index"
        :class="`call-button-container-${index}`"
        :ref="`callbutton`"
        @click="isopen=false"></div>
    </div>
  </div>
</template>

<script>
import DropdownHeader from "./DropdownHeader.vue";
import VueDraggableResizable from "vue-draggable-resizable";

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
      type: Object,
      required: true
    },
    positionclass: {
      type: String,
      required: true
    }
  },
  updated() {
    this.$emit("updated");
  },
  methods: {
    showDropdownComponent() {
      this.$emit("showDropdown");
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
  &.left {
    right: -3px;
  }
  &.right {
    left: 20px;
  }
  [class^="call-button-container-"] {
    padding: 0 10px;
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