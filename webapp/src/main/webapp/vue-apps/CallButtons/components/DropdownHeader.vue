<template>
  <div class="dropdown-header" @click="showdropdowncomponent(); passrefs()">
    <div class="dropdown-heading">
      <i class="uiIconSocPhone uiIconSocBlue"></i>
      {{ header.placeholder }}
    </div>
    <i class="uiIconArrowDownMini"></i>
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
    // padding: 0;
    display: inline-flex;
    align-items: center;
    background-color: white;
    border: 1px solid rgb(232, 238, 242);
    border-radius: 3px;
    width: 100%;
    min-height: 36px;
    [class^="uiIcon"] {
      font-size: 12px;
    }
    .dropdown-heading {
      padding-left: 5px;
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
    .uiIconArrowDownMini {
        height: 14px;
        font-size: 10px !important;
        top: 5px;
        padding-top: 4px;
      }
  }
}
</style>