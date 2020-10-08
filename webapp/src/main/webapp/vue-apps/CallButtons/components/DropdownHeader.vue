<template>
  <div class="dropdown-header">
    <i class="uiIconSocPhone uiIconSocBlue"></i>
    <div id="dropdown-span">{{ placeholder }}</div>
    <v-divider vertical />
    <i class="uiIconArrowDown" @click="opendropdowncomponent(); passrefs()"></i>
  </div>
</template>
<script>
export default {
  props: {
    placeholder: {
      type: String,
      required: true
    },
    opendropdowncomponent: {
      type: Function,
      required: true
    },
    passrefs: {
      type: Function,
      required: true
    }
  }
};
</script>
<style>
.dropdown-header {
  display: inline-flex;
  align-items: center;
}
</style>