/**
 * WebConferencing portlet in eXo Platform. This script initializes UI of a page where it is loaded using Web Conferencing
 * module.
 */
(function($, webConferencing) {
	"use strict";

	var updater;

	var init = function(user, context) {
		$(function() {
			// init context
			webConferencing.init(user, context);

			// and later on DOM changes (when portlets will load by Ajax)
			var iev = webConferencing.getIEVersion();
			if (iev == -1 || iev >= 11) {
				// TODO as for IE<11 need use polyfills
				// http://webcomponents.org/polyfills/
				updater = setTimeout(
							function() {
								var targetId;
								var target;
								var chat = document.getElementById("chat-application");
								if (chat) {
									target = document.getElementById("room-users");
									targetId = "chat-application";
								} else {
									target = document.getElementById("RightBody");
									if (target) {
										targetId = "RightBody";
									} else {
										target = document.getElementById("UIPortalApplication");
										if (target) {
											targetId = "UIPortalApplication"; // XXX this may cause CPU loading on intranet home
											console.log("[webconferencing_portlet] Portal's RightBody not found, will use the whole portal app for updates");
										}
									}
								}
								webConferencing.update(targetId);
								if (target) {
									var MutationObserver = window.MutationObserver || window.WebKitMutationObserver || window.MozMutationObserver;
									var observer = new MutationObserver(function(mutations) {
										// FYI this will be fired twice on each update
										webConferencing.update(targetId);
									});
									observer.observe(target, {
										subtree : true,
										childList : true,
										attributes : false,
										characterData : false
									});									
								} else {
									console.log("[webconferencing_portlet] target not found for updates");
								}
							}, 2500);
			}
		});
	};

	return {
		start : function(user, context) {
			init(user, context);
		},
		stop : function() {
			if (updater) {
				clearTimeout(updater);
			}
		}
	};
})($, webConferencing);
