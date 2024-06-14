import callRecordingPlugin from './components/CallRecordingPlugin.vue';

const components = {
  'call-recording-notification': callRecordingPlugin,
};

for (const key in components) {
  Vue.component(key, components[key]);
}