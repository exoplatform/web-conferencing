<template>
  <v-app id="web-conferencing-admin">
    <v-container style="width: 95%">
      <div
        v-show="error"
        class="alert alert-error">
        {{ $t(`${errorResourceBase}.${error}`) ? $t(`${errorResourceBase}.${error}`) : error }}
      </div>
      <v-row>
        <v-col xs12 px-3>
          <h4 class="webconferencingTitle">
            <span class="me-3">{{ $t("webconferencing.admin.title") }}</span>
          </h4>
        </v-col>
      </v-row>
      <v-row>
        <v-col xs12>
          <v-simple-table
            :dense="true"
            class="uiGrid table table-hover providersTable">
            <template v-slot:default>
              <thead>
                <tr class="providersTableRow">
                  <th class="text-start">{{ $t("webconferencing.admin.table.Provider") }}</th>
                  <th class="text-start">{{ $t("webconferencing.admin.table.Description") }}</th>
                  <th
                    class="text-start"
                    style="width: 5%">
                    {{ $t("webconferencing.admin.table.Active") }}
                  </th>
                  <th
                    class="text-start"
                    style="width: 5%">
                    {{ $t("webconferencing.admin.table.Permissions") }}
                  </th>
                </tr>
              </thead>
              <tbody v-if="providersConfig.length > 0">
                <tr 
                  v-for="providerConfig in providersConfig"
                  :key="providerConfig.title"
                  class="providersTableRow">
                  <td>
                    <div>
                      {{ $t(`webconferencing.admin.${providerConfig.title}.name`)
                        ? $t(`webconferencing.admin.${providerConfig.title}.name`)
                        : providerConfig.title
                      }}
                    </div>
                  </td>
                  <td>
                    <div 
                      v-html="$t(`webconferencing.admin.${providerConfig.title}.description`)
                        ? $t(`webconferencing.admin.${providerConfig.title}.description`)
                        : '' ">
                    </div>
                  </td>
                  <td class="center actionContainer">
                    <div>
                      <v-switch
                        :dense="true"
                        :input-value="providerConfig.active"
                        :ripple="false"
                        v-model="providerConfig.active"
                        hide-details
                        color="#568dc9"
                        class="providersSwitcher"
                        @change="changeActive(providerConfig)" />
                    </div>
                  </td>
                  <td class="center actionContainer">
                    <i
                      v-if="providerConfig.hasSettings"
                      class="uiIconSetting uiIconLightGray"
                      @click="providerConfig.provider.showSettings()"></i>
                  </td>
                </tr>
              </tbody>
            </template>
          </v-simple-table>
        </v-col>
      </v-row>
    </v-container>
  </v-app>
</template>

<script>
export default {
  props: {
    i18n: {
      type: Object,
      required: true
    },
    language: {
      type: String,
      required: true
    },
    resourceBundleName: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      providersConfig: [],
      switcher: false,
      error: null,
      log: null
    };
  },
  created() {
    this.log = webConferencing.getLog().prefix('webconferencing.admin');
    this.getProviders();
  },
  methods: {
    getProviders() {
      // services object contains urls for requests
      try {
        const thisvue = this;
        webConferencing.getProvidersConfig().then((providersConfig) => {
          for (const providerConfig of providersConfig) {
            webConferencing.getProvider(providerConfig.type).then((provider) => {
              providerConfig.provider = provider;
              // set setting visibility for the provider
              providerConfig.hasSettings = provider.showSettings && provider.hasOwnProperty('showSettings');
              thisvue.providersConfig.push(providerConfig);
            }).fail(function(err) {
              thisvue.log.warn(`Provider ${providerConfig.type} is not available`, err);
            });
          }
        });
        this.error = null;
      } catch (err) {
        this.error = err.message;
      }
    },
    getProviderResources(providerId) {
      const resourceUrl = `${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/locale.${providerId}.${this.resourceBundleName}-${this.language}.json`;
      return exoi18n.loadLanguageAsync(this.language, resourceUrl);
    },
    async changeActive(provider) {
      // getting rest for updating provider status
      try {
        await webConferencing.postProviderConfig(provider.type, provider.active);
        this.error = null;
      } catch (err) {
        this.error = err.message;
      }
    }
  }
};
</script>

<style scoped lang="less">
  #web-conferencing-admin {
    .webconferencingTitle {
    color: #4d5466;
    font-size: 24px;
    position: relative;
    overflow: hidden;
    line-height: 27px;

    &:after {
      border-bottom: 1px solid #dadada;
      height: 14px;
      content: "";
      position: absolute;
      width: 100%;
    }
  }
    .providersTable {
    border-left: 0;
      .providersTableRow {
        th,
        td {
          height: 20px;
          padding: 5px 15px;
        }
      }
    }
      .providersSwitcher {
      padding: 0;
      margin: 0;
      height: 25px;
    }
    .uiIconSetting::before{
      content: "\f13e";
      font-size: 21px;
    }
  }
</style>
