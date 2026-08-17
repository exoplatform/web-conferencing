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
  <!-- Sharing a room again should never mean opening a new one, so the link
       stays one click away for the whole life of the card. The button reports
       what happened rather than pretending: the clipboard API does not exist on
       an insecure origin, and the fallback can be refused too. -->
  <v-tooltip bottom>
    <template #activator="{on, attrs}">
      <v-btn
        v-bind="attrs"
        icon
        small
        :aria-label="label"
        v-on="on"
        @click="copy">
        <v-icon size="14" :color="failed && 'error' || ''">{{ icon }}</v-icon>
      </v-btn>
    </template>
    <span>{{ label }}</span>
  </v-tooltip>
</template>

<script>
import {copyText} from '../js/VisioClipboard.js';

export default {
  props: {
    url: {
      type: String,
      default: '',
    },
  },
  data: () => ({
    copied: false,
    failed: false,
  }),
  computed: {
    icon() {
      return this.copied && 'fa-check' || 'fa-link';
    },
    label() {
      return this.failed && this.$t('visio.drawer.copyFailed')
          || this.copied && this.$t('visio.drawer.copied')
          || this.$t('visio.drawer.copyLink');
    },
  },
  methods: {
    /**
     * Copies the link and shows, on the button itself, whether it worked.
     *
     * @returns {void}
     */
    copy() {
      this.failed = false;
      copyText(this.url)
        .then(() => {
          this.copied = true;
          window.setTimeout(() => this.copied = false, 3000);
        })
        .catch(() => this.failed = true);
    },
  },
};
</script>
