<template>
  <div class="dropdown-header" @click="opendropdowncomponent(); passrefs()">
    <div id="dropdown-span">
      <i class="uiIconSocPhone uiIconSocBlue"></i>
      <span>{{ placeholder }}</span>
    </div>
    <v-divider class="mx-4" vertical />
    <i class="uiIconArrowDown"></i>
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

<style scoped lang="less">
.VuetifyApp {
  .dropdown-header {
    display: inline-flex;
    align-items: center;
    background-color: white;
    border: 1px solid rgb(232, 238, 242);
    border-radius: 3px;
    width: 100%;
    #dropdown-span {
      flex: 3;
      text-align: center;
    }
    i {
      flex: 1;
      text-align: center;
    }
  }
  hr {
    margin: 0;
  }
  .v-divider--vertical {
    min-height: 36px;
  }
}
</style>