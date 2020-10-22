<template>
  <div ref="callbutton" class="call-button-container">
    <!-- <p>{{ $store.state.count }}</p> -->
    <dropdown
      v-if="providersButton.length > 1"
      :providersbutton="providersButton"
      :isopen="isOpen"
      :placeholder="placeholder"
      @updated="createButtons"
      @getRefs="getRef($event)"
      @openDropdown="openDropdown($event)" />
    <singlebtn v-else :providersbutton="providersButton" @updated="createButtons" />
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
    // dropdown: function() {import("./Dropdown.vue")}
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
    }
  },
  data() {
    return {
      //callContext: null,
      providersButton: [],
      error: null,
      placeholder: "Start call",
      isOpen: false,
      childRef: null
    };
  },
  computed: {
    callContext() {
      return this.$store.state.callContext;
    }
  },
  watch: {
    callContext(newContext, oldContext) {
      // console.log(newContext, oldContext, "new")
      this.providersButton = [];
      this.$refs.callbutton.classList.remove("single");
      this.setProvidersButtons(newContext);
    }
  },
  // computed: {
  //   dropdown() {
  //     return () => import("./Dropdown.vue");
  //   }
  // },
  created() {
    // const thevue = this;
    // this.$store.dispatch("setProvidersButtons")
    // try {
    //   if (thevue.callContext && thevue.callContext.details) {
    //     const callButtons = [];
    //     webConferencing.getAllProviders().then(providers => {
    //       providers.map(provider => {
    //         callButtons.push(provider.callButton(thevue.callContext));
    //       });
    //       Promise.allSettled(callButtons).then(resCallButtons => {
    //         resCallButtons.forEach((button) => {
    //           if (button.status === "fulfilled") {
    //             this.$store.state.providersButton.push(button.value);
    //           }
    //         });
    //         thevue.createButtons();
    //       });
    //     });
    //   }
    // } catch (err) {
    //   log.error("Error building call buttons", err);
    // }
      this.setProvidersButtons(this.callContext);
  },
  beforeDestroy() {
    // console.log(this.$store.state.callContext, "beforeDestroy")
  },
  updated() {
    // console.log(this.$store.state.callContext, "UPdate")
  },
  methods: {
    async initProvidersButton__donotuse() {
      // TODO do we needit actually? it is not reusable
      const thevue = this;
      await Promise.all(
        thevue.providers.map(async p => {
          // TODO async here???!
          if (await p.isInitialized) {
            // TODO await for boolean property??
            const callButton = await p.callButton(this.callContext, "vue");
            this.providersButton.push(callButton);
          }
        })
      );
    },
    setProvidersButtons(context) {
      const thevue = this;
      try {
        if (context && context.details && this.providersButton.length === 0) {
          const callButtons = [];
          webConferencing.getAllProviders().then(providers => {
            providers.map(provider => {
              callButtons.push(provider.callButton(context));
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
        }
      } catch (err) {
        log.error("Error building call buttons", err);
      }
    },
    createButtons() {
      let ref;
      let vm = null;

      //if (this.providersButton.length) {
      for (const [index, pb] of this.providersButton.entries()) {
        if (this.providersButton.length > 1) {
          //add buttons to dropdown coomponent
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
            // console.log(pb);
            // console.log(vm, callButton, "vm");
            callButton.appendChild(vm.$el);
          } else {
            // add button from DOM Element
            callButton.appendChild(pb.get(0));
          }
        }
      }
    },
    openDropdown() {
      this.isOpen = !this.isOpen;
    },
    getRef(ref) {
      // console.log(ref);
      this.childRef = ref;
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
</style>