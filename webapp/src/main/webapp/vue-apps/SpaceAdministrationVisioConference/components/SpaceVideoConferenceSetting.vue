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
      <!-- One row, drawn as the rows of the user Settings page; Space settings draws the card around it -->
      <div id="videoConferenceSpaceSetting">
        <v-list-item two-line>
          <v-list-item-content>
            <v-list-item-title class="text-color">
              {{ $t('videoConference.space.settings.title') }}
            </v-list-item-title>
            <v-list-item-subtitle class="video-conference-space-settings-summary text-wrap">
              {{ summary }}
            </v-list-item-subtitle>
          </v-list-item-content>
          <v-list-item-action>
            <v-switch
              id="videoConferenceSpaceSettingSwitch"
              v-model="active"
              class="ma-0"
              :disabled="savedActive === null"
              @change="switchVideoConference"
              :aria-label="this.$t(`videoConference.switch.label.${this.switchAriaLabel}`)" />
          </v-list-item-action>
        </v-list-item>
        <v-list-item v-if="shouldDisplayProvidersList">
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
                @change="updateProviderEnabled(provider, provider.enabled)" />
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
        <exo-confirm-dialog
          ref="disableConfirmDialog"
          :title="$t('videoConference.space.settings.disable.title')"
          :message="$t('videoConference.event.settings.description')"
          :ok-label="$t('videoConference.space.settings.disable.confirm')"
          :cancel-label="$t('videoConference.space.settings.disable.cancel')"
          @ok="confirmDisable"
          @dialog-closed="restoreUnlessDisabled" />
      </div>
    </template>
  </v-app>
</template>
<script>
export default {
  data: () =>({
    activeProviders: [],
    active: true,
    // the value stored for the space, null until read; the switch waits for it
    savedActive: null,
    lastSave: Promise.resolve(),
    savesCount: 0,
    disableConfirmed: false,
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
    /**
     * @returns {String} whether members can call the space outside an event, as one
     *          line; nothing until the stored value is read
     */
    summary() {
      if (this.savedActive === null) {
        return null;
      }
      return this.savedActive
        ? this.$t('videoConference.space.settings.summary.enabled')
        : this.$t('videoConference.space.settings.summary.disabled');
    },
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
    /**
     * Applies the switch: switching on saves at once; switching off asks for
     * a confirmation first, since the space then loses its calls outside an
     * event.
     *
     * @param {Boolean} enabled the value the user switched to
     * @returns {void}
     */
    switchVideoConference(enabled) {
      if (enabled) {
        this.updateVideoConferenceEnabled(true);
      } else {
        this.disableConfirmed = false;
        this.$refs.disableConfirmDialog.open();
      }
    },
    /**
     * Saves the switch-off the user confirmed.
     *
     * @returns {void}
     */
    confirmDisable() {
      this.disableConfirmed = true;
      this.updateVideoConferenceEnabled(false);
    },
    /**
     * Turns the switch back on when the confirmation closes without the user
     * confirming: the switch shows off while the confirmation is open, and the
     * setting stays on.
     *
     * @returns {void}
     */
    restoreUnlessDisabled() {
      if (!this.disableConfirmed) {
        this.active = true;
      }
    },
    getActiveProvidersForSpace() {
      this.$videoConferenceService.getActiveProvidersForSpace(this.spaceId)
        .then((activeProviders) => {
          this.activeProviders = activeProviders.slice().sort((a, b) => {
            // Tri en fonction de la valeur de l'attribut "integratedConnector"
            return (a.integratedConnector === b.integratedConnector) ? 0 : a.integratedConnector ? -1 : 1;
          });
        });
    },
    /**
     * Saves whether the space can call outside an event. Saves run one after
     * the other, so the server applies them in the order the user made them;
     * when the last one asked fails, the switch goes back to the value last
     * stored.
     *
     * @param {Boolean} enabled the value to save
     * @returns {Promise} resolved once this save is answered
     */
    updateVideoConferenceEnabled(enabled) {
      const saveIndex = ++this.savesCount;
      this.lastSave = this.lastSave
        .then(() => this.$videoConferenceService.updateVideoConferenceEnabled(this.spaceId, enabled))
        .then(() => this.savedActive = enabled)
        .catch(() => {
          if (saveIndex === this.savesCount) {
            this.active = this.savedActive;
          }
          this.$root.$emit('alert-message', this.$t('videoConference.error.unknownErrorWhenSavingSpace'), 'error');
        });
      return this.lastSave;
    },
    /**
     * Saves whether a connector is enabled for the space. It leaves the
     * space's own switch as it is. A failed save puts the connector's switch
     * back, unless the user has switched it again since.
     *
     * @param {Object} provider the connector whose switch changed
     * @param {Boolean} enabled the value to save
     * @returns {Promise} resolved once the save is answered
     */
    updateProviderEnabled(provider, enabled) {
      return this.$videoConferenceService.updateVideoConferenceEnabled(this.spaceId, enabled, provider.connectorId)
        .catch(() => {
          if (provider.enabled === enabled) {
            provider.enabled = !enabled;
          }
          this.$root.$emit('alert-message', this.$t('videoConference.error.unknownErrorWhenSavingSpace'), 'error');
        });
    },
    /**
     * Reads the value stored for the space. Until it is read the switch stays
     * disabled, since a save could not tell what it changes; a failed read
     * says so and leaves the switch disabled.
     *
     * @returns {Promise} resolved once the value is read or the error shown
     */
    isVideoConferenceEnabled() {
      return this.$videoConferenceService.isVideoConferenceEnabled(this.spaceId)
        .then((enabled) => {
          this.active = enabled;
          this.savedActive = enabled;
        })
        .catch(() => this.$root.$emit('alert-message', this.$t('videoConference.error.unknownErrorWhenReadingSpace'), 'error'));
    },
  }

};
</script>

