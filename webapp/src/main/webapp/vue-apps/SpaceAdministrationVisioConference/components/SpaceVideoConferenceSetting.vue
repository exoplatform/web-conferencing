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
  <v-app>
    <template v-if="displayed">
      <v-card class="application-body" flat>
        <v-list-item class="px-0">
          <v-list-item-content>
            <v-list-item-title class="text-title my-0">
              {{ $t('videoConference.space.settings.title') }}
            </v-list-item-title>
            <v-list-item-title class="pt-2">
              {{ $t('videoConference.event.settings.title') }}
            </v-list-item-title>
            <v-list-item-subtitle>
              {{ $t('videoConference.event.settings.description') }}
            </v-list-item-subtitle>
          </v-list-item-content>
          <v-list-item-action class="pt-6">
            <v-switch
              v-model="active"
              @change="updateVideoConferenceEnabled(active, undefined)"
              :aria-label="this.$t(`videoConference.switch.label.${this.switchAriaLabel}`)" />
          </v-list-item-action>
        </v-list-item>
        <v-list-item class="px-0" v-if="shouldDisplayProvidersList">
          <v-list-item-content>
            <v-list-item-title class="subtitle-1">
              <h4 class="my-0 text-color">{{ $t('videoConference.space.settings.list.title') }}</h4>
            </v-list-item-title>
            <v-list-item-subtitle>
              {{ $t('videoConference.space.settings.list.description') }}
            </v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
        <div v-if="shouldDisplayProvidersList">
          <v-list-item
            v-for="provider in activeProviders"
            :key="provider"
            class="pe-0">
            <v-list-item-content>
              <v-list-item-title class="subtitle-1">
                {{ provider.name }}
              </v-list-item-title>
              <v-list-item-subtitle v-if="provider.integratedConnector">
                {{ $t(`videoConference.space.settings.${provider.name}.description`) }}
              </v-list-item-subtitle>
              <v-list-item-subtitle v-else>
                {{ provider.url ? $t('videoConference.space.settings.connector.link.descrition', {0: provider.url}) : $t('videoConference.space.settings.connector.descrition') }}
              </v-list-item-subtitle>
            </v-list-item-content>
            <v-list-item-action v-if="provider.integratedConnector">
              <v-switch
                v-model="provider.enabled"
                @change="updateVideoConferenceEnabled(provider.enabled, provider.connectorId)" />
            </v-list-item-action>
            <v-list-item-action class="pt-0 ma-0 mb-6" v-else>
              <v-btn 
                :title="$t('videoConference.space.settings.editConnector')"
                primary
                icon
                @click="$root.$emit('open-video-conference-link-drawer', provider)">
                <i class="uiIconEdit"></i>
              </v-btn>
            </v-list-item-action>
          </v-list-item>
        </div>
        <video-conference-link-drawer />
      </v-card>
    </template>
  </v-app>
</template>
<script>
export default {
  data: () =>({
    activeProviders: [],
    active: true,
    spaceId: eXo.env.portal.spaceId,
    displayed: true,
  }),
  created() {
    this.isVideoConferenceEnabled();
    this.getActiveProvidersForSpace();
    this.$root.$on('refresh-video-conferences', this.getActiveProvidersForSpace);
    document.addEventListener('hideSettingsApps', (event) => {
      if (event && event.detail && this.id !== event.detail) {
        this.displayed = false;
      }
    });
    document.addEventListener('showSettingsApps', () => this.displayed = true);
  },
  computed: {
    switchAriaLabel() {
      return this.active && 'disable' || 'enable';
    },
    shouldDisplayProvidersList() {
      //we display provider list if there is at least on external connector available to configure
      //or if there is more than one integrated connector
      return this.activeProviders.some((provider) => !provider.integratedConnector ) || this.activeProviders.filter((provider) => provider.integratedConnector ).length > 1;
    },
  },
  methods: {
    getActiveProvidersForSpace() {
      this.$videoConferenceService.getActiveProvidersForSpace(this.spaceId)
        .then((activeProviders) => {
          this.activeProviders = activeProviders.slice().sort((a, b) => {
            // Tri en fonction de la valeur de l'attribut "integratedConnector"
            return (a.integratedConnector === b.integratedConnector) ? 0 : a.integratedConnector ? -1 : 1;
          });
        });
    },
    updateVideoConferenceEnabled(enabled, provider) {
      this.$videoConferenceService.updateVideoConferenceEnabled(this.spaceId, enabled,provider).then(() => {
        this.active=enabled;
      }).catch(() => {
        this.active=!enabled;
        this.$root.$emit('alert-message', this.$t('videoConference.error.unknownErrorWhenSavingSpace'), 'error');
      });
    },
    isVideoConferenceEnabled() {
      this.$videoConferenceService.isVideoConferenceEnabled(this.spaceId)
        .then((enabled) => {
          this.active = enabled;
        });
    },
  }

};
</script>

