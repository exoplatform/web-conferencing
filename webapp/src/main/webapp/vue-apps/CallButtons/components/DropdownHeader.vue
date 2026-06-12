<template>
  <div
    class="dropdown-header">
    <div
      class="dropdown-heading d-flex d-row align-center justify-center"
      :class="!isMenu && ps-2"
      @click.stop.prevent="startCall()">
      <v-tooltip bottom>
        <template v-slot:activator="{ on, attrs }">
          <v-icon
            class="align-center d-flex"
            :size="isMenu ? '18' : '20'"
            v-bind="attrs"
            v-on="on">
            fas fa-video
          </v-icon>
        </template>
        <span>{{ $t("webconferencing.callHeader") ? $i18n.t("webconferencing.callHeader")
          : "Start Call" }}</span>
      </v-tooltip>
      <span 
        v-if="!isMobile" 
        :class="isMenu ? 'ms-4' : 'ps-2'"
        class="text-color">
        {{ $t("webconferencing.callHeader") ? $i18n.t("webconferencing.callHeader")
          : "Start Call" }}</span>
    </div>
    <v-divider 
      v-if="!isMenu"
      class="mx-1 uiVertinalDividerMini"
      dark  
      inset
      vertical />
    <div 
      :class="isMenu ? 'ps-3' : 'px-1'"
      @click.stop.prevent="showdropdowncomponent(); passrefs()">
      <v-icon
        size="18"
        class="pb-1">
        {{ iconName }}
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
    iconName () {
      return this.isMenu ? 'fas fa-caret-right' : 'fas fa-caret-down';
    }
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
    },
    isMenu: {
      type: Boolean,
      default: false
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
    background-color: unset;
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
