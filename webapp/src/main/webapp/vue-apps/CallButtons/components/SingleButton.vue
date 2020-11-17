<template>
  <div ref="singlebtn" class="single-btn-container" @click.prevent.self="selectProvider"></div>
</template>

<script>
export default {
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