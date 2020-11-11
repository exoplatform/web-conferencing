<template>
  <div
    :style="{ 'background-color': header.bgHover }"
    class="dropdown-header"
    @click="showdropdowncomponent(); passrefs()">
    <div class="dropdown-heading px-2">
      <i class="uiIconSocPhone uiIconSocBlue"></i>
      {{ header.placeholder }}
    </div>
    <i 
      :style="{ 'background-color': header.bgMini }" 
      :class="header.paddingClass" 
      class="uiIconMiniArrowDown uiIconLightGray"></i>
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
@import "../../../skin/less/variables.less";

.VuetifyApp {
  .dropdown-header {
    display: inline-flex;
    align-items: center;
    background-color: white;
    border: 1px solid rgb(232, 238, 242);
    border-radius: 3px;
    width: 100%;
    min-height: 36px;
    .uiIconMiniArrowDown {
      color: var(--allPagesDarkGrey, #4d5466) !important;
      border-radius: 50%;
    }
    .dropdown-heading {
      i {
        vertical-align: text-bottom;
        text-align: center;
      }
      .uiIconSocPhone {
        &:before {
          content: "\e92b";
        }
      }
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
    .uiIconMiniArrowDown {
      position: absolute;
      top: 11px;
      right: -7px;
      text-align: center;
      &::before {
        color: @primaryColor;
      }
    }
  }
}
</style>