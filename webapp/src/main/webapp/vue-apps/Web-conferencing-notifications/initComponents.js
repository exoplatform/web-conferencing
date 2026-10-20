import callRecordingPlugin from './components/CallRecordingPlugin.vue';
import missedCallPlugin from './components/MissedCallPlugin.vue';
import declinedCallPlugin from './components/DeclinedCallPlugin.vue';

const components = {
  'call-recording-notification': callRecordingPlugin,
  'missed-call-notification': missedCallPlugin,
  'declined-call-notification': declinedCallPlugin,
};

for (const key in components) {
  Vue.component(key, components[key]);
}