/**
 * WebRTC provider module for Web Conferencing. This script will be used to add a provider to Web Conferencing module and then
 * handle calls for portal user/groups.
 */
(function($, webConferencing) {
	"use strict";

	var globalWebConferencing = typeof eXo != "undefined" && eXo && eXo.webConferencing ? eXo.webConferencing : null;
	
	// Use webConferencing from global eXo namespace (for non AMD uses)
	if (!webConferencing && globalWebConferencing) {
		webConferencing = globalWebConferencing;
	}

	if (webConferencing) {

		/** For debug logging. */
		var objId = Math.floor((Math.random() * 1000) + 1);
		var logPrefix = "[webrtc_" + objId + "] ";
		var log = function(msg, e) {
			webConferencing.log(msg, e, logPrefix);
		};
		//log("> Loading at " + location.origin + location.pathname);
		
		function WebrtcProvider() {
			var NON_WHITESPACE_PATTERN = /\s+/;
			var self = this;
			var settings, currentKey, clientId;
			
			this.isSupportedPlatform = function() {
				try {
					return navigator.mediaDevices && navigator.mediaDevices.getUserMedia && RTCPeerConnection;					
				} catch(e) {
					log("Error detecting WebRTC features: " + (typeof e == "string" ? e : ""), e);
					return false;
				}
			};
			
			this.getType = function() {
				if (settings) {
					return settings.type;
				}
			};
			
			this.getSupportedTypes = function() {
				if (settings) {
					return settings.supportedTypes;
				}
			};

			this.getTitle = function() {
				if (settings) {
					return settings.title;
				}
			};

			this.getCallTitle = function() {
				if (settings) {
					return settings.callTitle;
				}
			};
			
			this.getRtcConfiguration = function() {
				if (settings) {
					return settings.rtcConfiguration;
				}
			};

			this.configure = function(theSettings) {
				settings = theSettings;
			};

			this.isConfigured = function() {
				return settings != null;
			};
			
			this.getClientId = function() {
				return clientId;
			};
			
			var getRandomArbitrary = function(min, max) {
			  return Math.random() * (max - min) + min;
			};
			
			this.createId = function(prefix) {
				var rnd = Math.floor(getRandomArbitrary(10000, 99998) + 1);
				return prefix + "-" + rnd;
			};

			var joinedCall = function(callId) {
				return webConferencing.updateUserCall(callId, "joined").fail(function(err, status) {
					log("<< Error joining call: " + callId + ". " + JSON.stringify(err) + " [" + status + "]");
				});
			};
			this.joinedCall = joinedCall;
			
			var leavedCall = function(callId) {
				return webConferencing.updateUserCall(callId, "leaved").fail(function(err, status) {
					log("<< Error leaving call: " + callId + ". " + JSON.stringify(err) + " [" + status + "]");
				});
			};
			this.leavedCall = leavedCall;
			
			var deleteCall = function(callId) {
				// For P2P we delete closed call
				var process = $.Deferred();
				webConferencing.deleteCall(callId).done(function() {
					log("<< Deleted " + callId);
					process.resolve();
				}).fail(function(err) {
					if (err && (err.code == "NOT_FOUND_ERROR" || (typeof(status) == "number" && status == 404))) {
						// already deleted
						log("<< Call not found " + callId);
						process.resolve();
					} else {
						log("ERROR deleting " + callId + ": " + JSON.stringify(err));
						process.reject(err);
					}
				});
				return process.promise();
			};
			this.deleteCall = deleteCall;
			
			var onCallWindowReady = function(theWindow) {
				var process = $.Deferred();
				if (typeof theWindow == "undefined" && theWindow == null) {
					process.reject("Call window required");
				} else {
					var resolve = function() {
						if (process.state() == "pending") {
							if (theWindow.eXo && theWindow.eXo.webConferencing && typeof theWindow.eXo.webConferencing.startCall == "function") {
								process.resolve(theWindow);
								return true;
							} else {
								return false;
							}
						} else {
							return true;
						}
					};
					$(theWindow).on("load", function() {
						resolve();
					});
					var checker = setInterval(function() {
						if (resolve()) {
							clearInterval(checker);
						}
					}, 250);
				}
				return process.promise();
			};
			
			this.callButton = function(context) {
				var button = $.Deferred();
				if (self.isSupportedPlatform()) {
					if (settings && context && context.currentUser) {
						// XXX Currently we support only P2P calls
						if (!context.isGroup) {
							context.details().done(function(target) { // users, convName, convTitle
								var rndText = Math.floor((Math.random() * 1000000) + 1);
								var linkId = "WebrtcCall-" + clientId;
								var callId = "p/" + context.currentUser.id + "@" + target.id;
								var link = settings.callUri + "/" + callId;
								// TODO Disable call button if call already running
								//var disabledClass = hasJoinedCall(targetId) ? "callDisabled" : "";
								// TODO i18n for title
								var $button = $("<a id='" + linkId + "' title='" + target.title + "'"
											+ " class='webrtcCallAction'>"
											+ "<i class='callButtonIconVideo uiIconLightGray'></i>"
											+ "<span class='callTitle'>" + self.getCallTitle() + "</span></a>");
								$button.click(function() {
									// Open a window for a new call
									var callWindow = webConferencing.showCallPopup(link, self.getTitle() + " " + self.getCallTitle());
									// Create a call
									var callInfo = {
										owner : context.currentUser.id,
										ownerType : "user",  
										provider : self.getType(),
										title : target.title,
										participants : [context.currentUser.id, target.id].join(";") // eXo user ids separated by ';' !
									};
									webConferencing.addCall(callId, callInfo).done(function(call) {
										log(">> Added " + callId);
										// Tell the window to start the call  
										onCallWindowReady(callWindow).done(function() {
											log(">>> Call page loaded for " + callId);
											callWindow.document.title = "Call to " + target.title;
											// Timeout used for debug only - could be removed in production
											//setTimeout(function() {
											callWindow.eXo.webConferencing.startCall(call).done(function(state) {
												log("<<<< Call " + state + " " + callId);
											}).fail(function(err) {
												webConferencing.showError("Error starting call", webConferencing.errorText(err));
											});
										});
									}).fail(function(err) {
										log("ERROR adding " + callId + ": " + JSON.stringify(err));
										webConferencing.showError("Call error", webConferencing.errorText(err));
									});
								});
								button.resolve($button);
							}).fail(function(err) {
								log("Error getting context details for " + self.getTitle() + ": " + err);
								webConferencing.showWarn("Error starting a call", webConferencing.errorText(err));
							});
						} else {
							button.reject("Group calls not supported by WebRTC provider");
						}
					} else {
						button.reject("Not configured or empty context for " + self.getTitle());
					}
				} else {
					button.reject("WebRTC not supported in this browser: " + navigator.userAgent);
				}
				return button.promise();
			};
			
			var acceptCallPopover = function(callerLink, callerAvatar, callerMessage, playRingtone) {
				// TODO show an info popover in bottom right corner of the screen as specified in CALLEE_01
				log(">> acceptCallPopover '" + callerMessage + "' caler:" + callerLink + " avatar:" + callerAvatar);
				var process = $.Deferred();
				var $call = $("div.uiIncomingCall");
				if ($call.length > 0) {
					try {
						$call.dialog("destroy");
					} catch(e) {
						log(">>> acceptCallPopover: error destroing prev dialog ", e);
					}
					$call.remove();
				}
				$call = $("<div class='uiIncomingCall' title='Incoming call'></div>");
				//<span class='ui-icon messageIcon' style='float:left; margin:12px 12px 20px 0;'></span>
				$call.append($("<div class='messageAuthor'><a target='_blank' href='" + callerLink + "' class='avatarMedium'>"
					+ "<img src='" + callerAvatar + "'></a></div>"
					+ "<div class='messageBody'><div class='messageText'>" + callerMessage + "</div></div>"));
				// eXo UX guides way
				//$call.append($("<ul class='singleMessage popupMessage'><li><span class='messageAuthor'>"
				//		+ "<a class='avatarMedium'><img src='" + callerAvatar + "'></a></span>"
				//		+ "<span class='messageText'>" + callerMessage + "</span></li></ul>"));
				$(document.body).append($call);
				$call.dialog({
					resizable: false,
					height: "auto",
					width: 400,
					modal: false,
					buttons: {
					  "Answer": function() {
					  	process.resolve("accepted");
					  	$call.dialog("close");
					  },
					  "Decline": function() {
					  	process.reject("declined");
					  	$call.dialog("close");
					  }
					} 
				});
				$call.on("dialogclose", function( event, ui ) {
					if (process.state() == "pending") {
						process.reject("closed");
					}
				});
				process.notify($call);
				if (playRingtone) {
					// Start ringing incoming sound only if requested (depends on user status)
					var $ring = $("<audio loop autoplay style='display: none;'>" // controls 
								+ "<source src='/webrtc/audio/line.mp3' type='audio/mpeg'>"  
								+ "Your browser does not support the audio element.</audio>");
					$(document.body).append($ring);
					process.fail(function() {
						var $cancel = $("<audio autoplay style='display: none;'>" // controls 
									+ "<source src='/webrtc/audio/manner_cancel.mp3' type='audio/mpeg'>"  
									+ "Your browser does not support the audio element.</audio>");
						$(document.body).append($cancel);
						setTimeout(function() {
							$cancel.remove();
						}, 3000);
					});
					process.always(function() {
						// Stop incoming ringing on dialog completion
						$ring.remove();
					});					
				}
				return process.promise();
			};
			
			this.init = function(context) {
				var process = $.Deferred();
				if (self.isSupportedPlatform()) {
					var currentUserId = webConferencing.getUser().id;
					clientId = self.createId(currentUserId);
					var $callPopup;
					var closeCallPopup = function(callId, state) {
						if ($callPopup && $callPopup.callId && $callPopup.callId == callId) {
							if ($callPopup.is(":visible")) {
								// Set state before closing the dialog, it will be used by promise failure handler
								if (typeof state != "undefined") {
									$callPopup.callState = state;	
								}
								$callPopup.dialog("close");
							}								
						}
					};
					var lockCallButton = function(callerId, callerRoom) {
					};
					var unlockCallButton = function(callerId, callerRoom) {
					};
					if (webConferencing.getChat().isApplication()) {
						// Care about marking chat rooms for active calls
						var $chat = $("#chat-application");
						if ($chat.length > 0) {
							var $chats = $chat.find("#chats");
							if ($chats.length > 0) {
								var $users = $chat.find("#chat-users");
								// Implement Chat specific logic
								var getCallButton = function() {
									return $chat.find("#room-detail .webrtcCallAction");
								};
								lockCallButton = function(callerId, callerRoom) {
									var roomId = chatApplication.targetUser;
									if (roomId == (callerRoom ? callerRoom : callerId)) {
										var $callButton = getCallButton();
										if (!$callButton.hasClass("callDisabled")) {
											$callButton.addClass("callDisabled");
										}								
									}
								};
								unlockCallButton = function(callerId, callerRoom) {
									var roomId = chatApplication.targetUser;
									if (roomId == (callerRoom ? callerRoom : callerId)) {
										var $callButton = getCallButton();
										$callButton.removeClass("callDisabled");
									}
								};
							} else {
								log("Cannot find #chats element");
							} 
						} else {
							log("Chat application element not found.");
						}
					}
					// On portal pages we support incoming calls
					var lastUpdate = null; // XXX it's temp workaround
					var lastUpdateReset;
					if (window.location.pathname.startsWith("/portal/")) {
						webConferencing.onUserUpdate(currentUserId, function(update, status) {
							if (update.providerType == self.getType()) {
								if (update.eventType == "call_state") {
									if (update.owner.type == "user") {
										var callId = update.callId;
										var lastCallId = lastUpdate ? lastUpdate.callId : null;
										var lastCallState = lastUpdate ? lastUpdate.callState : null;
										if (callId == lastCallId && update.callState == lastCallState) {
											log("<<< XXX User call state updated skipped as duplicated: " + JSON.stringify(update) + " [" + status + "]");
										} else {
											log(">>> User call state updated: " + JSON.stringify(update) + " [" + status + "]");
											if (lastUpdateReset) {
												clearTimeout(lastUpdateReset);
											}
											lastUpdate = update;
											lastUpdateReset = setTimeout(function() {
												lastUpdate = null; // XXX avoid double action on duplicated update - temp solution
											}, 500);
											if (update.callState == "started") {
												webConferencing.getCall(callId).done(function(call) {
													log(">>> Got registered " + callId);
													var callerId = call.owner.id;
													var callerLink = call.ownerLink;
													var callerAvatar = call.avatarLink;
													var callerMessage = call.owner.title + " is calling you...";
													var callerRoom = callerId;
													call.title = call.owner.title; // for callee the call title is a caller name
													//
													webConferencing.getUserStatus(currentUserId).done(function(user) {
														var popover = acceptCallPopover(callerLink, callerAvatar, callerMessage, !user || user.status == "available" || user.status == "away");
														popover.progress(function($call) {
															$callPopup = $call;
															$callPopup.callId = callId;
															$callPopup.callState = update.callState;
														}); 
														popover.done(function(msg) {
															log(">>> User " + msg + " call " + callId);
															var link = settings.callUri + "/" + callId;
															var callWindow = webConferencing.showCallPopup(link, self.getTitle() + " " + self.getCallTitle());
															// Tell the window to start a call  
															onCallWindowReady(callWindow).done(function() {
																log(">>>> Call page loaded " + callId);
																callWindow.document.title = "Call with " + call.owner.title;
																callWindow.eXo.webConferencing.startCall(call).done(function(state) {
																	log("<<<< Call " + state + " " + callId);
																	lockCallButton(callerId, callerRoom);
																}).fail(function(err) {
																	webConferencing.showError("Error starting call", webConferencing.errorText(err));
																});
															});
														});
														popover.fail(function(err) {
															if ($callPopup.callState != "stopped" && $callPopup.callState != "joined") {
																log("<<< User " + err + ($callPopup.callState ? " just " + $callPopup.callState : "") + " call " + callId + ", deleting it.");
																deleteCall(callId);
															}
														});
													}).fail(function(err, status) {
														log(">>> User status error: " + JSON.stringify(err) + " [" + status + "]");
														if (err) {
															webConferencing.showError("Incoming call error", webConferencing.errorText(err));
														} else {
															webConferencing.showError("Incoming call error", "Error read user status information from the server");
														}
													});
												}).fail(function(err, status) {
													log(">>> Call info error: " + JSON.stringify(err) + " [" + status + "]");
													if (err) {
														webConferencing.showError("Incoming call error", webConferencing.errorText(err));
													} else {
														webConferencing.showError("Incoming call error", "Error read call information from the server");
													}
												});
											} else if (update.callState == "stopped") {
												// Hide accept popover for this call, if any
												closeCallPopup(callId, update.callState);
												// Unclock the call button
												var callerId = update.owner.id; // callerRoom is the same as owner Id for P2P
												unlockCallButton(callerId, callerId); 
											}
										}
									} else {
										log(">>> Group calls not supported: " + JSON.stringify(update) + " [" + status + "]");
									}
								} else if (update.eventType == "call_joined") {
									// If user has incoming popup open for this call (several user's windows/clients), then close it
									log(">>> User call joined: " + JSON.stringify(update) + " [" + status + "]");
									if (currentUserId == update.part.id) {
										closeCallPopup(update.callId, "joined");
									}
								} else if (update.eventType == "call_leaved") {
									// TODO not used
								} else if (update.eventType == "retry") {
									log("<<< Retry for user updates [" + status + "]");
								} else {
									log("<<< Unexpected user update: " + JSON.stringify(update) + " [" + status + "]");
								}
							} // it's other provider type
						}, function(err) {
							// Error handler
							log(err);
						});
					}
					//
					process.resolve();
				} else {
					process.reject("WebRTC not supported in this browser: " + navigator.userAgent);
				}
				return process.promise();
			};
			
			this.initAdmin = function(context) {
				var process = $.Deferred();
				// TODO init Settings button and its click handler to show WebRTC settings form
				/////
				
				// TODO use Lamia's WebRTC popup code here
				
				/////
				process.reosolve();
				return process.promise();
			};
		}

		var provider = new WebrtcProvider();

		// Add WebRTC provider into webConferencing object of global eXo namespace (for non AMD uses)
		if (globalWebConferencing) {
			globalWebConferencing.webrtc = provider;
			log("> Added eXo.webConferencing.webrtc");
		} else {
			log("eXo.webConferencing not defined");
		}
		
		$(function() {
			try {
				// XXX workaround to load CSS until gatein-resources.xml's portlet-skin will be able to load after the Enterprise skin
				webConferencing.loadStyle("/webrtc/skin/webrtc.css");
			} catch(e) {
				log("Error loading WebRTC Call styles.", e);
			}
		});

		log("< Loaded at " + location.origin + location.pathname);
		
		return provider;
	} else {
		window.console && window.console.log("WARN: webConferencing not given and eXo.webConferencing not defined. WebRTC provider registration skipped.");
	}
})($, typeof webConferencing != "undefined" ? webConferencing : null );
