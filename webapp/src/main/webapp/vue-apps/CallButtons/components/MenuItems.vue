<template>
  <div class="dropdown-vue">
    <v-menu
      :value="isopen"
      @input="onMenuToggle"
      :close-on-content-click="false"
      offset-x
      right
      open-on-hover
      eager
      transition="slide-x-transition">
      <template #activator="{ on, attrs }">
        <div v-bind="attrs" v-on="on">
          <dropdown-header
            ref="buttonsheader"
            :header="header"
            :showdropdowncomponent="showDropdownComponent"
            :passrefs="passRefs"
            :start-call="startCall"
            is-menu=true />
        </div>
      </template>

      <v-list 
        class="white elevation-3 pa-0 overflow-visible"
        min-width="180"
        dense
        eager>
        <v-list-item
          v-for="(button, index) in providersbutton"
          :key="index"
          class="px-0 py-0"
          style="min-height: 36px;">
          <div
            :class="`call-button-container-${index} w-100 d-flex align-center`"
            style="min-height: 36px;"
            ref="callbutton"
            @click="selectProvider">
          </div>
        </v-list-item>
      </v-list>
    </v-menu>
  </div>
</template>

<script>
import DropdownHeader from './DropdownHeader.vue';

export default {
  name: 'Dropdown',
  components: {
    DropdownHeader
  },
  props: {
    providersbutton: {
      type: Array,
      required: true
    },
    isopen: {
      type: Boolean,
      required: true
    },
    header: {
      type: Object,
      required: true
    },
    positionclass: {
      type: String,
      required: true
    }
  },
  watch: {
    isopen(value) {
      if (value) {
        this.$emit('dropdownIsVisualized');
      }
    }
  },
  mounted() {
    this.passRefs();
  },
  methods: {
    showDropdownComponent() {
      this.$emit('showDropdown');
    },
    onMenuToggle(val) {
      if (val !== this.isopen) {
        this.$emit('showDropdown');
        this.$nextTick(() => {
          this.$emit('updated');
        });
      }
    },
    startCall() {
      this.providersbutton[0].callSettings.onCallOpen();
    },
    passRefs() {
      this.$emit('getRefs', this.$refs);
    },
    selectProvider() {
      this.$emit('selectedProvider');
    }
  }
};
</script>

<style scoped lang="less">
@import "../../../skin/less/variables.less";

[class^="call-button-container-"] {
  cursor: pointer;
  width: 100%;
  display: flex !important;
  align-items: center !important;
  
  &:hover {
    background-color: @primaryColor !important;
    
    a, span, i, .v-icon {
      color: white !important;
    }
  }
}
</style>
