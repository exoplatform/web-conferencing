<template>
  <div class="dropdown-header" @click="showdropdowncomponent(); passrefs()">
    <div class="dropdown-heading">
      <i class="uiIconSocPhone uiIconSocBlue"></i>
      {{ header.placeholder }}
    </div>
    <i class="uiIconArrowDown"></i>
  </div>
</template>

<script>

export default {
  props: {
    header: {
      type: Object,
      required: true
    },
    showdropdowncomponent: {
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
    min-height: 36px;
    .dropdown-heading {
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
}
.call-button-mini {
  .dropdown-header {
    border: none;
    background: transparent;
    .dropdown-heading {
      flex: 10;
      text-align: right;
    }
  }
}
</style>