<template>
  <div ref="singlebtn" class="single-btn-container"></div>
</template>

<script>
export default {
  props: {
    providersbutton: {
      type: Array,
      required: true
    },
    createbuttons: {
      type: Function,
      required: true
    },
  },
//   data() {

//   },
updated() {
    // this.createbuttons()
    // console.log(this.providersbutton)
    this.$emit("updated")
  },
  methods: {
//     createOneButton(pb) {
//       //add a single button
//     //   for (const [index, pb] of this.providersbutton.entries()) {
          
//         const callButton = this.$refs.singlebtn;
//         console.log(this.$refs.singlebtn);
//         // this.$refs.callbutton.classList.add("single");
//         if (pb instanceof Vue) {
//           // add vue button
//           const vm = pb.$mount(); // TODO why we need vm globaly?
//           callButton.appendChild(vm.$el);
//         } else {
//           // add button from DOM Element
//           callButton.appendChild(pb.get(0));
//         }
//       }
//     // }
  }
}
</script>

<style>
</style>