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
  <!-- One visio, with exactly one primary action. The state chip says what is
       actually true of the room: live means somebody is in it, not that the
       calendar says it should have started. -->
  <v-card
    class="mb-3 pa-3"
    outlined
    flat>
    <div class="d-flex align-center justify-space-between">
      <v-chip
        small
        :color="color"
        outlined>
        <v-icon
          size="12"
          class="me-1"
          :color="color">
          {{ icon }}
        </v-icon>
        {{ stateLabel }}
      </v-chip>
      <span class="text-caption text-sub-title">{{ timeLabel }}</span>
    </div>
    <div class="text-body font-weight-bold mt-2 text-truncate">{{ title }}</div>
    <div v-if="showProgress" class="mt-2">
      <v-progress-linear
        :value="percent"
        :color="color"
        height="4"
        rounded />
      <div class="text-caption text-sub-title mt-1">{{ progressLabel }}</div>
    </div>
    <div
      v-else-if="countdown"
      class="text-caption text-sub-title mt-2">
      <v-icon size="12" class="me-1">fa-clock</v-icon>
      {{ countdownLabel }}
    </div>
    <div class="d-flex align-center justify-space-between mt-3">
      <a
        v-if="eventLink"
        :href="eventLink"
        class="text-caption">{{ $t('visio.drawer.openEvent') }}</a>
      <span v-else></span>
      <v-btn
        small
        depressed
        color="primary"
        :loading="joining"
        @click="join">
        <v-icon size="14" class="me-1">fa-video</v-icon>
        {{ $t('visio.drawer.join') }}
      </v-btn>
    </div>
    <div v-if="joinError" class="text-caption error--text mt-1">
      {{ $t('visio.drawer.join.unavailable') }}
    </div>
  </v-card>
</template>

<script>
import {LIVE, NOW} from '../js/VisioMerge.js';
import {formatTime, formatDay, isSameDay, elapsedPercent, splitDuration} from '../js/VisioFormat.js';

export default {
  props: {
    entry: {
      type: Object,
      default: null,
    },
    now: {
      type: Date,
      default: null,
    },
    countdown: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    joining: false,
    joinError: false,
  }),
  computed: {
    title() {
      return this.entry.title || this.$t('visio.drawer.untitled');
    },
    color() {
      return this.entry.state === LIVE && 'success'
          || this.entry.state === NOW && 'warning'
          || 'primary';
    },
    icon() {
      return this.entry.state === LIVE && 'fa-video'
          || this.entry.state === NOW && 'fa-user-clock'
          || 'fa-clock';
    },
    stateLabel() {
      return this.$t(`visio.drawer.state.${this.entry.state}`);
    },
    timeLabel() {
      const start = formatTime(this.entry.start);
      if (!this.entry.end) {
        return start && this.$t('visio.drawer.startedAt', {0: start}) || '';
      }
      const range = this.$t('visio.drawer.timeRange', {0: start, 1: formatTime(this.entry.end)});
      return isSameDay(this.entry.start, this.now) && range
          || this.$t('visio.drawer.dayTimeRange', {0: formatDay(this.entry.start), 1: range});
    },
    showProgress() {
      return this.entry.state === LIVE && !!this.entry.start && !!this.entry.end;
    },
    percent() {
      return elapsedPercent(this.entry.start, this.entry.end, this.now);
    },
    progressLabel() {
      return this.$t('visio.drawer.progress', {0: formatTime(this.entry.start), 1: this.percent});
    },
    countdownLabel() {
      const delay = splitDuration(this.entry.start.getTime() - this.now.getTime());
      const duration = delay.days && this.$t('visio.drawer.duration.dayHour', {0: delay.days, 1: delay.hours})
          || delay.hours && this.$t('visio.drawer.duration.hourMinute', {0: delay.hours, 1: delay.minutes})
          || delay.minutes && this.$t('visio.drawer.duration.minute', {0: delay.minutes})
          || this.$t('visio.drawer.duration.soon');
      return this.$t('visio.drawer.countdown', {0: duration});
    },
    eventLink() {
      // The event a visio was scheduled from: its description, its attendees,
      // its attachments — the context a bare join link loses. An occurrence of a
      // recurrent series has no id of its own, so it is addressed by its parent
      // plus its own date, the way agenda's own notifications link to it.
      if (!this.entry.eventId) {
        return '';
      }
      const site = eXo.env.portal.portalName || eXo.env.portal.siteKeyName;
      const target = this.entry.occurrenceId
          && `parentId=${this.entry.eventId}&occurrenceId=${encodeURIComponent(this.entry.occurrenceId)}`
          || `eventId=${this.entry.eventId}`;
      return `${eXo.env.portal.context}/${site}/agenda?${target}`;
    },
  },
  methods: {
    /**
     * Opens the meeting room.
     * <p>
     * A scheduled visio is joined through the very URL agenda stored, which is
     * what agenda's own Join button opens. An ongoing call that matches no
     * event only exists as a call id, so its URL is asked from its provider at
     * that moment; when no provider answers, the card says so rather than
     * opening nothing.
     *
     * @returns {void}
     */
    join() {
      this.joinError = false;
      if (this.entry.url) {
        window.open(this.entry.url, '_blank', 'noopener');
        return;
      }
      this.joining = true;
      this.$visioService.getWebConferencing()
        .then(core => this.$visioService.getCallUrl(core, this.entry.providerType, this.entry.callId))
        .then(url => {
          if (url) {
            window.open(url, '_blank', 'noopener');
          } else {
            this.joinError = true;
          }
        })
        .catch(() => this.joinError = true)
        .finally(() => this.joining = false);
    },
  },
};
</script>
