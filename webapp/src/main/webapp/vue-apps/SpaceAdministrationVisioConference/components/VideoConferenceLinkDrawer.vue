<!--
Copyright (C) 2024 eXo Platform SAS.

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
  <div>
    <exo-drawer
      id="videoConferenceLinkDrawer"
      ref="videoConferenceLinkDrawer"
      right
      allow-expand
      @closed="close">
      <template slot="title">
        <div class="d-flex">
          <v-icon
            size="16"
            class="clickable"
            :aria-label="$t('videoConference.close.label')"
            @click="close()">
            fas fa-arrow-left
          </v-icon>
          <span>{{ $t('videoConference.drawer.title', {0: videoConferenceName}) }}</span>
        </div>
      </template>
      <template slot="content">
        <v-form v-model="isValidForm">
          <v-card-text class="d-flex pb-2">
            <v-label>
              <span class="text-color font-weight-bold text-start text-truncate-2">
                {{ $t('videoConference.drawer.link.title') }}           
              </span>
              <p class="caption">{{ $t('videoConference.drawer.link.description') }}</p>
            </v-label>
          </v-card-text>
          <v-card-text class="d-flex py-0">
            <v-text-field
              v-model="videoConferenceLink"
              :placeholder="$t('videoConference.drawer.link.placeholder')"
              :rules="linkRules"
              class="pt-0"
              type="text"
              required
              outlined
              dense />
          </v-card-text>
        </v-form>
      </template>
      <template slot="footer">
        <div class="d-flex justify-end">
          <v-btn
            class="btn ms-2"
            @click="close">
            {{ $t('videoConference.label.btn.cancel') }}
          </v-btn>
          <v-btn
            :disabled="disabled"
            @click="saveVideoConference"
            class="btn btn-primary ms-2">
            {{ $t('videoConference.label.btn.Save') }}
          </v-btn>
        </div>
      </template>
    </exo-drawer>
  </div>
</template>
<script>
export default {
  data () {
    return {
      videoConference: null,
      videoConferenceLink: '',
      isValidForm: true,
      linkRules: [url => !!(url.match(/^((https?:\/\/)?(www\.)?[a-zA-Z0-9]+\.[^\s]{2,})|(javascript:)|(\/portal\/)/))
              || ( !url.length && this.$t('videoConference.required.error.message') || this.$t('videoConference.label.invalidLink'))],
    };
  },
  created() {
    this.$root.$on('open-video-conference-link-drawer', this.open);
  },
  computed: {
    disabled() { 
      return !this.videoConferenceLink || this.videoConferenceLink === this.videoConference.url || !this.isValidForm;
    },
    videoConferenceName() {
      return this.videoConference && this.videoConference.name;
    }
  },
  methods: {
    open(videoConference) {
      this.videoConference = videoConference;
      this.videoConferenceLink = videoConference.url;
      this.$nextTick().then(() => this.$refs.videoConferenceLinkDrawer.open());
    },
    close() {
      this.isValidForm = false;
      this.videoConferenceLink = '';
      this.videoConference = null;
      this.$nextTick().then(() => this.$refs.videoConferenceLinkDrawer.close());
    },
    saveVideoConference() {
      const videoConference = {
        connectorId: this.videoConference.connectorId,
        identity: eXo.env.portal.spaceId,
        name: this.videoConference.name,
        url: this.videoConferenceLink,
        integratedConnector: this.videoConference.integratedConnector

      };
      this.$videoConferenceService.saveActiveProvider(videoConference).then(() => {
        this.$root.$emit('alert-message', this.$t('videoConference.label.saveLink.success'), 'success');
        this.$root.$emit('refresh-video-conferences');
        this.close();
      });
    }
  }
};
</script>