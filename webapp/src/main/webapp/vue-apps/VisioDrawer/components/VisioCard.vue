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
  <!-- One visio, with exactly one primary action. The state chip says what is
       actually true of the room: live means somebody is in it, not that the
       calendar says it should have started. -->
  <!-- The hover tint is a scanning aid, not a promise: the card itself is not
       a click target, its actions are. So it tints rather than lifts —
       elevation would advertise a whole-card click that does not exist. The
       class is the platform's own, the same one the mail drawer's rows use, so
       it follows the deployment's theme instead of hard-coding a grey. -->
  <v-hover v-slot="{hover}">
    <v-card
      class="mb-3 pa-3"
      :class="hover && 'light-grey-background-color' || ''"
      :style="accentStyle"
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
      <!-- The full title on hover: meeting titles are routinely longer than the
           drawer is wide, and the truncated half is often the half that says
           which meeting it actually is. -->
      <v-tooltip bottom>
        <template #activator="{on, attrs}">
          <div
            v-bind="attrs"
            class="text-body font-weight-bold mt-2 text-truncate"
            v-on="on">{{ title }}</div>
        </template>
        <span>{{ title }}</span>
      </v-tooltip>
      <!-- How long it has been running, not how far through it is. A meeting's
           end time is a plan rather than a fact: they overrun, and a progress
           bar is wrong exactly when someone most wants to know. The accent
           stripe already carries the "this is live" signal a bar would repeat. -->
      <div
        v-if="showProgress"
        class="text-caption text-sub-title mt-2">
        <v-icon size="12" class="me-1">fa-circle-play</v-icon>
        {{ progressLabel }}
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
        <!-- Filled only where joining is the thing to do now. An upcoming
             meeting's room is joinable too, but almost nobody wants to, and
             giving every card the same weight makes the live one no easier to
             find than the rest. -->
        <v-btn
          small
          :depressed="prominent"
          :text="!prominent"
          :color="prominent && 'primary' || ''"
          :loading="joining"
          @click="join">
          <v-icon size="14" class="me-1">fa-video</v-icon>
          {{ joinLabel }}
        </v-btn>
      </div>
      <div v-if="joinError" class="text-caption error--text mt-1">
        {{ $t('visio.drawer.join.unavailable') }}
      </div>
    </v-card>
  </v-hover>
</template>

<script>
import {LIVE, NOW} from '../js/VisioMerge.js';
import {formatTime, formatDay, isSameDay, splitDuration} from '../js/VisioFormat.js';

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
    /**
     * Colour of the state chip, its icon and the live progress bar.
     *
     * LIVE and NOW carry a colour because they are the two states worth
     * interrupting someone for. UPCOMING deliberately carries none and falls
     * back to the chip's default, which takes the theme's own text colour.
     *
     * It used to be "primary", which was wrong for two reasons. The visible
     * one: primary is brand-configurable, so on a theme whose primary is pale
     * an outlined chip drew near-white text on the white drawer and the label
     * disappeared. The quieter one: upcoming is the baseline state, the one
     * most rows are in, and colouring it competes for attention with the two
     * states that have actually earned it.
     *
     * @returns {String} a Vuetify colour name, or null to keep the default
     */
    color() {
      return this.entry.state === LIVE && 'success'
          || this.entry.state === NOW && 'warning'
          || null;
    },
    /**
     * The accent stripe down the left edge of a live card.
     *
     * The drawer exists to answer one question — is something happening right
     * now — and a small chip makes that a question you have to read. A stripe
     * makes it answerable in peripheral vision, which is the difference
     * between a list and a status.
     *
     * Live only, deliberately: giving the same weight to a call that is merely
     * scheduled for now would put the drawer back where it started. The colour
     * comes from the Vuetify theme rather than a literal, so it follows the
     * deployment's palette, and it is an inline style because this webapp's
     * webpack carries no CSS loader for component style blocks.
     *
     * @returns {Object} a style binding, empty for every state but live
     */
    accentStyle() {
      if (this.entry.state !== LIVE) {
        return {};
      }
      const theme = this.$vuetify && this.$vuetify.theme;
      const success = theme && theme.currentTheme && theme.currentTheme.success;
      return {borderLeft: `4px solid ${success || '#2eb58c'}`};
    },
    icon() {
      return this.entry.state === LIVE && 'fa-video'
          || this.entry.state === NOW && 'fa-user-clock'
          || 'fa-clock';
    },
    stateLabel() {
      return this.$t(`visio.drawer.state.${this.entry.state}`);
    },
    /**
     * Whether joining is the thing to do right now, which decides how loud the
     * button is: filled for a call in progress or one scheduled for this
     * moment, quiet for anything merely upcoming.
     *
     * @returns {Boolean} true when the join button should be prominent
     */
    prominent() {
      return this.entry.state === LIVE || this.entry.state === NOW;
    },
    /**
     * Label of the join button.
     *
     * A visio scheduled for now that nobody has joined is the one case where
     * the wording can do some work: somebody has to go first, and saying so
     * turns an awkward fact into an invitation.
     *
     * @returns {String} the translated label
     */
    joinLabel() {
      return this.entry.state === NOW
          && this.$t('visio.drawer.join.first')
          || this.$t('visio.drawer.join');
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
      return this.entry.state === LIVE && !!this.entry.start;
    },
    /**
     * How long the meeting has been running.
     *
     * Elapsed time rather than a percentage of the scheduled length: the end
     * time is what someone planned, not what will happen, so a proportion is
     * least trustworthy in the case people care about most — the meeting that
     * has run past its slot.
     *
     * @returns {String} the translated "started N ago" label
     */
    progressLabel() {
      const elapsed = splitDuration(this.now.getTime() - this.entry.start.getTime());
      const duration = elapsed.days && this.$t('visio.drawer.duration.dayHour', {0: elapsed.days, 1: elapsed.hours})
          || elapsed.hours && this.$t('visio.drawer.duration.hourMinute', {0: elapsed.hours, 1: elapsed.minutes})
          || elapsed.minutes && this.$t('visio.drawer.duration.minute', {0: elapsed.minutes})
          || this.$t('visio.drawer.duration.soon');
      return this.$t('visio.drawer.startedAgo', {0: duration});
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
