<template>
  <div class="dropdown-header" @click="opendropdowncomponent(); passrefs()">
    <div class="dropdown-heading">
      <i class="uiIconSocPhone uiIconSocBlue"></i>
      {{ header.placeholder }}
    </div>
    <v-divider 
      :style="header.divider"
      vertical />
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
    opendropdowncomponent: {
      type: Function,
      required: true
    },
    passrefs: {
      type: Function,
      required: true
    }
  },
  computed: {
    divider() {
      return {
        display: "inline-flex"
      };
      /*!store.state.mini
        ? {
            display: "inline-flex"
          }
        : {
            display: "none"
          };*/
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
  .v-divider--vertical {
    min-height: 36px;
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