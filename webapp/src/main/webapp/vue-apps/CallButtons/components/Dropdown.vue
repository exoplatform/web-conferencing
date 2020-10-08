<template>
  <div id="dropdown-vue">
    <dropdown-header
      :placeholder="placeholder"
      :opendropdowncomponent="openDropdownComponent"
      :passrefs="passRefs"/>
    <div v-if="isopen" class="template">
      <div
        v-for="(button, index) in providersbutton"
        :key="index"
        :id="`call-button-container-${button._uid}`"
        :ref="`callbutton`"
        @click="openDropdownComponent">
      </div>
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
    createbuttons: {
      type: Function,
      required: true
    },
    placeholder: {
      type: String,
      required: true
    }
  },
  updated() {
    this.createbuttons();
  },
  methods: {
    openDropdownComponent(isOpen) {
      this.$emit("openDropdown", isOpen);
    },
    passRefs() {
      this.$emit("getRefs", this.$refs);
    },
  }
};
</script>