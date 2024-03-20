<template>
  <div
    :style="{ 'background-color': header.bgHover }"
    class="dropdown-header">
    <div
      class="dropdown-heading d-flex d-row align-center justify-center ps-2"
      @click="startCall()">
      <v-tooltip bottom>
        <template v-slot:activator="{ on, attrs }">
          <v-icon
            size="20"
            v-bind="attrs"
            v-on="on">
            fas fa-video
          </v-icon>
        </template>
        <span>{{ $t("webconferencing.callHeader") ? $i18n.t("webconferencing.callHeader")
          : "Start Call" }}</span>
      </v-tooltip>
      <span v-if="!isMobile" class="ps-2 text-color">
        {{ $t("webconferencing.callHeader") ? $i18n.t("webconferencing.callHeader")
          : "Start Call" }}</span>
    </div>
    <v-divider 
      class="mx-1 uiVertinalDividerMini"
      dark  
      inset
      vertical />
    <div class="px-1" @click="showdropdowncomponent(); passrefs()">
      <v-icon
        size="18"
        class="pb-1">
        fas fa-caret-down
      </v-icon>
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
    startCall: {
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
      font-size: 18px;
      color: var(--allPagesDarkGrey, #000000) !important;
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
    border: none !important;
    background: transparent;
    .dropdown-heading {
      span {
        display: none;
      }
    }
    .uiVertinalDividerMini {
      display: none;
    }
    .uiIconMiniArrowDown {
      position: absolute;
      top: 9px;
      right: 2px;
      text-align: center;
      display: none;
      &::before {
        color: @primaryColor;
      }
    }
  }
}
</style>
