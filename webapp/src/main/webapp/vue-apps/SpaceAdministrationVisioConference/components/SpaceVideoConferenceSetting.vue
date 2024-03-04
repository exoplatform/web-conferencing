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
      <v-card class="pa-6 card-border-radius overflow-hidden" flat>
        <v-list-item class="px-0 mb-4">
          <v-list-item-content class="py-0">
            <v-list-item-title class="title text-color my-0">
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
              :aria-label="this.$t(`videoConference.switch.label.${this.switchAriaLabel}`)" />
          </v-list-item-action>
        </v-list-item>
        <v-list-item class="px-0">
          <v-list-item-content>
            <v-list-item-title class="subtitle-1">
              {{ $t('videoConference.space.settings.list.title') }}
            </v-list-item-title>
            <v-list-item-subtitle>
              {{ $t('videoConference.space.settings.list.description') }}
            </v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
        <v-list-item
          v-for="provider in activeProviders"
          :key="provider"
          class="px-0">
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
          <v-list-item-action class="pt-0 ma-0 mb-6" v-if="!provider.integratedConnector">
            <v-btn 
              :title="$t('videoConference.space.settings.editConnector')"
              primary
              icon>
              <i class="uiIconEdit"></i>
            </v-btn>
          </v-list-item-action>
        </v-list-item>
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
    this.getActiveProvidersForSpace();
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
  }

};
</script>
