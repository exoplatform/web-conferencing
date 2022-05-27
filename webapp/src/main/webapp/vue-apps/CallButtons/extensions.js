export function initCallExtensions() {
  extensionRegistry.registerComponent('UserPopover', 'user-popover-action', {
    id: 'call',
    vueComponent: Vue.options.components['call-botton-popover'],
    rank: 70,
  });
}