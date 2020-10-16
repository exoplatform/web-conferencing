<template>
  <!-- TODO we cannot use ID as many buttons may appear on the page!! Can class work for us? -->
  <div id="call-button-container" ref="callbutton">
    <dropdown
      v-if="providersButton.length > 1"
      :providersbutton="providersButton"
      :isopen="isOpen"
      :createbuttons="createButtons"
      :placeholder="placeholder"
      @getRefs="getRef($event)"
      @openDropdown="openDropdown($event)"/>
  </div>
</template>

<script>
import dropdown from "./Dropdown.vue";

const log = webConferencing.getLog("webconferencing-call-buttons");

let vm = null; // TODO why we need vm globaly?
let ref; // TODO what is this a global one?

export default {
  components: {
    dropdown
  },
  props: {
    services: {
      type: Object,
      required: true
    },
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
      //providers: [],
      providersButton: [],
      error: null,
      placeholder: "Start call",
      isOpen: false,
      childRef: null
      //providersTypes: []
    };
  },
  async beforeMount() {
    //const thevue = this;
    
    try {
      //const providersConfig = await webConferencing.getProvidersConfig();
      //this.providersTypes = providersConfig.map(provider => provider.type);
      const context = await webConferencing.getCallContext();
      //thevue.callContext = context;
      //if (this.callContext) {
      const providers = [];
      try {
        providers.push(await webConferencing.getProvider("jitsi"));
        //await Promise.all(
        //  this.providersTypes.map(async type => {
        //    const p = await webConferencing.getProvider(type);
        //    this.providers.push(p);
        //  })
        //);
        //await this.initProvidersButton();
        await Promise.all(
          providers.map(async p => {
            const callButton = await p.callButton(context); // TODO don't force vue - it should be detected by ext point
            this.providersButton.push(callButton);
          })
        );
        
        this.createButtons();
      } catch (err) {
        log.error("Error building call buttons", err);
      }
      //}
    } catch (err) {
      log.error("Error getting call context", err);
    }
  },
  methods: {
    async initProvidersButton__donotuse() { // TODO do we needit actually? it is not reusable
      const thevue = this;
      await Promise.all(
        thevue.providers.map(async p => { // TODO async here???!
          if (await p.isInitialized) { // TODO await for boolean property??
            const callButton = await p.callButton(this.callContext);
            thevue.providersButton.push(callButton);
          }
        })
      );
    },
    createButtons() {
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
          const callButton = this.$refs.callbutton;
          callButton.classList.add("single");
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
      //}
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
  #call-button-container {
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
      padding: 0 10px;
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
  #call-button-container.single:hover,
  [id^="call-button-container-"]:hover,
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
    //       // #call-button-container.single:hover,
    //       // [id^="call-button-container-"]:hover,
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