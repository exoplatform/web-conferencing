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
  <!-- The Meeting block of a past event, contributed to the agenda drawer's
       AgendaEvent / agenda-event-body extension point. -->
  <div
    v-if="visible"
    class="webconferencing-meeting-recordings d-flex flex-grow-0 flex-shrink-1 pb-5">
    <v-icon size="20" class="icon-default-color pe-4 align-self-start mt-1">fas fa-film</v-icon>
    <div class="d-flex flex-column flex-grow-1 flex-shrink-1 overflow-hidden">
      <div class="d-flex align-center">
        <span class="text-body font-weight-bold">{{ $t('visio.recordings.title') }}</span>
        <v-spacer />
        <v-btn
          :title="$t('visio.recordings.refresh')"
          :loading="loading"
          class="webconferencing-meeting-recordings-refresh"
          icon
          small
          @click="load">
          <v-icon size="14">fas fa-redo</v-icon>
        </v-btn>
      </div>
      <v-progress-linear
        v-if="loading && !state"
        indeterminate
        height="2"
        class="mt-1" />
      <template v-else-if="state === STATE_READY">
        <meeting-recording-item
          v-for="recording in recordings"
          :key="recording.documentId"
          :recording="recording"
          :transcript="transcripts[recording.documentId]"
          @retry="retryTranscript" />
      </template>
      <span
        v-else-if="message"
        class="webconferencing-meeting-recordings-message text-subtitle mt-1">
        {{ message }}
      </span>
      <a
        v-if="seriesLinkVisible"
        class="webconferencing-meeting-recordings-series text-caption primary--text mt-1"
        @click="toggleSeries">
        {{ wholeSeries && $t('visio.recordings.occurrenceOnly') || $t('visio.recordings.allOfSeries') }}
      </a>
      <!--
        The extension point for actions on the meeting (the enterprise AI row):
        app MeetingRecordings, type meeting-actions, the contributors receive
        the props event and recordings. Rebuilt on every load, so a
        contributor's isEnabled sees the recordings just read.
      -->
      <extension-registry-components
        v-if="actionsVisible"
        :key="loadCount"
        :params="actionParams"
        name="MeetingRecordings"
        type="meeting-actions"
        parent-element="div"
        element="div"
        class="webconferencing-meeting-actions d-flex flex-wrap mt-2" />
    </div>
  </div>
</template>

<script>
import {
  STATE_READY,
  STATE_NOT_RECORDED,
  STATE_PROCESSING,
  STATE_OTHER_PROVIDER,
  STATE_NOT_MEMBER,
  STATE_FORBIDDEN,
  STATE_ERROR,
  getConferenceUrl,
  getRecordingsWindow,
  getSpaceName,
  isRecurring,
  stateFromResponse,
} from '../js/MeetingRecordingsModel.js';
import {getRecordingsByConference, getTranscriptionState, retryTranscription} from '../services.js';

export default {
  props: {
    event: {
      type: Object,
      default: null,
    },
    isPast: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    STATE_READY,
    state: null,
    recordings: [],
    transcripts: {},
    loading: false,
    wholeSeries: false,
    loadCount: 0,
    requestId: 0,
  }),
  computed: {
    /**
     * The event's conference URL.
     *
     * @returns {String} the URL, or null without a conference
     */
    conferenceUrl() {
      return getConferenceUrl(this.event);
    },
    /**
     * Whether the block shows at all: a past occurrence with a conference.
     * Before the end of the meeting nothing is shown, and nothing is asked.
     *
     * @returns {Boolean} true when the block is shown
     */
    visible() {
      return !!(this.isPast && this.conferenceUrl);
    },
    /**
     * Whether the event is an occurrence of a recurring series.
     *
     * @returns {Boolean} true for a recurring event
     */
    recurring() {
      return isRecurring(this.event);
    },
    /**
     * The time window the recordings are asked for.
     *
     * @returns {Object} {from, to}, or an empty object for every recording
     */
    recordingsWindow() {
      return getRecordingsWindow(this.event, this.wholeSeries);
    },
    /**
     * What identifies one lookup: when it changes (another event or
     * occurrence opened, the series toggled, the meeting just ended) the
     * recordings are read again.
     *
     * @returns {String} the lookup key, or null when nothing is to be read
     */
    lookupKey() {
      return this.visible
        && JSON.stringify([this.conferenceUrl, this.recordingsWindow.from || '', this.recordingsWindow.to || ''])
        || null;
    },
    /**
     * The sentence shown in place of the recordings.
     *
     * @returns {String} the translated message, or null
     */
    message() {
      switch (this.state) {
      case STATE_NOT_RECORDED:
        return this.$t('visio.recordings.notRecorded');
      case STATE_PROCESSING:
        return this.$t('visio.recordings.processing');
      case STATE_OTHER_PROVIDER:
        return this.$t('visio.recordings.otherProvider');
      case STATE_NOT_MEMBER: {
        const spaceName = getSpaceName(this.event);
        return spaceName
          ? this.$t('visio.recordings.notSpaceMember', {0: spaceName})
          : this.$t('visio.recordings.notSpaceMemberUnnamed');
      }
      case STATE_FORBIDDEN:
        return this.$t('visio.recordings.forbidden');
      case STATE_ERROR:
        return this.$t('visio.recordings.error');
      default:
        return null;
      }
    },
    /**
     * Whether the link between the occurrence's recordings and the whole
     * series' is offered: on a recurring event whose recordings could be read.
     *
     * @returns {Boolean} true when the link is shown
     */
    seriesLinkVisible() {
      return this.recurring
        && [STATE_READY, STATE_NOT_RECORDED, STATE_PROCESSING].indexOf(this.state) >= 0;
    },
    /**
     * Whether the meeting-actions extension point is rendered: once the
     * recordings have been read, even when there is none, so a contributor
     * decides what it offers without them.
     *
     * @returns {Boolean} true when the action slot is rendered
     */
    actionsVisible() {
      return !this.loading
        && [STATE_READY, STATE_NOT_RECORDED, STATE_PROCESSING].indexOf(this.state) >= 0;
    },
    /**
     * The params handed to the meeting-actions contributors.
     *
     * @returns {Object} the event and the recordings shown
     */
    actionParams() {
      return {
        event: this.event,
        recordings: this.recordings,
      };
    },
  },
  watch: {
    lookupKey: {
      immediate: true,
      handler(key, previousKey) {
        if (key !== previousKey) {
          this.reset();
          // Whatever is still on its way belongs to the previous lookup
          this.requestId++;
          if (key) {
            this.load();
          }
        }
      },
    },
    event() {
      // Another event or occurrence opened: back to its own recordings
      this.wholeSeries = false;
    },
  },
  methods: {
    /**
     * Forgets what the previous lookup showed.
     *
     * @returns {void}
     */
    reset() {
      this.state = null;
      this.recordings = [];
      this.transcripts = {};
    },
    /**
     * Reads the recordings of the meeting, then their transcript states. Only
     * the answer to the latest request is shown: one to an earlier request,
     * for another lookup or before a refresh, is dropped.
     *
     * @returns {Promise} resolved once the recordings are shown
     */
    load() {
      if (!this.lookupKey) {
        return Promise.resolve();
      }
      const requestId = ++this.requestId;
      this.loading = true;
      return getRecordingsByConference(this.conferenceUrl, this.recordingsWindow)
        .then(({status, body}) => {
          if (requestId !== this.requestId) {
            return;
          }
          this.state = stateFromResponse(status, body, this.event, Date.now());
          this.recordings = this.state === STATE_READY ? body : [];
          this.transcripts = {};
          this.loadTranscripts(requestId);
        })
        .catch(() => {
          if (requestId === this.requestId) {
            this.state = STATE_ERROR;
            this.recordings = [];
          }
        })
        .finally(() => {
          if (requestId === this.requestId) {
            this.loading = false;
            this.loadCount++;
          }
        });
    },
    /**
     * Reads the transcript state of each recording. The first one is read
     * alone: where no transcriber is installed it says so, and the others are
     * not asked.
     *
     * @param {Number} requestId - the request the recordings were read by
     * @returns {Promise} resolved once every state is read
     */
    loadTranscripts(requestId) {
      const [first, ...others] = this.recordings;
      if (!first) {
        return Promise.resolve();
      }
      return this.loadTranscript(first.documentId, requestId)
        .then(state => state && state.available === true
          && Promise.all(others.map(recording => this.loadTranscript(recording.documentId, requestId))));
    },
    /**
     * Reads the transcript state of one recording into the block.
     *
     * @param {String} documentId - the recording's document id
     * @param {Number} requestId - the request the recording was read by
     * @returns {Promise<Object>} the state, or null
     */
    loadTranscript(documentId, requestId) {
      return getTranscriptionState(documentId)
        .then(state => {
          if (requestId === this.requestId && state) {
            this.$set(this.transcripts, documentId, state);
          }
          return state;
        });
    },
    /**
     * Asks for a failed transcription to run again, then shows its new state.
     *
     * @param {String} documentId - the recording's document id
     * @returns {Promise} resolved once the new state is shown
     */
    retryTranscript(documentId) {
      const requestId = this.requestId;
      return retryTranscription(documentId)
        .then(() => this.loadTranscript(documentId, requestId))
        .catch(() => this.$root.$emit('alert-message', this.$t('visio.recordings.transcript.retryFailed'), 'error'));
    },
    /**
     * Switches between the recordings of the occurrence opened and those of
     * the whole series.
     *
     * @returns {void}
     */
    toggleSeries() {
      this.wholeSeries = !this.wholeSeries;
    },
  },
};
</script>
