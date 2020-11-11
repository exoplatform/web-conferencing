<template>
  <div
    :style="{ 'background-color': header.bgHover }"
    class="dropdown-header"
    @click="showdropdowncomponent(); passrefs()">
    <div class="dropdown-heading px-2">
      <i class="uiIconSocPhone uiIconSocBlue"></i>
      {{ header.placeholder }}
    </div>
    <i class="uiIconMiniArrowDown uiIconLightGray pr-2"></i>
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
    },
    parentclass: {
      type: String,
      required: true
    }
  },
  computed: {
    // color() {
    //   return this.isopen && (!this.parentclass.includes("call-button-mini") || !this.parentclass.includes("call-button--tiptip")) ? "var(--allPagesGreyColor, #e1e8ee)" : "white"

    // }
  },
  created() {
    console.log(this.parentclass);
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
      top: 3px;
      // ::before {
      //   color: var(--allPagesDarkGrey, #4d5466) !important;
      // }
    }
    .dropdown-heading {
      // padding-left: 5px;
      // flex: 3;
      text-align: center;
      .uiIconSocPhone {
        &::before {
          content: "\e92b";
        }
      }
    }
    i {
      // flex: 1;
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