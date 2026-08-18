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
      <!-- The title does the one thing that makes sense for the state it is
           in. Where joining is on offer it joins, matching the button beside
           it. Where it is not — a meeting still days away — it opens the event
           instead, which is safe: entering a room marks its call started, and a
           title that joined a meeting next week would hand back exactly the
           mis-click the join guard exists to prevent. With neither, it is plain
           text and looks it; a heading that cannot be clicked must not invite
           the click. -->
      <v-tooltip bottom>
        <template #activator="{on, attrs}">
          <a
            v-if="canJoin"
            v-bind="attrs"
            class="text-body font-weight-bold mt-2 text-truncate d-block text-color"
            href="#"
            v-on="on"
            @click.prevent="join">{{ title }}</a>
          <a
            v-else-if="eventLink"
            v-bind="attrs"
            :href="eventLink"
            class="text-body font-weight-bold mt-2 text-truncate d-block text-color"
            v-on="on">{{ title }}</a>
          <div
            v-else
            v-bind="attrs"
            class="text-body font-weight-bold mt-2 text-truncate"
            v-on="on">{{ title }}</div>
        </template>
        <span>{{ titleHint }}</span>
      </v-tooltip>
      <!-- How long it has been running, not how far through it is. A meeting's
           end time is a plan rather than a fact: they overrun, and a progress
           bar is wrong exactly when someone most wants to know. The accent
           stripe already carries the "this is live" signal a bar would repeat. -->
      <div
        v-if="showProgress && hasFooterContent"
        class="text-caption text-sub-title mt-2">
        <v-icon size="12" class="me-1">fa-circle-play</v-icon>
        {{ progressLabel }}
      </div>
      <div
        v-else-if="countdown && hasFooterContent"
        class="text-caption text-sub-title mt-2">
        <v-icon size="12" class="me-1">fa-clock</v-icon>
        {{ countdownLabel }}
      </div>
      <div class="d-flex align-center justify-space-between mt-3">
        <!-- Who is in there, where the eye lands first. Two faces already
             inside is a different invitation from a green chip, and it is the
             thing that actually decides whether somebody joins.

             eXo people go through the platform's own avatars list, the same
             component the task cards use, so they get the avatar, the popover
             and the sizing everyone else has. Guests cannot: that component
             resolves a profile by username, and a guest has no profile to
             resolve — it would ask the server for one and come back with
             nothing. They are counted instead, which is also the more useful
             fact about them. -->
        <!-- With nobody inside and no join to offer, the meta line moves in here
             rather than standing on a row of its own: otherwise the overflow
             button is marooned on an empty line and the card grows a band of
             white space for nothing. -->
        <div
          v-if="!hasFooterContent"
          class="text-caption text-sub-title">
          <v-icon size="12" class="me-1">{{ showProgress && 'fa-circle-play' || 'fa-clock' }}</v-icon>
          {{ showProgress && progressLabel || countdownLabel }}
        </div>
        <div v-else-if="people.length" class="d-flex align-center">
          <exo-user-avatars-list
            v-if="members.length"
            :users="members"
            :max="3"
            :icon-size="22"
            :margin-left="members.length > 1 && 'ml-n2' || ''"
            :compact="members.length > 1"
            clickable="'false'"
            retrieve-extra-information />
          <span class="text-caption text-sub-title ms-2">{{ peopleLabel }}</span>
        </div>
        <!-- And when nobody is in it, say so. An empty room reads as broken
             otherwise: the card announces itself ready, and shows nothing at
             all about the one thing the opener is waiting for. -->
        <div
          v-else-if="showsEmptyRoom"
          class="d-flex align-center text-caption text-sub-title">
          <v-icon size="12" class="me-1">fa-user-slash</v-icon>
          {{ $t('visio.drawer.nobodyIn') }}
        </div>
        <span v-else></span>
        <div class="d-flex align-center">
          <!-- Filled only where joining is the thing to do now. An upcoming
               meeting's room is joinable too, but almost nobody wants to, and
               giving every card the same weight makes the live one no easier to
               find than the rest. -->
          <v-btn
            v-if="canJoin"
            small
            :depressed="prominent"
            :text="!prominent"
            :color="prominent && 'primary' || ''"
            :loading="joining"
            @click="join">
            <v-icon size="14" class="me-1">fa-video</v-icon>
            {{ joinLabel }}
          </v-btn>
          <!-- Secondary actions live behind the overflow, named rather than
               drawn. A cross said "delete" for something that only ever removes
               the room from your own list — the meeting carries on without you,
               and no icon can say that. Words can. -->
          <v-menu v-if="hasMenu" offset-y>
            <template #activator="{on, attrs}">
              <v-btn
                v-bind="attrs"
                icon
                small
                :aria-label="$t('visio.drawer.moreActions')"
                v-on="on">
                <v-icon size="14">fas fa-ellipsis-v</v-icon>
              </v-btn>
            </template>
            <v-list class="pa-0" dense>
              <v-list-item v-if="eventLink" :href="eventLink">
                <v-list-item-icon class="me-2 my-2">
                  <v-icon size="16">fas fa-calendar-day</v-icon>
                </v-list-item-icon>
                <v-list-item-title>
                  {{ $t('visio.drawer.openEvent') }}
                </v-list-item-title>
              </v-list-item>
              <v-list-item v-if="entry.shareUrl" @click="copyLink">
                <v-list-item-icon class="me-2 my-2">
                  <v-icon size="16">{{ copied && 'fas fa-check' || 'fas fa-link' }}</v-icon>
                </v-list-item-icon>
                <v-list-item-title>
                  {{ copied && $t('visio.instant.copied') || $t('visio.drawer.copyLink') }}
                </v-list-item-title>
              </v-list-item>
              <v-list-item v-if="entry.shareUrl" @click="sendByMail">
                <v-list-item-icon class="me-2 my-2">
                  <v-icon size="16">fas fa-envelope</v-icon>
                </v-list-item-icon>
                <v-list-item-title>
                  {{ $t('visio.drawer.sendByMail') }}
                </v-list-item-title>
              </v-list-item>
              <v-list-item v-if="chatAvailable && entry.shareUrl" @click="sendByChat">
                <v-list-item-icon class="me-2 my-2">
                  <v-icon size="16">fas fa-comment</v-icon>
                </v-list-item-icon>
                <v-list-item-title>
                  {{ $t('visio.drawer.sendByChat') }}
                </v-list-item-title>
              </v-list-item>
              <v-list-item v-if="deletable" @click="$refs.deleteConfirm.open()">
                <v-list-item-icon class="me-2 my-2">
                  <v-icon size="16">fas fa-trash</v-icon>
                </v-list-item-icon>
                <v-list-item-title>
                  {{ $t('visio.instant.delete') }}
                </v-list-item-title>
              </v-list-item>
            </v-list>
          </v-menu>
        </div>
      </div>
      <div v-if="joinError" class="text-caption error--text mt-1">
        {{ $t('visio.drawer.join.unavailable') }}
      </div>
      <div v-if="mailError" class="text-caption error--text mt-1">
        {{ $t('visio.drawer.sendByMail.unavailable') }}
      </div>
      <!-- A confirm, because the whole point of this feature is that the link
           travels before anyone arrives: "nobody joined yet" is not the same as
           "nobody is about to", and the person most likely to press this is the
           one who just mailed the link to a customer. -->
      <exo-confirm-dialog
        ref="deleteConfirm"
        :title="$t('visio.instant.delete')"
        :message="$t('visio.instant.delete.confirm')"
        :ok-label="$t('visio.instant.delete')"
        :cancel-label="$t('visio.instant.delete.cancel')"
        @ok="destroy" />
    </v-card>
  </v-hover>
</template>

<script>
import {LIVE, NOW, READY, UPCOMING} from '../js/VisioMerge.js';
import {formatTime, formatDay, isSameDay, splitDuration} from '../js/VisioFormat.js';
import {copyText} from '../js/VisioClipboard.js';

/**
 * How close an upcoming meeting must be before joining it is offered. Arriving
 * a quarter of an hour early is intent; a button on next week's meeting is a
 * mis-click waiting to announce a meeting that is not happening.
 */
const JOIN_AHEAD_MS = 15 * 60 * 1000;

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
    copied: false,
    mailError: false,
    confirmDelete: false,
    deleteError: false,
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
      // Ready shares live's green: a room you opened is standing open, which
      // belongs to the same family as one people are in. It stays the quieter
      // of the two — no accent stripe, and the chip is outlined like every
      // other — so "somebody is actually in it" remains its own answer rather
      // than becoming a shade of green nobody can tell apart.
      return this.entry.state === LIVE && 'success'
          || this.entry.state === NOW && 'warning'
          || this.entry.state === READY && 'success'
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
      const theme = this.$vuetify && this.$vuetify.theme;
      const success = theme && theme.currentTheme && theme.currentTheme.success || '#2eb58c';
      if (this.entry.state === LIVE) {
        // Live is the loudest: the whole outline greens, and the left edge
        // thickens into a stripe you can find without reading the card.
        return {borderColor: success, borderLeft: `4px solid ${success}`};
      }
      if (this.entry.state === READY) {
        // A room standing open is the same family, one step quieter: the
        // outline greens, the stripe does not. Somebody being in it stays a
        // distinction the eye can make.
        return {borderColor: success};
      }
      if (this.entry.state === NOW) {
        // Scheduled for this minute with nobody in it: the outline carries the
        // same amber the chip does, so the card reads at a glance instead of
        // saying one thing in colour and another in words.
        const warning = theme && theme.currentTheme && theme.currentTheme.warning || '#ffb441';
        return {borderColor: warning};
      }
      return {};
    },
    icon() {
      return this.entry.state === LIVE && 'fa-video'
          || this.entry.state === NOW && 'fa-user-clock'
          || this.entry.state === READY && 'fa-door-open'
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
    /**
     * Whether the card has any secondary action worth an overflow menu.
     *
     * @returns {Boolean} true when the menu should be offered
     */
    hasMenu() {
      return !!this.entry.shareUrl || this.deletable || !!this.eventLink;
    },
    /**
     * The people actually in the room right now.
     *
     * @returns {Array} joined participants, possibly empty
     */
    /**
     * Whether the chat add-on is on this page to receive a share.
     *
     * The share travels as a document event, so dispatching it where nothing
     * listens fails silently — worse than not offering it. Matrix registers its
     * constants on the Vue prototype when its app mounts, which is the honest
     * signal that something is listening.
     *
     * @returns {Boolean} true when the chat can be handed a link
     */
    chatAvailable() {
      return !!(window.Vue && window.Vue.prototype && window.Vue.prototype.$matrixService);
    },
    /**
     * Whether to say that nobody is inside.
     *
     * Only where somebody could be: a room standing open, or a call the
     * schedule says is happening. On a meeting next week it is not news.
     *
     * @returns {Boolean} true when the empty-room line belongs
     */
    showsEmptyRoom() {
      return !this.people.length && (this.entry.state === READY || this.entry.state === NOW);
    },
    people() {
      return this.entry.people || [];
    },
    /**
     * At most three faces; the rest become a count.
     *
     * @returns {Array} the participants to draw
     */
    /**
     * The eXo people inside, in the shape the platform's avatars list wants:
     * it renders exo-user-avatar by profile id, so a username is the whole
     * contract.
     *
     * @returns {Array} the members, possibly empty
     */
    members() {
      return this.people
        .filter(person => person.type !== 'guest')
        .map(person => ({userName: person.id, fullname: person.title, ariaLabel: person.title}));
    },
    /**
     * How many of the people inside are guests — someone who followed the link
     * without an eXo account, and therefore has no profile to draw.
     *
     * @returns {Number} the guest count
     */
    guestCount() {
      return this.people.filter(person => person.type === 'guest').length;
    },
    /**
     * How many are inside, said in words.
     *
     * @returns {String} the translated count
     */
    peopleLabel() {
      return this.guestCount
          && this.$t('visio.drawer.peopleInWithGuests', {0: this.people.length, 1: this.guestCount})
          || this.$t('visio.drawer.peopleIn', {0: this.people.length});
    },
    /**
     * Whether this room can be destroyed outright.
     *
     * Only a room of your own that nobody has ever entered: with people inside
     * there is a meeting to interrupt, and on a visio that came from an agenda
     * event the room belongs to the event rather than to this list.
     *
     * @returns {Boolean} true when deleting is offered
     */
    deletable() {
      return !!this.entry.instant && this.entry.state === READY && !this.people.length;
    },
    /**
     * What the tooltip says the title will do, so the promise is legible before
     * the click rather than after it.
     *
     * @returns {String} the hint for this card's title
     */
    titleHint() {
      return this.canJoin && this.$t('visio.drawer.joinTitle', {0: this.title})
          || this.eventLink && this.$t('visio.drawer.openEventTitle', {0: this.title})
          || this.title;
    },
    /**
     * Whether the footer row already has something on its left: people inside,
     * or the line saying nobody is. When it does not, the meta line moves down
     * into it rather than leaving the overflow button alone on a row.
     *
     * @returns {Boolean} true when the footer has its own left-hand content
     */
    hasFooterContent() {
      return this.people.length > 0 || this.showsEmptyRoom;
    },
    prominent() {
      return this.entry.state === LIVE || this.entry.state === NOW;
    },
    /**
     * Whether joining is offered at all.
     *
     * Not a tidiness rule. Joining a meeting scheduled for tomorrow is not
     * merely useless, it has consequences: entering the room marks its call
     * started, which shows it live to everyone else in it and counts it in the
     * ongoing-visio badge. A mis-click on a card for next week would announce a
     * meeting that is not happening.
     *
     * So the button appears where joining is a real intention — a call in
     * progress, one scheduled for now, a room standing open — and on an
     * upcoming meeting only once it is close enough that arriving early is the
     * point rather than an accident. Sharing has no such cost, so the copy
     * affordance stays on every card regardless.
     *
     * @returns {Boolean} true when the join button should be offered
     */
    canJoin() {
      if (this.entry.state !== UPCOMING) {
        return true;
      }
      return !!this.entry.start
          && this.entry.start.getTime() - this.now.getTime() <= JOIN_AHEAD_MS;
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
    /**
     * Whether the viewer is one of the people already in the room.
     *
     * @returns {Boolean} true when the current user is joined
     */
    alreadyIn() {
      const me = eXo && eXo.env && eXo.env.portal && eXo.env.portal.userName;
      return !!me && this.people.some(person => person.id === me);
    },
    joinLabel() {
      // Already inside: the room is open in another tab, so the button brings
      // it back rather than pretending this is an arrival.
      return this.alreadyIn && this.$t('visio.drawer.backToRoom')
          || this.entry.state === NOW
          && this.$t('visio.drawer.join.first')
          || this.$t('visio.drawer.join');
    },
    timeLabel() {
      const start = formatTime(this.entry.start);
      if (this.entry.state === READY) {
        // A room you opened has no schedule at all: the only time it has is
        // when you opened it, and saying "started" about an empty room would
        // be the very confusion the drawer exists to remove.
        return start && this.$t('visio.drawer.openedAt', {0: start}) || '';
      }
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
     * Puts the share link in the clipboard.
     *
     * @returns {void}
     */
    copyLink() {
      copyText(this.entry.shareUrl).then(() => {
        this.copied = true;
        window.setTimeout(() => this.copied = false, 3000);
      }).catch(() => this.copied = false);
    },
    /**
     * Takes the room off this browser's list, leaving the call untouched.
     *
     * @returns {void}
     */
    /**
     * Destroys the room for everyone, once confirmed.
     *
     * @returns {void}
     */
    /**
     * Hands the link to the chat, which asks the user which conversation it
     * goes into. The payload is the shape the chat's own share action sends.
     *
     * @returns {void}
     */
    sendByChat() {
      document.dispatchEvent(new CustomEvent('meeds-chat-share', {
        detail: {link: {url: this.entry.shareUrl, title: this.title}},
      }));
    },
    /**
     * Opens the eXo mail composer on this link, the way Contacts opens it on a
     * card: the mail add-on's own modules are required first, so the composer
     * exists even on a page where the mailbox has never been opened.
     * <p>
     * The link goes to the clipboard in the same gesture, and that is a
     * workaround rather than a design. The composer's prefill carries
     * recipients only — its open() reads prefill.to and nothing else — so there
     * is no way from outside to seed a body with the link. Sending subject and
     * body anyway costs nothing and starts working the day the mail add-on
     * honours them; until then the user pastes, and is told so.
     *
     * @returns {void}
     */
    sendByMail() {
      this.mailError = false;
      this.$visioService.openMailComposer(
        this.$t('visio.drawer.sendByMail.subject', {0: this.title}),
        this.$t('visio.drawer.sendByMail.body', {0: this.entry.shareUrl}))
        .catch(() => this.mailError = true);
    },
    destroy() {
      this.$visioService.deleteInstantVisio(this.entry.callId, this.entry.providerType)
        .then(() => this.$root.$emit('visio-rooms-changed'))
        .catch(() => this.deleteError = true);
    },
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
