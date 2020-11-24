<template>
  <v-app class="ma-0">
    <div ref="callbutton" :class="['call-button-container']">
      <dropdown
        v-click-outside="hideDropdown"
        v-if="providersButton.length > 1"
        ref="dropdown"
        :positionclass="positionClass"
        :providersbutton="providersButton"
        :isopen="isOpen"
        :header="header"
        @updated="createButtons"
        @getRefs="getRef($event)"
        @showDropdown="showDropdown($event)"
        @dropdownIsVisualized="fireDropdownIsVisualized"
        @selectedProvider="hideDropdown"/>
      <singlebtn 
        v-else />
    </div>
  </v-app>
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
    }
  },
  data() {
    return {
      providersButton: [],
      error: null,
      isOpen: false,
      isDropdownVisualized: false, // is added to DOM
      childRef: null,
      isFirstInitialization: true,
    };
  },
  computed: {
    dropdown: function() {
      return this.$refs.callbutton.getBoundingClientRect();
    },
    dropdownContainer: function() {
      return this.$refs.dropdown.$refs.buttonsContainer.getBoundingClientRect();
    },
    client: function() {
      return document.body.getBoundingClientRect();
    },
    positionClass: function() {
      let position = "right";
      if (this.isDropdownVisualized) {
        const widthRelation = (this.client.width - this.dropdown.left) / this.dropdownContainer.width;
        if (widthRelation < 1.1) {
          position = "left";
        }
      }
      return position;
    },
    parentContainerElement() {
      return this.$refs.callbutton.parentElement.parentElement.parentElement;
    },
    parentClass() {
      return Object.values(this.parentContainerElement.classList).join("");
    },
    condition() {
      return this.parentClass.includes("call-button-mini") 
        || this.parentClass.includes("call-button--tiptip");
    },
    header() {
      return this.condition
        ? { placeholder: "", bgHover: "white", paddingClass: "pa-1", bgMini: this.isOpen ?  "#d3d6db"  : "#ffffff"}
        : {
            placeholder: this.$i18n.te("webconferencing.callHeader")
              ? this.$i18n.t("webconferencing.callHeader")
              : "Start Call",
            bgHover: this.isOpen ? "var(--allPagesGreyColor, #e1e8ee)" : "white",
            paddingClass: "px-2"
          };
    }
  },
  watch: {
    callContext(newContext, oldContext) {
      this.setProvidersButtons(newContext);
    }
    // screenWidth(newWidth, oldWidth) {
    //   if (newWidth <= 980) {
    //     console.log("WIIDTH")
    //     // this.parentClass = this.parentClass + "call-button-mini";
    //   }
    // }
  },
  // mounted() {
  //   this.$nextTick(() => {
  //     window.addEventListener("resize", this.onResize);
  //   });
  // },
  methods: {
    // onResize() {
    //   this.screenWidth = window.innerWidth;
    // },
    setProvidersButtons(context) {
      this.providersButton.splice(0);
      this.$refs.callbutton.classList.remove("single");
      this.hideDropdown();
      const thevue = this;
      try {
        if (context && context.details && this.providersButton.length === 0) {
          const callButtons = [];
          context.parentClasses = this.parentClass;
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
                  if (button.value.$data) {
                    button.value.$data.header = "CALL";
                  }
                }
              });
              thevue.createButtons();
            });
          });
        } else if (context && !context.details) {
          // mini chat
          if (this.isFirstInitialization) {
            this.isFirstInitialization = false;
            log.trace("Call buttons are initialized first time");
            this.fireCreated();
          }
        }
      } catch (err) {
        log.error("Error building call buttons", err);
      }
    },
    createButtons() {
      log.trace("CREATE BUTTONS");
      let ref;
      let vm = null;
      if (this.providersButton.length !== 0) {
        for (const [index, pb] of this.providersButton.entries()) {
          if (this.providersButton.length > 1) {
            //add buttons to dropdown component
            if (this.isOpen) {
              ref = this.childRef.callbutton[index];
              // add vue button
              if (pb instanceof Vue) {
                vm = pb.$mount(); // TODO why we need vm globaly?
                // vm.$el.innerHTML = "<span class='v-btn__content'><i class='uiIconSocPhone uiIconBlue'></i>Jitsi Call</span>";
                ref.appendChild(vm.$el);
              } else {
                // add button as DOM Element
                ref.appendChild(pb);
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
              const container = vm.$children[0].$el.childNodes[0].childNodes[2];
              const singleBtnContainer = this.condition
                ? container.classList.add("display-hidden")
                : vm.$el.childNodes[0];
              callButton.appendChild(vm.$el);
            } else {
              // add button as DOM Element
              callButton.appendChild(pb);
            }
          }
        }
      } else {
        const callButton = this.$refs.callbutton.childNodes[0];
        callButton.innerHTML = "";
      }

      if (this.isFirstInitialization) {
        this.isFirstInitialization = false;
        log.trace("Call buttons are initialized first time");
        this.fireCreated();
      }
    },
    showDropdown() {
      this.isOpen = !this.isOpen;
    },
    hideDropdown() {
      if (this.isOpen) {
        this.isOpen = false;
        this.isDropdownVisualized = false;
        this.parentContainerElement.blur();
      }
    },
    getRef(ref) {
      this.childRef = ref;
    },
    fireCreated() {
      this.$emit("created");
    },
    fireDropdownIsVisualized() {
      this.isDropdownVisualized = true;
    }
  }
};
</script>

<style lang="less">
@import "../../../skin/less/variables.less";
@import "../../../skin/less/mixins.less";
.VuetifyApp {
  .call-button-container {
    min-width: 36px;
    &:hover {
      .dropdown-header {
        background-color: var(--allPagesGreyColor, #e1e8ee);
      }
    }
    button {
      .v-btn__content {
        letter-spacing: 0.1px;
        padding: 0 10px;
      }
    }
    a:hover,
    button:hover {
      i, span {
        color: white;
      }
    }
    &.single {
      width: @width - 14px;
      height: 36px;
      border: 1px solid rgb(232, 238, 242);
      border-radius: 3px;
      // padding: 0 10px;
      background-color: #ffffff;
      .single-btn-container {
        height: inherit;
        a {
          display: flex;
          height: inherit;
        }
      }
      &:hover {
        background-color: var(--allPagesGreyColor, #e1e8ee);
        .single-btn-container, button  {
          background-color: var(--allPagesGreyColor, #e1e8ee);
        }

        a:hover,
        button:hover {
          i {
            color: @primaryColor;
          }
          span {
            color: unset;
          }
        }
      }
    }
    a,
    a:hover,
    a:focus {
      color: unset;
    }
    [class^="call-button-container-"] {
      width: 100%;
    }
    cursor: pointer !important;
    position: relative;
    z-index: 100;
    min-height: 36px;
  }
  [class^="call-button-container-"]:hover,
  button:hover {
    i, span {
      color: white;
    }
  }
  .room-actions-container {
    .call-button-container {
      a {
        [class^="uiIcon"] {
          &:before {
            color: unset;
            margin-right: 4px;
          }
        }
      }
    }
  }
}
.call-button-mini {
  .call-button-container {
    #dropdown-vue {
      .buttons-container {
        &.left {
          right: -10px;
        }
        [class^="call-button-container-"] {
          button {
            background: transparent;
            box-shadow: none;
            border: none;
          }
        }
      }
    }
    &.single {
      width: unset;
      border: none;
      .single-btn-container {
        button {
          margin-right: 0;
          border: none;
          background: transparent;
        }
      }
    }
  }
  &:hover {
    background-color: unset;
    #dropdown-vue {
      background-color: unset;
      .dropdown-header {
        background-color: unset;
        .uiIconMiniArrowDown {
          background: #d3d6db;
          border-radius: 50%;
        }
      }
    }
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
            width: inherit;
          }
        }
      }
    }
  }
}
.call-button-mini.call-button--tiptip {
  .call-button-container {
    #dropdown-vue {
      position: relative;
      .buttons-container {
        &.left {
          right: -16px;
        }
      }
      .dropdown-header {
        position: relative;
        .dropdown-heading {
          .uiIconSocPhone {
            vertical-align: text-top;
            &::before {
              content: "\e92b";
            }
          }
        }
        .uiIconMiniArrowDown {
          position: absolute;
          right: -16px;
          font-size: 8px !important;
          padding: 4px;
          bottom: -2px;
          border-radius: 50%;
          &::before {
            color: @primaryColor;
          }
        }
      }
    }
    .buttons-container {
      position: absolute;
      top: 23px;
      box-shadow: 1px 1px 10px rgba(0, 0, 0, 0.15);
      [class^="call-button-container-"] {
        text-align: left;
        a,
        a:hover,
        a:focus {
          color: black;
        }
        a:hover,
        button:hover {
          i, span {
            color: white;
          }
        }
        &:hover {
          i, span {
            color: white;
          }
        }
        button {
          padding-left: 0;
          .logo {
            margin-bottom: -5px;
          }
          .v-btn__content {
            .uiIconSocPhone  {
              font-size: 16px !important;
                &::before {
                content: "\e92b";
              }
            }
          }
        }
      }
    }
  }
}
.call-button-mini.call-button--chat-drawer {
  margin-right: 11px;
}
.space-action-menu {
  width: 86px;
  height: 36px;
  position: absolute;
  .call-button.call-button--space {
    .call-button-container {
      &.single {
        top: 14px;
        left: -117%;
      }
    }
  }
}

#UIProfileHeaderContainer {
  position: relative;
  z-index: 100;
}
</style>