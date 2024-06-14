/**
 * WebConferencing portlet in eXo Platform. This script initializes UI of a page where it is loaded using Web Conferencing
 * module.
 */
(function ($, webConferencing) {
  "use strict";
  return {
    start: function (user, context) {
      $(function () {
        if (!webConferencing.initialized) {
          webConferencing.loadUserInfo(eXo.env.portal.userName)
            .then(data => user = data)
            .then(() => webConferencing.loadContext(eXo.env.portal.userName, eXo.env.portal.language))          
            .then(data => context = data)
            .then(() => {
              // init context
              webConferencing.init(user, context);
              webConferencing.update();
            });
        }
      });
    }
  };
}) ($, webConferencing);

