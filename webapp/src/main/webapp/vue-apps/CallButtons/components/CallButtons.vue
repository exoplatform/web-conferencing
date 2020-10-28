<template>
  <div ref="callbutton" :class="['call-button-container']">
    <dropdown
      v-if="providersButton.length > 1"
      :providersbutton="providersButton"
      :isopen="isOpen"
      :placeholder="getPlaceholder()"
      @updated="createButtons"
      @getRefs="getRef($event)"
      @openDropdown="openDropdown($event)" />
    <singlebtn 
      v-else 
      :providersbutton="providersButton" />
  </div>
</template>

<script>
import dropdown from "./Dropdown.vue";
import singlebtn from "./SingleButton.vue";

const log = webConferencing.getLog("webconferencing-call-buttons");
export default {
  components: {
    dropdown,
    singlebtn
  },
  props: {
    i18n: {
      type: Object,
      required: true
    },
    language: {
      type: String,
      required: true
    },
    resourceBundleName: {
      type: String,
      required: true
    }, 
    store: {
      type: Object,
      required: true
    },
    st: {
      type: Object,
      required: true
    },
  },
  data() {
    return {
      providersButton: [],
      error: null,
      isOpen: false,
      childRef: null,
      initFinished: true
    };
  },
  computed: {
    callContext() {
      if (this.store) {
        return this.store.state.callContext;
      }
    },
  },
  watch: {
    callContext(newContext, oldContext) {
      if (this.initFinished) {
        this.initFinished = false;
        this.providersButton.splice(0);
        this.$refs.callbutton.classList.remove("single");
        this.setProvidersButtons(newContext);
      }
    }
  },
  methods: {
    setProvidersButtons(context) {
      this.isOpen = false;
      const thevue = this;
      try {
        if (context && context.details && this.providersButton.length === 0) {
          const callButtons = [];
          webConferencing.getAllProviders().then(providers => {
            providers.map(provider => {
              if (provider.isInitialized) {
                 callButtons.push(provider.callButton(context));
              }
            });
            Promise.allSettled(callButtons).then(resCallButtons => {
              resCallButtons.forEach(button => {
                if (button.status === "fulfilled") {
                  this.providersButton.push(button.value);
                }
              });
              thevue.createButtons();
            });
          });
        } else {
          this.initFinished = true;
        }
      } catch (err) {
        log.error("Error building call buttons", err);
        this.initFinished = true;
      }
    },
    createButtons() {
      let ref;
      let vm = null;
      // const classList = Object.values(this.$refs.callbutton.classList);
      const parentClass = Object.values(this.$refs.callbutton.parentElement.classList);
      // if (parentClass.indexOf("mini") !== -1) {
         //const condition = parentClass.indexOf("mini") !== -1 || parentClass.indexOf("popup") !== -1;
       // store.commit("toggleMini", condition);
      // }
      for (const [index, pb] of this.providersButton.entries()) {
        if (this.providersButton.length > 1) {
          //add buttons to dropdown component
          if (this.isOpen) {
            ref = this.childRef.callbutton[index];
            // add vue button
            if (pb instanceof Vue) {
              vm = pb.$mount(); // TODO why we need vm globaly?
              ref.appendChild(vm.$el);
            } else {
              // add button from DOM Element
              ref.appendChild(pb.get(0));
            }
          }
        } else {
          //add a single button
          const callButton = this.$refs.callbutton.childNodes[0];
          this.$refs.callbutton.classList.add("single");
          callButton.innerHTML = "";
          if (pb instanceof Vue) {
            // add vue button
            vm = pb.$mount(); // TODO why we need vm globaly?
            callButton.appendChild(vm.$el);
          } else {
            // add button from DOM Element
            callButton.appendChild(pb.get(0));
          }
        }
      }
      this.initFinished = true;
    },
    openDropdown() {
      this.isOpen = !this.isOpen;
    },
    getRef(ref) {
      this.childRef = ref;
    },
    getPlaceholder() {
      // TO Do check the element class
      return "Start call";
    }
  }
};
</script>

<style lang="less">
@import "../../../skin/less/variables.less";
.VuetifyApp {
  .call-button-container {
    button {
      .v-btn__content {
        letter-spacing: 0.1px;
      }
    }
    &.single {
      width: @width - 14px;
      height: 36px;
      left: @width + 60px;
      border: 1px solid rgb(232, 238, 242);
      border-radius: 3px;
      padding: 0 5px;
      &:hover {
        background-color: @primaryColor;
        opacity: 1;
      }
    }
    a:hover,
    button:hover {
      i {
        color: white;
      }
      span {
        color: white;
      }
    }
    cursor: pointer !important;
    position: absolute;
    z-index: 100;
    left: @width + 40px;
    top: 2px;
    width: @width + 20px;
    [class^="uiIcon"] {
      font-size: 12px;
    }
  }
  a,
  a:hover,
  a:focus {
    color: unset;
  }
  .single:hover,
  [class^="call-button-container-"]:hover,
  button:hover {
    i {
      color: white;
    }
    span {
      color: white;
    }
  }
  .room-actions-container {
    [class^="uiIcon"] {
      &.callButtonIconVideo {
        top: 6px;
      }
      &:before {
        color: unset;
        height: 16px;
        width: 16px;
        margin-right: 4px;
      }
    }
    // .room-action-menu {
    //   .room-action-component {
    //     .webConferencingCallButtonAction {
    //       // .call-button-container.single:hover,
    //       // [class^="call-button-container-"]:hover,
    //       // a:hover,
    //       // button:hover {
    //       //   i {
    //       //     color: white;
    //       //   }
    //       //   span {
    //       //     color: white;
    //       //   }
    //       // }
    //       // button {
    //       //   .v-btn__content {
    //       //     letter-spacing: 0.1px;
    //       //   }
    //       // }
    //     }
    //   }
    // }
  }
}
.mini {
  .call-button-container {
    right: 100px;
    left: unset;
    top: 12px;
    width: unset;
  }
}
</style>