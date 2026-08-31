<!--
Copyright (C) 2026 eXo Platform SAS.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
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
      <!-- Opening a room is an action on the drawer, not an item in it, so it
           lives in the header next to refresh. It creates first and asks
           nothing: the whole point of an on-the-fly room is that somebody is
           already waiting for the link. -->
      <v-tooltip bottom>
        <template #activator="{on, attrs}">
          <v-btn
            v-bind="attrs"
            icon
            :aria-label="$t('visio.instant.start')"
            :disabled="creating || loading"
            :loading="creating"
            v-on="on"
            @click="createInstant">
            <v-icon size="18">fa-plus</v-icon>
          </v-btn>
        </template>
        <span>{{ $t('visio.instant.start') }}</span>
      </v-tooltip>
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
        <visio-instant-panel
          v-if="instantRoom"
          :room="instantRoom"
          @renamed="onRenamed"
          @close="instantRoom = null" />
        <div v-if="createError" class="text-caption error--text mb-4">
          {{ $t('visio.instant.failed') }}
        </div>
        <visio-empty-state
          v-if="error"
          error
          @retry="refresh" />
        <visio-empty-state v-else-if="!loading && !entries.length && !instantRoom" />
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
import {LIVE, NOW, READY, UPCOMING} from '../js/VisioMerge.js';
import visioRefreshMixin from '../js/VisioRefreshMixin.js';

export default {
  mixins: [visioRefreshMixin],
  data: () => ({
    drawer: false,
    loading: false,
    error: false,
    creating: false,
    createError: false,
    instantRoom: null,
    entries: [],
  }),
  computed: {
    sections() {
      return [
        {id: LIVE, label: this.$t('visio.drawer.section.live'), entries: this.entriesOf(LIVE)},
        {id: READY, label: this.$t('visio.drawer.section.ready'), entries: this.entriesOf(READY)},
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
    this.$root.$on('visio-rooms-changed', this.refresh);
  },
  beforeDestroy() {
    this.$root.$off('visio-drawer-open', this.open);
    this.$root.$off('visio-rooms-changed', this.refresh);
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
     * Opens a room on the spot and shows its link.
     * <p>
     * Nothing is asked first, on purpose. The name comes prefilled from who is
     * opening it, and stays editable in the panel: making somebody name a room
     * before they can get its link is exactly the delay this replaces.
     *
     * @returns {void}
     */
    createInstant() {
      this.creating = true;
      this.createError = false;
      this.$visioService.createInstantVisio(this.defaultRoomName())
        .then(room => {
          this.instantRoom = room;
          return this.refresh();
        })
        .catch(() => this.createError = true)
        .finally(() => this.creating = false);
    },
    /**
     * The name a room gets, since nobody is asked for one.
     * <p>
     * Numbered, because a person opening a second room while the first is still
     * listed needs to tell them apart — and the number they expect is the next
     * one, not a count. It is taken from the highest number already on the list
     * rather than from its length, so deleting the second of three rooms does
     * not make the next one collide with the third.
     *
     * @returns {String} the room name
     */
    defaultRoomName() {
      const portal = eXo && eXo.env && eXo.env.portal;
      const owner = portal && (portal.userFullName || portal.fullName || portal.userName) || '';
      const next = this.nextRoomNumber(owner);
      return owner && this.$t('visio.instant.defaultName', {0: owner, 1: next})
          || this.$t('visio.instant.defaultNameAnonymous', {0: next});
    },
    /**
     * The number the next room takes: one past the highest already used.
     *
     * @param {String} owner - the room owner's display name
     * @returns {Number} the number to give the new room
     */
    nextRoomNumber(owner) {
      const prefix = owner && this.$t('visio.instant.defaultName', {0: owner, 1: ''}) || '';
      const used = (this.entries || [])
        .filter(entry => entry.instant && entry.title)
        .map(entry => {
          const tail = prefix && entry.title.indexOf(prefix) === 0
            ? entry.title.slice(prefix.length)
            : entry.title;
          const found = /(\d+)\s*$/.exec(tail);
          return found && parseInt(found[1], 10) || 0;
        });
      return used.length && Math.max.apply(null, used) + 1 || 1;
    },
    /**
     * Reflects a renamed room in the list underneath the panel.
     *
     * @param {Object} room - the renamed room
     * @returns {void}
     */
    onRenamed(room) {
      this.instantRoom = room;
      this.refresh();
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
      this.instantRoom = null;
      this.createError = false;
      this.stopWatching();
    },
  },
};
</script>
