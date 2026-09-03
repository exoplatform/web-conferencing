<!--
  Copyright (C) 2026 eXo Platform SAS.
 
  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License
  as published by the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.
 
  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  GNU Affero General Public License for more details.
 
  You should have received a copy of the GNU Affero General Public License
  along with this program. If not, see <gnu.org/licenses>.
 -->
<template>
  <!-- The room was opened before this panel appeared, so nothing here is a
       form to fill in: the link exists, and the only thing between the user and
       the person waiting for it is one click. Hence copy first and join second
       — the reason for opening a room on the fly is almost always somebody who
       is not in eXo at all. -->
  <v-card
    class="mb-4 pa-3 light-grey-background-color"
    outlined
    flat>
    <div class="d-flex align-center justify-space-between">
      <span class="subtitle-2 font-weight-bold">{{ $t('visio.instant.ready') }}</span>
      <v-btn
        icon
        small
        :aria-label="$t('visio.instant.close')"
        @click="$emit('close')">
        <v-icon size="16">fa-times</v-icon>
      </v-btn>
    </div>
    <div class="text-caption text-sub-title mt-3">{{ $t('visio.instant.link') }}</div>
    <v-text-field
      class="pt-0 mt-0"
      :value="room.shareUrl"
      readonly
      dense
      outlined
      hide-details
      @focus="selectAll" />
    <div class="d-flex align-center mt-3">
      <v-btn
        class="me-2"
        color="primary"
        depressed
        small
        @click="copy">
        <v-icon size="14" class="me-1">{{ copied && 'fa-check' || 'fa-link' }}</v-icon>
        {{ copied && $t('visio.instant.copied') || $t('visio.instant.copy') }}
      </v-btn>
      <v-btn
        small
        text
        @click="join">
        <v-icon size="14" class="me-1 icon-default-color">fa-video</v-icon>
        {{ $t('visio.instant.join') }}
      </v-btn>
    </div>
    <div v-if="copyError" class="text-caption error--text mt-1">
      {{ $t('visio.instant.copyFailed') }}
    </div>
    <!-- Both of these are things nobody discovers on their own, and both are
         things people report as bugs when they meet them the hard way: that the
         link works without an account, and that the room is not a permanent
         address but a place that closes when the last person walks out. -->
    <div class="text-caption text-sub-title mt-3">
      <v-icon size="12" class="me-1">fa-user-plus</v-icon>
      {{ $t('visio.instant.guests') }}
    </div>
    <div class="text-caption text-sub-title mt-1">
      <v-icon size="12" class="me-1">fa-hourglass-half</v-icon>
      {{ $t('visio.instant.lifetime') }}
    </div>
  </v-card>
</template>

<script>
import {copyText} from '../js/VisioClipboard.js';

export default {
  props: {
    room: {
      type: Object,
      default: null,
    },
  },
  data() {
    return {
      copied: false,
      copyError: false,
    };
  },
  watch: {
    room() {
      this.copied = false;
      this.copyError = false;
    },
  },
  methods: {
    /**
     * Puts the link in the clipboard, and says so.
     *
     * @returns {void}
     */
    copy() {
      this.copyError = false;
      copyText(this.room.shareUrl)
        .then(() => {
          this.copied = true;
          window.setTimeout(() => this.copied = false, 3000);
        })
        .catch(() => this.copyError = true);
    },
    /**
     * Opens the room in a new tab.
     * <p>
     * The plain room address, never the share link: the two differ by an
     * {@code inviteId}, and that parameter is what makes the platform admit
     * somebody as a guest. Following one's own invitation would register the
     * person who opened the room as a guest of it — no participant row of type
     * user, so the room counts for nobody and no badge is raised for the one
     * person certainly in it. The share link stays what it is for: other people.
     *
     * @returns {void}
     */
    join() {
      const own = this.room.url || this.room.shareUrl || '';
      const invite = own.indexOf('inviteId=');
      const cut = invite > 0 && own.lastIndexOf('?', invite) || -1;
      window.open(cut > 0 && own.substring(0, cut) || own, '_blank', 'noopener');
    },
    /**
     * Selects the whole link, so it can be copied by hand wherever the
     * clipboard is out of reach.
     *
     * @param {object} event - the focus event
     * @returns {void}
     */
    selectAll(event) {
      event.target.select();
    },
  },
};
</script>
