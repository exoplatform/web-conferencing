/**
 * WebConferencing portlet in eXo Platform. This script initializes UI of a page where it is loaded using Web Conferencing
 * module.
 */
(function($, webConferencing) {
	"use strict";

	var init = function(user, context) {
		$(function() {
			// init context
			webConferencing.init(user, context);
			// and update it later on DOM changes (when portlets will load by Ajax)
			// MutationObserver supported only since IE11
			var iev = webConferencing.getIEVersion();
			if (iev == -1 || iev >= 11) {
				setTimeout(function() {
					var hasAdmin = document.getElementById("webconferencingAdmin");
					var targetId;
					var target;
					var chat = document.getElementById("chat-application");
					if (chat) {
						target = document.getElementById("room-users");
						targetId = "chat-application";
					} else {
						target = document.getElementById("UIActivitiesLoader");
						if (target) {
							targetId = "UIActivitiesLoader";
						} else {
							target = document.getElementById("RightBody");
							if (target) {
								targetId = "RightBody";
							} else {
								target = document.getElementById("UIPortalApplication");
								if (target) {
									targetId = "UIPortalApplication"; // XXX this may cause CPU loading on intranet home
									console.log("[webconferencing_portlet] WARN Portal's UIActivitiesLoader not found, will use the whole portal app for updates");
								}
							}
						}
					}
					webConferencing.update(targetId);
					if (!hasAdmin) {
						if (target) {
							var MutationObserver = window.MutationObserver || window.WebKitMutationObserver || window.MozMutationObserver;
							var observer = new MutationObserver(function(mutations) {
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
					} else {
						console.log("[webconferencing_portlet] running on Web Conferencing admin page");
					}
				}, 2500);
			}
		});
	};

	return {
		start : function(user, context) {
			init(user, context);
		}
	};
})($, webConferencing);
