<template>
  <div
    :style="{ 'background-color': header.bgHover }"
    class="dropdown-header"
    @click="showdropdowncomponent(); passrefs()">
    <div class="dropdown-heading d-flex d-row align-center justify-center">
      <v-tooltip bottom>
        <template v-slot:activator="{ on, attrs }">
          <i 
            v-bind="attrs"
            v-on="on" 
            class="uiIconSocPhone uiIconSocBlue v-btn--icon v-size--default d-flex align-center justify-end pr-1"></i>
        </template>
        <span>{{ $i18n.te("webconferencing.callHeader") ? $i18n.t("webconferencing.callHeader")
          : "Start Call" }}</span>
      </v-tooltip>
      <span v-if="!isMobile">
        {{ $i18n.te("webconferencing.callHeader") ? $i18n.t("webconferencing.callHeader")
          : "Start Call" }}</span>
    </div>
    <div class="pr-4 pl-2">
    <i
        :style="{ 'background-color': header.bgMini }"
        :class="header.paddingClass"
        class="uiIconMiniArrowDown uiIconLightGray "></i>
    </div>
  </div>
</template>

<script>
export default {
  computed: {
    isMobile() {
      return this.$vuetify && this.$vuetify.breakpoint && this.$vuetify.breakpoint.name === 'xs';
    },
  },
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
    border-radius: 4px;
    border: 1px solid var(--allPagesBtnBorder, var(--allPagesGreyColor, #e1e8ee)) !important;
    width: 100%;
    min-height: 36px;
    color: @primaryColor !important;
    letter-spacing: 0.09em;
    .uiIconMiniArrowDown {
      color: var(--allPagesDarkGrey, #4d5466) !important;
    }
    .dropdown-heading {
      i {
        vertical-align: text-bottom;
        text-align: center;
      }
      .uiIconSocPhone {
        &:before {
          color: unset;
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
    min-height: 10px;
    border: none;
    background: transparent;
    .dropdown-heading {
      span {
        display: none;
      }
    }
    .uiIconMiniArrowDown {
      position: absolute;
      top: 9px;
      right: 2px;
      text-align: center;
      &::before {
        color: @primaryColor;
      }
    }
  }
}
</style>