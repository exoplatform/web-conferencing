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
    :url="url" />
</template>

<script>
export default {
  props: {
    notification: {
      type: Object,
      default: null,
    },
  },
  computed: {
    parameters() {
      return this.notification && this.notification.parameters || {};
    },
    declinerIds() {
      return (this.parameters.DECLINER_IDS || '').split(',').filter(id => id);
    },
    severalDecliners() {
      return this.declinerIds.length > 1;
    },
    decliner() {
      return this.notification && this.notification.from;
    },
    declinerName() {
      return this.decliner && this.decliner.fullname || this.declinerIds[0];
    },
    avatarUrl() {
      return this.severalDecliners && this.parameters.AVATAR || this.decliner && this.decliner.avatar;
    },
    url() {
      return !this.severalDecliners && this.declinerIds.length && `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/profile/${this.declinerIds[0]}` || null;
    },
    message() {
      if (this.severalDecliners) {
        return this.$t('Notification.webconferencing.declinedcall.several', {0: `<span class="font-weight-bold">${this.declinerIds.length}</span>`});
      }
      return this.$t('Notification.webconferencing.declinedcall.one', {0: `<a class="user-name font-weight-bold">${this.escapeHtml(this.declinerName)}</a>`});
    },
  },
  methods: {
    escapeHtml(text) {
      const element = document.createElement('span');
      element.textContent = text || '';
      return element.innerHTML;
    },
  },
};
</script>
