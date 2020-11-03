<template>
  <div ref="callbutton" :class="['call-button-container']">
    <dropdown
      v-click-outside="hideDropdown"
      v-if="providersButton.length > 1"
      :providersbutton="providersButton"
      :isopen="isOpen"
      :header="header"
      @updated="createButtons"
      @getRefs="getRef($event)"
      @showDropdown="showDropdown($event)" />
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
  name: "CallButtons",
  components: {
    dropdown,
    singlebtn
  },
  props: {
    language: {
      type: String,
      required: true
    },
    resourceBundleName: {
      type: String,
      required: true
    }, 
    callContext: {
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
    header() {
      const parentClass = Object.values(this.$refs.callbutton.parentElement.classList).join("");
      const condition = parentClass.includes("mini") || parentClass.includes("popup");
      return condition ? {placeholder: ""} : {placeholder: this.$i18n.te("webconferencing.callHeader")
        ? this.$i18n.t("webconferencing.callHeader")
      : "Start Call"}
    },
  },
  watch: {
    callContext(newContext, oldContext) {
      if (this.initFinished) {
        this.setProvidersButtons(newContext);
      }
    },
  },
  methods: {
    setProvidersButtons(context) {
      this.initFinished = false;
      this.providersButton.splice(0);
      this.$refs.callbutton.classList.remove("single");
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
            const parentClass = Object.values(this.$refs.callbutton.parentElement.classList).join("");
            const condition = parentClass.includes("mini") || parentClass.includes("popup");
            const singleBtnContainer = condition ? vm.$el.childNodes[0].removeChild(vm.$el.childNodes[0].childNodes[1]) : vm.$el.childNodes[0]
            callButton.appendChild(vm.$el);
          } else {
            // add button from DOM Element
            callButton.appendChild(pb.get(0));
          }
        }
      }
      this.initFinished = true;
    },
    showDropdown() {
      this.isOpen = !this.isOpen;
    },
    hideDropdown() {
      this.isOpen = false;
    },
    getRef(ref) {
      this.childRef = ref;
    }
  },
  }
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
      left: 0px;
      border: 1px solid rgb(232, 238, 242);
      border-radius: 3px;
      padding: 0 5px;
      background-color: #ffffff;
      &:hover {
        background-color: @primaryColor;
        opacity: 1;
        border-color: @primaryColor
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
    a,
    a:hover,
    a:focus {
      color: unset;
    }
    cursor: pointer !important;
    position: relative;
    z-index: 100;
    left: 0;
    top: 0;
    min-height: 36px;
    width: @width + 20px;
    [class^="uiIcon"] {
      font-size: 12px;
    }
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
  }
}
.call-button-mini {
  width: min-content;
  .call-button-container {
    left: -19px;
    top: 0;
    width: unset;
    &.single {
      border: none;
      width: inherit;
      .single-btn-container {
        width: inherit;
        button {
           width: inherit;
           margin-right: 0;
           border: none;
           background: #ffffff;
           span {
             width: inherit
           }
        }
      }
    }
  }
}
.call-button--profile {
      width: 120px;
      height: 36px;
      position: relative;
    .call-button-container {
      left: -129px;
      top: -38px;
      &.single {
        left: -105px;
      }
    }
}
.space-action-menu {
    width: 86px;
    height: 36px;
    position: absolute;
  .call-button.call-button--space {
    .call-button-container {
      &.single{
        top: 14px;
        left: -100px;
      }
    }
  }
}
#UIProfileHeaderContainer {
  position: relative;
  z-index: 100;
}
</style>