import CallButtons from './components/CallButtons.vue';
import Dropdown from './components/Dropdown.vue';
import DropdownHeader from './components/DropdownHeader.vue';
import SingleButton from './components/SingleButton.vue';
import CallBottonPopover from './components/CallBottonPopover.vue';
const components = {
  'call-button': CallButtons,
  'dropdown': Dropdown,
  'dropdown-header': DropdownHeader,
  'single-button': SingleButton,
  'call-botton-popover': CallBottonPopover,
};

for (const key in components) {
  Vue.component(key, components[key]);
}