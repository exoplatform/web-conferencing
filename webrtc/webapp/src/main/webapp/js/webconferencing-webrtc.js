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
			var messages = {};
			
			var message = function(key) {
				return messages["webrtc." + key];
			};
			
			var message = function(key) {
				return settings ? settings.messages["webrtc." + key] : "";
			};
			
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
			
			/**
			 * TODO not used, setting loaded with the module (see in init())
			 */
			var getSettings = function() {
				var request = $.ajax({
					async : true,
					type : "GET",
					url : "/portal/rest/webrtc/webconferencing/settings"
				});
				return webConferencing.initRequest(request);
			};
			
			var postSettings = function(settings) {
				var request = $.ajax({
					async : true,
					type : "POST",
					url : "/portal/rest/webrtc/webconferencing/settings",
					data : settings
				});
				return webConferencing.initRequest(request);
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
								// We want have same ID independently on who started the call
								var callId;
								//var callId = "p/" + context.currentUser.id + "@" + target.id;
								if (target.group) {
									// This should not happen until group calls will be supported
									callId = "g/" + (target.type == "chat_room" ? context.roomName : target.id);
								} else {
									// Sort IMs to have always the same ID (independently on who started the call)
									var parts = [context.currentUser.id, target.id];
									var partsAsc = parts.slice();
									partsAsc.sort();
									callId = "p/" + partsAsc.join("@");
								}
								var link = settings.callUri + "/" + callId;
								// TODO Disable call button if call already running
								//var disabledClass = hasJoinedCall(targetId) ? "callDisabled" : "";
								var $button = $("<a id='" + linkId + "' title='" + target.title + "'"
											+ " class='webrtcCallAction'>"
											+ "<i class='uiIcon callButtonIconVideo uiIconLightGray'></i>"
											+ "<span class='callTitle'>" + message("call") + "</span></a>");
								$button.click(function() {
									// Open a window for a new call
									var callWindow = webConferencing.showCallPopup(link, self.getTitle() + " " + message("call"));
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
											callWindow.document.title = message("callTo") + " " + target.title;
											callWindow.eXo.webConferencing.startCall(call).done(function(state) {
												log("<<<< Call " + state + " " + callId);
											}).fail(function(err) {
												webConferencing.showError(message("errorStartingCall"), webConferencing.errorText(err));
											});
										});
									}).fail(function(err) {
										log("ERROR adding " + callId + ": " + JSON.stringify(err));
										webConferencing.showError(message("errorAddCall"), webConferencing.errorText(err));
									});
								});
								button.resolve($button);
							}).fail(function(err) {
								log("Error getting context details for " + self.getTitle() + ": " + err);
								webConferencing.showWarn(message("errorGettingCall"), webConferencing.errorText(err));
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
			
			var acceptCallPopover_old = function(callerLink, callerAvatar, callerMessage, playRingtone) {
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
				$call = $("<div class='uiIncomingCall' title='" + message("incomingCall") + "'></div>");
				//<span class='ui-icon messageIcon' style='float:left; margin:12px 12px 20px 0;'></span>
				$call.append($("<div class='messageAuthor'><a target='_blank' href='" + callerLink + "' class='avatarMedium'>"
					+ "<img src='" + callerAvatar + "'></a></div>"
					+ "<div class='messageBody'><div class='messageText'>" + callerMessage + "</div></div>"));
				// eXo UX guides way
				//$call.append($("<ul class='singleMessage popupMessage'><li><span class='messageAuthor'>"
				//		+ "<a class='avatarMedium'><img src='" + callerAvatar + "'></a></span>"
				//		+ "<span class='messageText'>" + callerMessage + "</span></li></ul>"));
				$(document.body).append($call);
				var dialogSettings = {
					resizable: false,
					height: "auto",
					width: 400,
					modal: false,
					buttons: {}
				};
				dialogSettings.buttons[message("answer")] = function() {
			  	process.resolve("accepted");
			  	$call.dialog("close");
			  };
			  dialogSettings.buttons[message("decline")] = function() {
			  	process.reject("declined");
			  	$call.dialog("close");
				};
				$call.dialog(dialogSettings);
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
			
			var acceptCallPopover = function(callerLink, callerAvatar, callerMessage, playRingtone) {
				log(">> acceptCallPopover '" + callerMessage + "' caler:" + callerLink + " avatar:" + callerAvatar);
				var process = $.Deferred();
				var $call = $(".incomingCall");
				$call = $.extend($call, {
					close : function() {
						$call.hide();
						if (process.state() == "pending") {
							process.reject("closed");
						}
					}
				});
				$call.find(".avatar a").attr("href", callerLink);
				$call.find(".avatar img").attr("src", callerAvatar);
				$call.find(".messageText").text(callerMessage);
				$call.find(".uiIconClose").click(function () {
					$call.close();
				});
				$call.find(".answerButton").click(function () {
					process.resolve("accepted");
					$call.hide();
				});
				$call.find(".declineButton").click(function () {
					process.reject("declined");
					$call.hide();
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
				
				$call.show();
				return process.promise();
			};
			
			this.init = function(context) {
				
				messages = context.messages;
				
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
								$callPopup.close();
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
													var callerMessage = call.owner.title + " " + message("callingYou");
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
															var callWindow = webConferencing.showCallPopup(link, self.getTitle() + " " + message("call"));
															// Tell the window to start a call  
															onCallWindowReady(callWindow).done(function() {
																log(">>>> Call page loaded " + callId);
																callWindow.document.title = message("callWith") + " " + call.owner.title;
																callWindow.eXo.webConferencing.startCall(call).done(function(state) {
																	log("<<<< Call " + state + " " + callId);
																	lockCallButton(callerId, callerRoom);
																}).fail(function(err) {
																	webConferencing.showError(message("errorStartingCall"), webConferencing.errorText(err));
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
															webConferencing.showError(message("errorIncomingCall"), webConferencing.errorText(err));
														} else {
															webConferencing.showError(message("errorIncomingCall"), message("errorReadUserStatus"));
														}
													});
												}).fail(function(err, status) {
													log(">>> Call info error: " + JSON.stringify(err) + " [" + status + "]");
													if (err) {
														webConferencing.showError(message("errorIncomingCall"), webConferencing.errorText(err));
													} else {
														webConferencing.showError(message("errorIncomingCall"), message("errorReadCall"));
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
					process.resolve();
				} else {
					process.reject("WebRTC not supported in this browser: " + navigator.userAgent);
				}
				return process.promise();
			};
			
			this.showSettings = function(context) {
				log("> showSettings");
				
				var process = $.Deferred();
				
				// load HTML with settings
				var $popup = $("#webrtcSettingsPopup");
				if ($popup.length == 0) {
					$popup = $("<div class='uiPopupWrapper' id='webrtcSettingsPopup' style='display: none;'><div>");
					$(document.body).append($popup);
				}
				$popup.load("/portal/webrtc/settings", function(content, textStatus) {
					if (textStatus == "success" || textStatus == "notmodified") {
						var $settings = $popup.find(".settingsForm");
						var $iceServers = $settings.find(".iceServers");
						var $serverTemplate = $iceServers.find(".iceServer");
						// copy ICE servers from the working settings and use them for updates
						// Deep copy of the settings.rtcConfiguration as a working copy for the form 
						var rtcConfiguration = $.extend(true, {}, settings.rtcConfiguration);
						//activate tooltip
						$("#webrtcSettingsPopup [data-toggle='tooltip']").tooltip();
						function addIceServer(ices, $sibling) {
							var $ices = $serverTemplate.clone();
							// Fill URLs
							var $urlsGroup = $ices.find(".urlsGroup");
							$.each(ices.urls, function(ui, url) {
								var $urlGroup = $urlsGroup.find(".urlGroup").first();
								if (ui > 0) {
									$urlGroup = $urlGroup.clone();
									$urlGroup.find("i.uiIconPlus").remove();
									$urlGroup.find("i.uiIconTrash").remove();
									$urlsGroup.append($urlGroup);
								} else {
									$urlGroup.find("i.uiIconPlus").click(function() {
										// Add new ICE server (not URL - this unsupported for the moment)
										var newIces = {
											enabled : true,
											urls : [ "" ]
										};
										addIceServer(newIces, $ices); // add in DOM
										// add in RTC config
										rtcConfiguration.iceServers.push(newIces);
									});
									if ($iceServers.find(".iceServer").length == 1) { // 1 is for template
										// Remove trash on first server
										$urlGroup.find("i.uiIconTrash").remove();
									}
								}
								var $url = $urlGroup.find("input[name='url']");
								$url.val(url);
								$url.change(function() {
									ices.urls[ui] = $(this).val();
								});
							});
							// Server removal (trash icon) - do with confirmation
							$urlsGroup.find("i.uiIconTrash").click(function() {
								var $dialog = $popup.find(".serverRemovalDialog");
								$dialog.find(".removeButton").click(function() {
									// Remove this ICE server in DOM
									$ices.remove(); 
									// in RTC config
									rtcConfiguration.iceServers = rtcConfiguration.iceServers.filter(function(nextIces) {
										return ices !== nextIces;
									});
									$dialog.hide();
								});
								$dialog.find("a.uiIconClose, .cancelButton").click(function(){
									$dialog.hide();
								});
								$dialog.show();
							});
							// Fill username/credential
							var $credentialsGroup = $ices.find(".credentialsGroup");
							var $enabler = $credentialsGroup.find(".enabler input");
							var $credentials = $credentialsGroup.find(".credentials");
							$enabler.click(function() {
								var $username = $credentials.find("input[name='username']");
								var $credential = $credentials.find("input[name='credential']");
								if ($enabler.prop("checked") && !$credentials.is(":visible")) {
									if (typeof ices.username == "string" && typeof ices.credential == "string") {
										$username.val(ices.username);
										$credential.val(ices.credential);
									} else {
										$username.val("");
										$credential.val("");
										ices.username = "";
										ices.credential = "";
									}
									if (!$credentials.data("initialized")) {
										$credentials.data("initialized", true);
										$username.change(function() {
											ices.username = $(this).val();
										});
										$credential.change(function() {
											ices.credential = $(this).val();
										});
									}
									$credentials.show();
								} else {
									$credentials.hide();
									$username.val("");
									$credential.val("");
									ices.username = null;
									ices.credential = null;
								}
							});
							if (typeof ices.username == "string" && typeof ices.credential == "string") {
								$enabler.click();
							}
							$ices.show();
							if ($sibling) {
								$sibling.after($ices);
							} else {
								$iceServers.append($ices);
							}
							//activate tooltip for added servers
							$("#webrtcSettingsPopup [data-toggle='tooltip']").tooltip();
						}
						$.each(rtcConfiguration.iceServers, function(si, ices) {
							addIceServer(ices);
						});
						// Save action
						$settings.find(".saveButton").click(function() {
							setTimeout(function() { // timeout to let change events populate config object
								// Validation to do not have an ICE server w/o URL
								var confOk = true;
								for (var si=0; si<rtcConfiguration.iceServers.length; si++) {
									var is = rtcConfiguration.iceServers[si];
									for (var ui=0; ui<is.urls.length; ui++) {
										if (!is.urls[ui]) {
											// Empty URL
											confOk = false;
											break;
										}
									}
									if (!confOk) {
										break;
									}
								}
								if (confOk) {
									var rtcConfStr = JSON.stringify(rtcConfiguration);
									log("Saving RTC configuration: " + rtcConfStr);
									postSettings({
										rtcConfiguration : rtcConfStr
									}).done(function(savedRtcConfig) {
										$popup.hide();
										settings.rtcConfiguration = savedRtcConfig;
									}).fail(function(err) {
										log("ERROR saving settings", err);
										webConferencing.showError(message("admin.errorSavingSettings"), webConferencing.errorText(err));
									});
								} else {
									webConferencing.showWarn(message("admin.wrongSettings"), message("admin.serverUrlMandatory"));
								}
							}, 100);
						});
						$settings.find("a.uiIconClose, .cancelButton").click(function(){
							$popup.hide();
						});
						$popup.show();
					} else {
						log("ERROR loading settings page: " + content);
					}
				});
				
				return process.promise();
			};
		}

		var provider = new WebrtcProvider();

		// Add WebRTC provider into webConferencing object of global eXo namespace (for non AMD uses)
		if (globalWebConferencing) {
			globalWebConferencing.webrtc = provider;
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
