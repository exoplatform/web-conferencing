<!--
Copyright (C) 2026 eXo Platform SAS.

This is free software; you can redistribute it and/or modify it
under the terms of the GNU Lesser General Public License as
published by the Free Software Foundation; either version 2.1 of
the License, or (at your option) any later version.

This software is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this software; if not, write to the Free
Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
02110-1301 USA, or see the FSF site: http://www.fsf.org.
-->
<template>
  <v-app
    role="main"
    id="visioQuickAction">
    <visio-drawer />
  </v-app>
</template>

<script>
export default {
  mounted() {
    // The single way in: extensions.js dispatches this once the app is mounted,
    // whether the click came from the applications launcher or from the public
    // 'open-visio-drawer' event another app fired.
    document.addEventListener('quick-action-visio-drawer', this.openDrawer);
  },
  beforeDestroy() {
    document.removeEventListener('quick-action-visio-drawer', this.openDrawer);
  },
  methods: {
    /**
     * Opens the visio drawer.
     *
     * @returns {void}
     */
    openDrawer() {
      this.$root.$emit('visio-drawer-open');
    },
  },
};
</script>
