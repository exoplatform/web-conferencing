<template>
  <div 
    ref="singlebtn" 
    class="single-btn-container" 
    @click="selectProvider">
  </div>
</template>

<script>
export default {
  props: {
  },
  methods: {
    selectProvider(event) {
      if (event && event.target && event.target.children[0]) {
        event.target.children[0].click(); // fire click on the provider button
      }
    }
  }
};
</script>

<style>
</style>