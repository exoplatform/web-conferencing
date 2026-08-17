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
  <!-- The visios drawer: what is running now, what is about to. It is opened
       from the applications launcher, and refreshed on every occasion that can
       change the answer — opening, a call starting or ending, coming back to
       the tab — because a list of meetings that is not current is worse than no
       list at all. -->
  <exo-drawer
    id="visioDrawer"
    ref="visioDrawer"
    v-model="drawer"
    right
    :loading="loading"
    @closed="onClosed">
    <template #title>
      {{ $t('visio.drawer.title') }}
    </template>
    <template #titleIcons>
      <v-btn
        icon
        :aria-label="$t('visio.drawer.refresh')"
        :disabled="loading"
        @click="refresh">
        <v-icon size="18">fa-sync</v-icon>
      </v-btn>
    </template>
    <template #content>
      <div class="pa-4">
        <visio-empty-state
          v-if="error"
          error
          @retry="refresh" />
        <visio-empty-state v-else-if="!loading && !entries.length" />
        <template v-else>
          <visio-section
            v-for="section in sections"
            :key="section.id"
            :label="section.label"
            :entries="section.entries"
            :now="now"
            :countdown-key="countdownKey" />
        </template>
      </div>
    </template>
  </exo-drawer>
</template>

<script>
import {LIVE, NOW, UPCOMING} from '../js/VisioMerge.js';
import visioRefreshMixin from '../js/VisioRefreshMixin.js';

export default {
  mixins: [visioRefreshMixin],
  data: () => ({
    drawer: false,
    loading: false,
    error: false,
    entries: [],
  }),
  computed: {
    sections() {
      return [
        {id: LIVE, label: this.$t('visio.drawer.section.live'), entries: this.entriesOf(LIVE)},
        {id: NOW, label: this.$t('visio.drawer.section.now'), entries: this.entriesOf(NOW)},
        {id: UPCOMING, label: this.$t('visio.drawer.section.upcoming'), entries: this.entriesOf(UPCOMING)},
      ].filter(section => section.entries.length);
    },
    countdownKey() {
      // Only the very next meeting gets a countdown: on every card it is noise.
      const next = this.entriesOf(UPCOMING)[0];
      return next && next.key || '';
    },
  },
  created() {
    this.$root.$on('visio-drawer-open', this.open);
  },
  beforeDestroy() {
    this.$root.$off('visio-drawer-open', this.open);
  },
  methods: {
    /**
     * Opens the drawer on a freshly read list.
     *
     * @returns {void}
     */
    open() {
      this.drawer = true;
      this.startWatching();
      this.refresh();
    },
    /**
     * Reads both sources again and rebuilds the list.
     *
     * @returns {Promise} resolved once the list is up to date
     */
    refresh() {
      this.loading = true;
      this.now = new Date();
      return this.$visioService.getVisios(this.now)
        .then(entries => {
          this.entries = entries;
          this.error = false;
        })
        .catch(() => {
          // An error is shown as an error. The widget this replaces rendered a
          // failed fetch exactly like an empty calendar.
          this.entries = [];
          this.error = true;
        })
        .finally(() => this.loading = false);
    },
    /**
     * The entries in one state.
     *
     * @param {string} state - LIVE, NOW or UPCOMING
     * @returns {Array} the matching entries
     */
    entriesOf(state) {
      return this.entries.filter(entry => entry.state === state);
    },
    /**
     * Releases everything when the drawer closes.
     *
     * @returns {void}
     */
    onClosed() {
      this.drawer = false;
      this.stopWatching();
    },
  },
};
</script>
