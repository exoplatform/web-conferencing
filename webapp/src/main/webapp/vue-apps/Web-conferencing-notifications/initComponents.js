import callRecordingPlugin from './components/CallRecordingPlugin.vue';
import missedCallPlugin from './components/MissedCallPlugin.vue';

const components = {
  'call-recording-notification': callRecordingPlugin,
  'missed-call-notification': missedCallPlugin,
};

for (const key in components) {
  Vue.component(key, components[key]);
}