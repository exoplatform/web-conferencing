<template>
  <div class="dropdown-vue">
    <dropdown-header
      ref="buttonsheader"
      :header="header"
      :showdropdowncomponent="showDropdownComponent"
      :passrefs="passRefs" />
    <div 
      v-show="isopen" 
      ref="buttonsContainer" 
      :class="positionclass" 
      class="buttons-container">
      <!-- TODO why we need IDs for them?? a class will not work? -->
      <div
        v-for="(button, index) in providersbutton"
        :key="index"
        :class="`call-button-container-${index}`"
        :ref="`callbutton`"
        @click="selectProvider"></div>
    </div>
  </div>
</template>

<script>
import DropdownHeader from "./DropdownHeader.vue";

export default {
  name: "Dropdown",
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
        this.$emit("dropdownIsVisualized");
      }
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
    },
    selectProvider(event) {
      this.$emit("selectedProvider");
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
  min-width: @width + 30px;
  max-width: @width + @width;
  box-shadow: @defaultShadow;
  position: absolute;
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