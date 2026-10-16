<!--
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
-->
<template>
  <user-notification-template
    :notification="notification"
    :avatar-url="avatarUrl"
    :message="message"
    :url="url">
    <template v-if="actionLabel" #actions>
      <v-btn
        :loading="acting"
        class="ignore-vuetify-classes"
        color="primary"
        elevation="0"
        small
        outlined
        @click.stop.prevent="act">
        <v-icon size="14" class="me-2 pt-2px">fa-solid fa-video</v-icon>
        <span class="text-none">{{ actionLabel }}</span>
      </v-btn>
    </template>
  </user-notification-template>
</template>

<script>
import {getWebConferencing} from '../../VisioDrawer/services.js';

const STARTED = 'started';
const ENDED = 'stopped';
const CALL_STATE_TIMEOUT_MS = 8000;
const PROVIDER_TIMEOUT_MS = 3000;

export default {
  props: {
    notification: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    callState: null,
    acting: false,
  }),
  computed: {
    parameters() {
      return this.notification && this.notification.parameters || {};
    },
    callId() {
      return this.parameters.CALL_ID;
    },
    providerType() {
      return this.parameters.PROVIDER_TYPE;
    },
    groupCall() {
      return this.parameters.CALL_TYPE !== 'user';
    },
    caller() {
      return this.notification && this.notification.from;
    },
    callerName() {
      return this.caller && this.caller.fullname || this.parameters.CALLER_ID;
    },
    avatarUrl() {
      return this.groupCall && this.parameters.AVATAR || this.caller && this.caller.avatar;
    },
    url() {
      return !this.groupCall && `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/profile/${this.parameters.CALLER_ID}` || null;
    },
    ended() {
      return this.groupCall && this.callState === ENDED;
    },
    message() {
      const callerName = `<a class="user-name font-weight-bold">${this.escapeHtml(this.callerName)}</a>`;
      if (!this.groupCall) {
        return this.$t('Notification.webconferencing.missedcall.user', {0: callerName});
      }
      const message = this.$t('Notification.webconferencing.missedcall.group', {
        0: callerName,
        1: `<span class="font-weight-bold">${this.escapeHtml(this.parameters.CALL_OWNER)}</span>`,
      });
      return this.ended && `${message}<br>${this.$t('Notification.webconferencing.missedcall.ended')}` || message;
    },
    actionLabel() {
      if (!this.groupCall) {
        return this.$t('Notification.webconferencing.missedcall.callAgain');
      }
      return this.callState === STARTED && this.$t('Notification.webconferencing.missedcall.join') || null;
    },
  },
  created() {
    if (this.groupCall) {
      // The state of a group call is read when the notification is shown, not when it was sent
      this.readCallState().then(state => this.callState = state);
    }
  },
  methods: {
    act() {
      // The call window is opened on the click itself, or the browser blocks it: the connector then reuses it
      const callWindow = window.open('', `${this.providerType}-${this.callId}`);
      this.acting = true;
      const action = this.groupCall && this.join(callWindow) || this.callAgain();
      return action
        .catch(() => {
          this.closeWindow(callWindow);
          this.alert(this.$t('Notification.webconferencing.missedcall.unavailable'), 'error');
        })
        .finally(() => this.acting = false);
    },
    join(callWindow) {
      // The call may have ended since the notification was shown
      return this.readCallState().then(state => {
        this.callState = state;
        if (state === ENDED) {
          this.closeWindow(callWindow);
          this.alert(this.$t('Notification.webconferencing.missedcall.ended'), 'warning');
        } else if (state === STARTED) {
          return this.getProvider().then(provider => provider.joinCall(this.callId));
        } else {
          return Promise.reject(new Error(`Unknown state of the call ${this.callId}`));
        }
      });
    },
    callAgain() {
      return getWebConferencing()
        .then(core => core && core.createUserContext(this.parameters.CALLER_ID))
        .then(context => context && this.getProvider().then(provider => provider.startCall(context)) || Promise.reject(new Error('no context')));
    },
    readCallState() {
      return getWebConferencing().then(core => {
        if (!core) {
          return null;
        }
        return this.withTimeout(new Promise(resolve => {
          // A 1:1 call that ended is removed: a call not found has ended too, any other error leaves its state unknown
          core.getCall(this.callId).then(
            call => resolve(call && call.state || null),
            error => resolve(error && error.code === 'NOT_FOUND_ERROR' && ENDED || null));
        }), null, CALL_STATE_TIMEOUT_MS);
      });
    },
    getProvider() {
      return getWebConferencing()
        .then(core => core && this.withTimeout(new Promise(resolve => core.getProvider(this.providerType).then(resolve, () => resolve(null))), null, PROVIDER_TIMEOUT_MS))
        .then(provider => provider && provider.startCall && provider.joinCall && provider || Promise.reject(new Error(`No call provider ${this.providerType}`)));
    },
    withTimeout(promise, fallback, delay) {
      return Promise.race([
        promise,
        new Promise(resolve => window.setTimeout(() => resolve(fallback), delay)),
      ]);
    },
    escapeHtml(text) {
      const element = document.createElement('span');
      element.textContent = text || '';
      return element.innerHTML;
    },
    closeWindow(callWindow) {
      // Only the blank window of this click is closed, never a call window the user is already in
      if (callWindow && !callWindow.closed && this.isBlankWindow(callWindow)) {
        callWindow.close();
      }
    },
    isBlankWindow(callWindow) {
      try {
        return callWindow.location.href === 'about:blank';
      } catch (e) {
        // A window on another origin, the call page of the provider, is not blank
        return false;
      }
    },
    alert(message, type) {
      document.dispatchEvent(new CustomEvent('alert-message', {detail: {
        alertMessage: message,
        alertType: type,
      }}));
    },
  },
};
</script>
