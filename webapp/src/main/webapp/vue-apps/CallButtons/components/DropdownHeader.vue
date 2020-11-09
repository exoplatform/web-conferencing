<template>
  <div :style="{backgroundColor: isopen ? 'var(--allPagesGreyColor, #e1e8ee)' : 'white'}" class="dropdown-header" @click="showdropdowncomponent(); passrefs()">
    <div class="dropdown-heading">
      <i class="uiIconSocPhone uiIconSocBlue"></i>
      {{ header.placeholder }}
    </div>
    <i class="uiIconMiniArrowDown uiIconLightGray"></i>
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
    },
    isopen: {
      type: Boolean,
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
    // &:focus {
    //   background-color: var(--allPagesGreyColor, #e1e8ee);
    // }
    .uiIconMiniArrowDown {
      color: var(--allPagesDarkGrey, #4d5466) !important;
      ::before {
        color: var(--allPagesDarkGrey, #4d5466) !important;
      }
    }
    .dropdown-heading {
      padding-left: 5px;
      flex: 3;
      text-align: center;
      [class^="uiIcon"] {
        &::before {
          content: "\e92b";
        }
        // font-size: 14px;
      }
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
    .uiIconMiniArrowDown {
      height: 11px;
      font-size: 9px !important;
      top: 5px;
      padding: 2px 3px 0 3px;
    }
  }
}
</style>