/* eslint-disable no-undef */
<template>
  <v-app id="web-conferencing-admin" class="VuetifyApp">
    <v-container style="width: 95%" class="v-application--is-ltr">
      <div
        v-show="error"
        class="alert alert-error">{{ i18n.te(`${errorResourceBase}.${error}`) ? $t(`${errorResourceBase}.${error}`) : error }}</div>
      <v-row class="white">
        <v-col xs12 px-3>
          <h4 class="webconferencingTitle">{{ $t("webconferencing.admin.title") }}</h4>
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
                  <th class="text-left">{{ $t("webconferencing.admin.table.Provider") }}</th>
                  <th class="text-left">{{ $t("webconferencing.admin.table.Description") }}</th>
                  <th
                    class="text-left"
                    style="width: 5%">{{ $t("webconferencing.admin.table.Active") }}</th>
                  <th
                    class="text-left"
                    style="width: 5%">{{ $t("webconferencing.admin.table.Permissions") }}</th>
                </tr>
              </thead>
              <tbody v-if="providers.length > 0">
                <tr 
                  v-for="item in providers" 
                  :key="item.title"
                  class="providersTableRow">
                  <td>
                    <div>
                      {{ i18n.te(`webconferencing.admin.${item.title}.name`)
                        ? $t(`webconferencing.admin.${item.title}.name`)
                        : item.title
                      }}
                    </div>
                  </td>
                  <td>
                    <div 
                      v-html="i18n.te(`webconferencing.admin.${item.title}.description`)
                        ? $t(`webconferencing.admin.${item.title}.description`)
                      : '' ">
                    </div>
                  </td>
                  <td class="center actionContainer">
                    <div>
                      <v-switch
                        :dense="true"
                        :input-value="item.active"
                        :ripple="false"
                        v-model="item.active"
                        hide-details
                        color="#568dc9"
                        class="providersSwitcher"
                        @change="changeActive(item)"/>
                    </div>
                  </td>
                  <td class="center actionContainer">
                    <i class="uiIconSetting uiIconLightGray"></i>
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
import { postData, getData } from "../AdminAPI";

export default {
  components: {
  },
  props: {
    services: {
      type: Object,
      required: true
    },
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
      providers: [],
      switcher: false,
      error: null
    };
  },
  created() {
    this.getProviders();
  },
  methods: {
    async getProviders() {
      // services object contains urls for requests
      try {
        // const data = await getData(this.services.providers);
        const response = await webConferencing.getProvidersConfig();
        const data = webConferencing.getProvidersConfig().done(response);
        this.error = null;
        this.providers = await response;
        // const resourcesPromises = this.providers.map(({ provider }) => this.getProviderResources(provider));
        // Promise.all(resourcesPromises).then(res => {
        //   res.map(localized => {
        //     this.i18n.mergeLocaleMessage(this.language, localized.getLocaleMessage(this.language));
        //   });
        // });
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
        const data = await webConferencing.postProviderConfig(provider.type, provider.active)
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
      margin-left: 10px;
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