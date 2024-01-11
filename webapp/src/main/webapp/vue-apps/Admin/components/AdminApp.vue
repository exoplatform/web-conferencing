<template>
  <v-app id="web-conferencing-admin">
    <v-card class="px-6 card-border-radius overflow-hidden" flat>
      <div
        v-show="error"
        class="alert alert-error">
        {{ $t(`${errorResourceBase}.${error}`) ? $t(`${errorResourceBase}.${error}`) : error }}
      </div>
      <v-list-item class="px-0 mb-2 mt-4">
        <v-list-item-content class="py-0">
          <v-list-item-title class="my-0">
            <h4 class="font-weight-bold mt-0">
              {{ $t("webconferencing.admin.title") }}
            </h4>
          </v-list-item-title>
          <v-list-item-subtitle class="pt-4">
            <h4 class="my-0 text-color">{{ $t("webconferencing.admin.section.title") }}</h4>
          </v-list-item-subtitle>
        </v-list-item-content>
      </v-list-item>
      <v-list-item
        class="pb-2 px-0"
        dense
        v-for="providerConfig in providersConfig"
        :key="providerConfig.title">
        <v-list-item-action class="me-4 mt-0">
          <v-switch
            :input-value="providerConfig.active"
            v-model="providerConfig.active"
            color="primary"
            class="providersSwitcher"
            @change="changeActive(providerConfig)" />
        </v-list-item-action>
        <v-list-item-content class="py-0">
          <v-list-item-title class="subtitle-1 pt-2">
            {{ $t(`webconferencing.admin.${providerConfig.title}.name`)
              ? $t(`webconferencing.admin.${providerConfig.title}.name`)
              : providerConfig.title }} 
          </v-list-item-title>
          <div
            class="caption text-light-color" 
            v-html="$t(`webconferencing.admin.${providerConfig.title}.description`)
              ? $t(`webconferencing.admin.${providerConfig.title}.description`)
              : '' ">
          </div>
        </v-list-item-content>
      </v-list-item>
    </v-card>
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
