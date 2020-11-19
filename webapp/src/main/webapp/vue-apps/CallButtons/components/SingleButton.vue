<template>
  <div 
    ref="singlebtn" 
    class="single-btn-container" 
    @click.prevent.self="selectProvider">
  </div>
</template>

<script>
export default {
  props: {
  },
  // computed: {
  //   parentClass() {
  //     return Object.values(this.$refs.singlebtn.parentElement.parentElement.parentElement.parentElement.classList).join("");
  //   }
  // },
  methods: {
    selectProvider(event) {
      if (event && event.target && event.target.children[0]) {
        event.target.children[0].click(); // fire click on the provider button
      }
    }
  }
  // props: {
  // },
  // methods: {
  // }
};
</script>

<style>
</style>