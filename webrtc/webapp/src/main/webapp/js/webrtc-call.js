/**
 * WebRTC call application (web page). This script initializes an UI of a page that will handle a particular call.
 */
if (eXo.webConferencing) {
	(function(webConferencing) {
		"use strict";
		
		/** For debug logging. */
		var log = webConferencing.getLog("webrtc").prefix("call");
		// log.trace("> Loading at " + location.origin + location.pathname);
		
		var isEdge = /Edge/.test(navigator.userAgent);
		var isFirefox = /Firefox/.test(navigator.userAgent);
		var isChrome = /Chrom(e|ium)/.test(navigator.userAgent);

		function alignLoader() {
			var $throber = $("#webrtc-call-starting>.waitThrobber");
			if ($throber.length > 0) {
				var newHeight = $(window).height() - 30; // 15px for margins top/bottom
				var oldHeight = $throber.height();
				if (newHeight > 0) {
					$throber.height(newHeight);
				}
			}
		}
		
		function addToContainer($content) {
			var $container = $("div.webrtc-call-container");
			if ($container.length == 0) {
				$container = $("<div class='webrtc-call-container' style='display: none;'></div>");
				var newHeight = $(window).height() - 30; // 15px for margins top/bottom
				var oldHeight = $container.height();
				if (newHeight > 0) {
					$container.height(newHeight);
				}
				$(document.body).append($container);
			}
			$container.append($content);
			$container.show();
		}

		function decodeMessage(msg) {
			var dmsg = decodeURIComponent(msg);
			return dmsg.replace(/\+/g, " ");
		}

		function showError(title, message) {
			$("#webrtc-call-conversation, #webrtc-call-title, #webrtc-call-starting").hide();
			var $error = $("#webrtc-call-error");
			if ($error.length == 0) {
				$error = $("<div id='webrtc-call-error'></div>");
				addToContainer($error);
			}

			// Append errors (for history)
			var $title = $("<h1 class='error-title'></h1>");
			$title.text(title);
			$error.append($title);
			var $description = $("<div class='error-description'></div>");
			$description.text(message);
			$error.append($description);
		}
		
		function showStarted(message) {
			var $starting = $("#webrtc-call-starting");
			$starting.empty();
			var $info = $(".startInfo");
			if ($info.length == 0) {
				$info = $("<div class='startInfo'></div>");
				$starting.append($info);
			}
			$info.text(message);
		}
		
		function objectUrl(obj) {
			if (window.URL) {
		  	return window.URL.createObjectURL(obj);
		  } else {
		  	return obj;
		  }
		}
		
		$(function() {
			alignLoader();
		});
		
		eXo.webConferencing.startCall = function(call) {
			var process = $.Deferred();
			$(function() {
				webConferencing.getProvider("webrtc").done(function(webrtc, initialized) {
					if (initialized) {
						if (webrtc.isSupportedPlatform()) {
							log.debug("Call page: " + location.origin + location.pathname);
							var callId = call.id;
							var isGroup = callId.startsWith("g/");
							if (isGroup) {
								log.warn("Group calls not supported: " + callId);
								showError("Warning", "Group calls not supported by WebRTC connector");
								setTimeout(function() {
									window.close();
								}, 7000);
								process.reject("Group calls not supported");
							} else {
								log.trace("Preparing call: " + callId);
								var currentUserId = webConferencing.getUser().id;
								var isOwner = currentUserId == call.owner.id;
								// TODO Use this for avatar when no video stream available
								//var callerLink = call.ownerLink;
								//var callerAvatar = call.avatarLink;
								
								$("#webrtc-call-starting").hide();
								$("#webrtc-call-container").show();
								var $convo = $("#webrtc-call-conversation");
								var $title = $("#webrtc-call-title > h1");
								$title.text(call.title);
								
								var $videos = $convo.find("#videos");
								var $remoteVideo = $videos.find("#remote-video");
								var remoteVideo = $remoteVideo.get(0);
								var $localVideo = $videos.find("#local-video");
								var localVideo = $localVideo.get(0);
								var $miniVideo = $videos.find("#mini-video");
								var miniVideo = $miniVideo.get(0);
								
								var $controls = $convo.find("#controls");
								$controls.addClass("active");
								$controls.show(); // TODO apply show/hide on timeout 
								var $hangupButton = $controls.find("#hangup");
								
								var handleError = function(title, message) {
									//userLog.error(title + ". " + message);
									showError(title, message);
								};
								
								// Page closing should end a call properly
								var pc;
								var stopping = false;
								var stopCall = function(localOnly) {
									// TODO here we also could send 'bye' message - it will work for 'Hang Up' button, 
									// but in case of page close it may not be sent to others, thus we delete the call here.
									if (!stopping) {
										stopping = true;
										// Play complete ringtone
										var $complete = $("<audio autoplay style='display: none;'>" 
													+ "<source src='/webrtc/audio/complete.mp3' type='audio/mpeg'>"  
													+ "Your browser does not support the audio element.</audio>");
										$(document.body).append($complete);
										function stopLocal() {
											if (pc) {
												try {
													pc.close();
												} catch(e) {
													log.warn("Failed to close peer connection", e);
												}											
											}
											window.removeEventListener("beforeunload", beforeunloadListener);
											window.removeEventListener("unload", unloadListener);
										}
										if (localOnly) {
											stopLocal();
										} else {
											//leavedCall(); // No sense to send 'leaved' for P2P, it is already should be stopped
											webrtc.deleteCall(callId).always(function() {
												stopLocal();
											});
										}									
									}
								};
								var beforeunloadListener = function(e) {
									stopCall();
								};
								var unloadListener = function(e) {
									stopCall();
								};
								window.addEventListener("beforeunload", beforeunloadListener);
								window.addEventListener("unload", unloadListener);
								
								var stopCallWaitClose = function(localOnly) {
									stopCall(localOnly);
									setTimeout(function() {
										window.close();
									}, 1500);
								};
								
								// Subscribe to user calls to know if this call updated/stopped remotely
							  webConferencing.onUserUpdate(currentUserId, function(update, status) {
									if (update.eventType == "call_state") {
										if (update.owner.type == "user") {
											if (update.callState == "stopped" && update.callId == callId) {
												if (!stopping) {
													log.info("Call stopped remotelly: " + update.callId);
													stopCallWaitClose(true);
												} // else, call already stopped by this user, but we want keep the window open to report errors 
											}
										}
									}
								}, function(err) {
									// TODO should we move all further logic to done promise?
									// Otherwise, in case of error subscribing user updates and remote call stopping
									// this window will stay open.
									log.warn("User calls subscription failure: " + callId, err);
									// we don't reject the progress promise as the call still may run successfully 
									webConferencing.showWarn(webrtc.message("errorSubscribeUser"), err);
								});
								
								// WebRTC connection to establish a call connection
								log.trace("Creating RTC peer connection for " + callId);
								try {
									var rtcConfig = webrtc.getRtcConfiguration();
									// Clean to keep only meaningful fields
									if (!rtcConfig.bundlePolicy) {
										delete rtcConfig.bundlePolicy;
									}
									if (!rtcConfig.iceTransportPolicy) {
										delete rtcConfig.iceTransportPolicy;
									}
									if (rtcConfig.iceCandidatePoolSize <= 0) {
										delete rtcConfig.iceCandidatePoolSize;
									}
									if (isEdge) {
										// XXX Edge doesn't support STUN yet? Only TURN will work.
										// https://msdn.microsoft.com/en-us/library/mt502501(v=vs.85).aspx
										var onlyTurn = [];
										for (var i=0; i<rtcConfig.iceServers.length; i++) {
											var server = rtcConfig.iceServers[i];
											var newUrls = [];
											for (var ui=0; ui<server.urls.length; ui++) {
												var url = server.urls[ui];
												if (url.startsWith("turn")) {
													// use it
													newUrls.push(url);
												}
											}
											if (newUrls.length > 0) {
												server.urls = newUrls;
												onlyTurn.push(server);
											}
										}
										rtcConfig.iceServers = onlyTurn;
									}
									// Also clean not actually meaningful fields
									for (var i=0; i<rtcConfig.iceServers.length; i++) {
										var server = rtcConfig.iceServers[i];
										delete server.enabled;
										// username and credential can be empty strings
										if (typeof server.username != "string") {
											delete server.username;
										}
										if (typeof server.credential != "string") {
											delete server.credential;
										}
									}
									
									//log.trace("WebRTC configuration: " + JSON.stringify(rtcConfig));
									log.trace("Creating RTCPeerConnection");
									pc = new RTCPeerConnection(rtcConfig);
									var negotiation = $.Deferred();
									var connection = $.Deferred();
									
									// Play incoming ringtone for the call owner until complete negotiation
									if (isOwner) {
										var $ring = $("<audio loop autoplay style='display: none;'>" 
													+ "<source src='/webrtc/audio/echo.mp3' type='audio/mpeg'>"  
													+ "Your browser does not support the audio element.</audio>");
										$(document.body).append($ring);
										negotiation.then(function() {
											$ring.remove();
										});
									}
									
									connection.fail(function(err) {
										log.error("Failed to start connection for: " + callId, err);
										err = webConferencing.errorText(err);
										process.reject(webrtc.message("errorStartingConnection") + ". " + webConferencing.errorText(err)); 
										showError(webrtc.message("errorStartingConnection"), webConferencing.errorText(err));
										// we don't stop the call to keep the window open and let user report an error 
									});
									
									var sendMessage = function(message) {
										return webConferencing.toCallUpdate(callId, $.extend({
							    		"provider" : webrtc.getType(),
							    		"sender" : currentUserId,
							    		"host" : isOwner
							      }, message));
									};
									var sendHello = function() {
										// It is a first message send on the call channel by a peer, 
										// it tells that the end is ready to exchange other information (i.e. accepted the call)
										// If it will be required to change a host in future, then this message should be send
										// by a new host with '__all__' content, others should understand this and update their 
										// owner ID to this sender ID.
										return sendMessage({
							    		"hello": isOwner ? "__all__" : call.owner.id
							      }).fail(function(err) {
											log.error("Failed to send Hello for " + callId, err);
											showError("Error starting call", webConferencing.errorText(err));
										});
									};
									var sendBye = function() {
										// TODO not used
										// It is a last message send on the call channel by a peer, 
										// other side should treat is as call successfully ended and no further action required (don't need delete the call)
										return sendMessage({
							    		"bye": isOwner ? "__all__" : call.owner.id
							      }).fail(function(err) {
											log.error("Failed to send Bye for " + callId, err);
										});
									};
									var sendOffer = function(localDescription) {
										return sendMessage({
							    		"offer": JSON.stringify(localDescription)
							      }).done(function() {
							      	log.debug("Published offer for " + callId + " " + JSON.stringify(localDescription));
										}).fail(function(err) {
											log.error("Failed to send offer for " + callId, err);
											// TODO May retry?
											showError("Error of sharing media (offer)", webConferencing.errorText(err));
										});
									};
									var sendAnswer = function(localDescription) {
										return sendMessage({
							    		"answer": JSON.stringify(localDescription)
							      }).done(function() {
							      	log.debug("Published answer for " + callId + " " + JSON.stringify(localDescription));
										}).fail(function(err) {
											log.error("Failed to send answer for " + callId, err);
											showError("Error of sharing media (answer)", webConferencing.errorText(err));
										});
									};
									var sendCandidate = function(candidate) {
										return sendMessage({
							        "candidate" : candidate
							      }).done(function() {
							      	log.debug("Published candidate for " + callId + " " + JSON.stringify(candidate));
										}).fail(function(err) {
											log.error("Failed to send candidate for " + callId, err);
											showError("Error of sharing connection", webConferencing.errorText(err));
										});
									};
									
									// 'Hang Up' also ends call properly
									$hangupButton.click(function() {
										stopCallWaitClose();
									});
									
									// Save user state for audio/video mute in local storage
									var preferenceKey = function(name) {
										return currentUserId + "@exo.webconferencing.webrtc." + name;
									};
									var savePreference = function(name, value) {
										localStorage.setItem(preferenceKey(name), value);
									};
									var getPreference = function(name) {
										return localStorage.getItem(preferenceKey(name));
									};
									
									// Add peer listeners for connection flow
									pc.onicecandidate = function (event) {
										// This will happen when browser will be ready to exchange peers setup
										log.debug("ICE candidate ready for " + callId);
										connection.then(function() {
									    if (event.candidate) {
									    	sendCandidate(event.candidate);
									    } else {
									      // else All ICE candidates have been sent. ICE gathering has finished.
									    	// Send empty candidate as a sign of finished ICE gathering.
									    	sendCandidate({}).done(function() {
									    		log.debug("All ICE candidates have been sent");
									    	});
									    }
										});
								  };
								  var sdpConstraints = {
						  			"offerToReceiveAudio": true, 
					  				"offerToReceiveVideo": false/*,
						  			"mandatory": { 
						  				"OfferToReceiveAudio": true, 
						  				"OfferToReceiveVideo": true
						  			}*/
								  };
								  if (isEdge) {
								  	sdpConstraints = {}; // TODO in fact even undefined doesn't fit, need call without a parameter
								  }
							  	// let the 'negotiationneeded' event trigger offer generation
								  pc.onnegotiationneeded = function () {
								  	// This will be fired after adding a local media stream and browser readiness
								  	log.debug("Negotiation needed for " + callId);
								  	
								  	// Ready to join the call: say hello to each other
						  			if (isOwner) {
						  				sendHello().then(function() {
							  				log.debug("Sent Hello by " + (isOwner ? "owner" : "participant") + " for " + callId);
							  			});
								  		// Owner will send the offer when negotiation will be resolved (received Hello from others)
						  				negotiation.then(function() {
						  					log.debug("Creating offer for " + callId);
										    pc.createOffer().then(function(desc) { // sdpConstraints
										    	log.debug("Setting local description for " + callId);
										    	pc.setLocalDescription(desc).then(function() {
										    		log.debug("Sending offer for " + callId);
										    		sendOffer(pc.localDescription).then(function() {
										    			log.debug("Sent offer for " + callId);
										    			// TODO Something else here?
										    		});
										      }).catch(function(err) {
										      	log.error("Failed to set local description for " + callId, err);
										      	showError("Error of preparing connection", webConferencing.errorText(err));
											    });
										    }).catch(function(err) {
										    	log.error("Failed to create an offer for " + callId, err);
										    	showErroror("Error of starting connection", webConferencing.errorText(err));
										    });
						  				});
								  	}
								  };			  	
								  // once remote stream arrives, show it in the remote video element
								  // TODO it's modern way of WebRTC stream addition, but it doesn't work in Chrome
								  /*pc.ontrack = function(event) {
								  	log.trace(">>> onTrack for " + callId + " > " + new Date().toLocaleString());
								  	$remoteVideo.get(0).srcObject = event.streams[0];
								  };*/
									pc.onaddstream = function (event) { 
										// Remote video added: switch local to a mini and show the remote as main
										log.debug("Added stream for " + callId);
										// Stop local
										localVideo.pause();
										$localVideo.removeClass("active");
										$localVideo.hide();
										
										// Show remote
										remoteVideo.srcObject = event.stream;
										$remoteVideo.addClass("active");
										$remoteVideo.show();
										
										// Show local in mini
										miniVideo.srcObject = localVideo.srcObject;
										localVideo.srcObject = null;
										$miniVideo.addClass("active");
										$miniVideo.show();
										
										//
										$videos.addClass("active");
									};
								  pc.onremovestream = function(event) {
								  	// TODO check the event stream URL before removal?
								  	log.debug("Removed stream for " + callId);
								  	// Stop remote
								  	remoteVideo.pause();
										$remoteVideo.removeClass("active");
										$remoteVideo.hide();
										remoteVideo.srcObject = null;
										
										// Show local
										localVideo.srcObject = miniVideo.srcObject;
										$localVideo.addClass("active");
										
										// Hide mini
										miniVideo.srcObject = null;
										$miniVideo.removeClass("active");
										$miniVideo.hide();
										
										//
										$videos.removeClass("active");
								  };
									
									// Subscribe to the call updates
									var listener = webConferencing.onCallUpdate(callId, function(message) {
										if (message.provider == webrtc.getType()) {
											if (message.sender != currentUserId) {
												log.trace("Received call update for " + callId + ": " + JSON.stringify(message));
												if (message.candidate) {
													// ICE candidate of remote party (can happen several times)
													log.debug("Received candidate for " + callId + ": " + JSON.stringify(message.candidate));
													if (Object.getOwnPropertyNames(message.candidate).length > 0 || isEdge) {
														connection.then(function() {
															log.debug("Creating candidate for " + callId);
															var candidate = new RTCIceCandidate(message.candidate);
															log.debug("Adding candidate for " + callId);
															pc.addIceCandidate(candidate).then(function() {
															  log.debug("Added candidate for " + callId);
															}).catch(function(err) {
																log.error("Failed to add candidate for " + callId, err);
																showError("Error establishing call", webConferencing.errorText(err));
															});														
														});
													} else {
														log.debug("All candidates received for " + callId);
													}
												} else if (message.offer) {
													log.debug("Received offer for " + callId);
													// Offer of a caller on callee side
													if (isOwner) {
														log.warn("Unexpected offer received on owner side for " + callId);
													} else {
														try {
															var offer = JSON.parse(message.offer);
															if (isEdge) {
																offer = new RTCSessionDescription(offer);
															}
															negotiation.then(function(localStream) {
																log.debug("Setting remote description (offer) for " + callId);
																pc.setRemoteDescription(offer).then(function() {
														      // if we received an offer, we need to answer
														      if (pc.remoteDescription.type == "offer") {
														      	// Add local stream for participant after media negotiation (as in samples) 
														      	log.debug("Adding local stream for " + callId);
														      	pc.addStream(localStream); // XXX It's deprecated way but Chrome works using it
														      	// Will it be better to do this in onnegotiationneeded event?
														      	log.debug("Creating answer for " + callId);
														      	pc.createAnswer().then(function(desc) { // sdpConstraints?
														      		log.debug("Setting local description for " + callId);
														      		pc.setLocalDescription(desc).then(function() {
														      			log.debug("Sending answer for " + callId);
														      			sendAnswer(pc.localDescription).then(function() {
														      				log.debug("Sent answer for " + callId);	
														      				connection.resolve().then(function() {
														      					// Participant ready to exchange ICE candidates
																						log.debug("Started exchange network information with peers for " + callId);
																					});
														      			});
														      		}).catch(function(err) {
														      			log.error("Failed to set local description (answer) for " + callId, err);
															      		showError("Error accepting call", webConferencing.errorText(err));
														      		});
														      	}).catch(function(err) {
														      		log.error("Failed to create an answer for " + callId, err);
														      		showError("Error answering call", webConferencing.errorText(err));
														      	});
														      } else {
														      	log.error("Remote description type IS NOT 'offer' BUT '" + pc.remoteDescription.type + "'. Call state not defined.");
														      }
														    }).catch(function(err) {
														    	log.error("Failed to set remote description (offer) for " + callId, err);
														    	showError("Error applying call", webConferencing.errorText(err));
														    });
															});
														} catch(e) {
															log.error("Error processing offer for " + callId + ": " + JSON.stringify(message.offer), e);
														}
													}
												} else if (message.answer) {
													if (isOwner) {
														// Answer of a callee to the caller: it's final stage of the parties discovery
														log.debug("Received answer for " + callId);
														try {
															var answer = JSON.parse(message.answer);
															if (isEdge) {
																answer = new RTCSessionDescription(answer);
															}
															negotiation.then(function() {
																log.debug("Setting remote description for " + callId);
																pc.setRemoteDescription(answer).then(function() {
														      log.debug("Apllied remote description (answer) for " + callId);
														      // Resolve connection (network) exchange only from here
														      connection.resolve().then(function() {
														      	// Owner ready to exchange ICE candidates
																		log.debug("Started exchange network information with peers for " + callId);
																	});
														    }).catch(function(err) {
														    	log.error("Failed to set remote description (answer) for " + callId, err);
														    	showError("Error answering call", webConferencing.errorText(err));
														    });
															});
														} catch(e) {
															log.error("Error processing answer for " + callId + ": " + JSON.stringify(message.answer), e);
														}
													} else {
														// FYI this could be OK to receive for group call
														log.error("Unexpected answer received on participant side of " + callId);
													}
												} else if (message.hello) {
													// To start the call send "hello" - first message in the flow for each client
													log.debug("Received Hello for " + callId);
													if (message.hello == currentUserId) {
														// We assume it's a hello to the call owner: start sending offer and candidates
														// This will works once (for group calls, need re-initialize the process)
														negotiation.resolve().then(function() {
															log.debug("Started exchange (owner) media information of " + callId);
														});
													} else {
														log.debug("Hello was not to me (" + message.hello + ")");
													}
												} else if (message.bye && false) {
													// TODO not used
													// Remote part leaved the call: stop listen the call
													log.debug("Received Bye for " + callId);
													if (message.bye == currentUserId || message.bye == "__all__") {
														// We assume it's a Bye from the call owner or other party to us: ends the call locally and close the window
														stopCall(true);
														listener.off();											
													} else {
														log.debug("Bye was not to me (" + message.bye + ")");
													}
												} else {
													log.error("Received unexpected message for " + callId);
												}
											} else {
												//log.trace("<<< skip own update");
											}
										}
									}, function(err) {
										log.error("Call subscribtion failed for " + callId, err);
										err = webConferencing.errorText(err);
										process.reject(webrtc.message("errorSubscribeCall") + ": " + err);
										showError(webrtc.message("errorSubscribeCall"), webConferencing.errorText(err));
									});
									
									// Show current user camera in the video,
									var inputsReady = $.Deferred();
									try {
										navigator.mediaDevices.enumerateDevices().then(function(devices) {
											// device it's MediaDeviceInfo
											var cams = devices.filter(function(device) { 
												return device.kind == "videoinput";
											});
											var mics = devices.filter(function(device) { 
												return device.kind == "audioinput";
											});
									    
											var constraints = {
											};
									    if (mics.length > 0) {
									    	constraints.audio = true;
									    	if (cams.length > 0) {
									    		// TODO use optimal camera res and video quality
									    		// 720p (1280x720) is an optimal for all cases
									    		// then 960x720, 640x480 and 320x240, 160x120
									    		var vw, vh, vwmin, vhmin; 
									    		var isPortrait = screen.width < screen.height; 
									    		/*if (screen.width >= 1280) {
									    			vw = 1280;
									    			vh = 720;
									    		} else */if (screen.width >= 640) {
									    			vw = 640;
									    			vh = 480;
									    		} else {
									    			vw = 320;
									    			vh = 240;
									    		}
									    		var videoSettings = {
												  	width: { min: isPortrait ? vh : vw, ideal: isPortrait ? 720 : 1280 }
												  	//height: { min: 480, ideal: vh } // 360? it's small mobile like Galaxy S7
												  };
									    		//constraints.video = true;
										    	constraints.video = videoSettings;
									    		if (typeof constraints.video === "object") {
									    			try {
															var supportedConstraints = navigator.mediaDevices.getSupportedConstraints();
															if (supportedConstraints.hasOwnProperty("facingMode")) {
																constraints.video.facingMode = "user"; // or { exact: "user" }
															}
														} catch(e) {
															log.warn("MediaDevices.getSupportedConstraints() failed", e);
															// constraints.video = true; // TODO set this?
														}							    			
									    		}
										    } else {
										    	constraints.video = false;
										    }
									    	inputsReady.resolve(constraints, "Found audio" + (constraints.video ? " and video" : ""));
									    } else {
									    	inputsReady.reject(webrtc.message("noAudioFound") + "." + (cams.length > 0 ? " " + webrtc.message("butVideoFound") + "." : ""));
									    }
										});
									} catch(e) {
										log.warn("MediaDevices.enumerateDevices() failed", e);
										inputsReady.resolve({
											audio : true,
											video : true
										}, "Unable read devices, go with default audio and video.");
									}
									inputsReady.done(function(constraints, comment) {
										log.debug("Media constraints: " + JSON.stringify(constraints) + " " + comment);
										navigator.mediaDevices.getUserMedia(constraints).then(function(localStream) {
											// successCallback
											// show local camera output
											localVideo.srcObject = localStream;
											$localVideo.addClass("active");
											
											var enableAudio = function(newValue) {
												var enabled;
												var audioTracks = localStream.getAudioTracks();
												if (audioTracks.length > 0) {
													for (var i = 0; i < audioTracks.length; ++i) {
														audioTracks[i].enabled = typeof newValue == "boolean" ? newValue : !audioTracks[i].enabled;
												  }
													enabled = typeof newValue == "boolean" ? newValue : audioTracks[0].enabled;
												} else {
													enabled = typeof newValue == "boolean" ? newValue : true;
												}
												log.info("Audio " + (enabled ? "un" : "") + "muted for " + callId);
												return enabled;
											};
											var $muteAudio = $controls.find("#mute-audio");
											$muteAudio.click(function() {
												savePreference("audio.disable", new String(!enableAudio()));
												$muteAudio.toggleClass("on");
											});
											var enableVideo = function(newValue) {
												var enabled;
												var videoTracks = localStream.getVideoTracks();
												if (videoTracks.length > 0) {
													for (var i = 0; i < videoTracks.length; ++i) {
														videoTracks[i].enabled = typeof newValue == "boolean" ? newValue : !videoTracks[i].enabled;
												  }
													enabled = typeof newValue == "boolean" ? newValue : videoTracks[0].enabled;
												} else {
													enabled = typeof newValue == "boolean" ? newValue : true;
												}
												log.info("Video " + (enabled ? "un" : "") + "muted for " + callId);
												return enabled;
											};
											var $muteVideo = $controls.find("#mute-video");
											$muteVideo.click(function() {
												savePreference("video.disable", new String(!enableVideo()));
												$muteVideo.toggleClass("on");
											});

										  log.info("Starting call " + callId);
										  // add local stream for owner right now
										  if (isOwner) {
										  	log.debug("Adding local (owner) stream for " + callId);
											  pc.addStream(localStream); 
											  // XXX It's deprecated way but Chrome works using it
											  //localStream.getTracks().forEach(function(track) {
											  //  pc.addTrack(track, localStream);
											  //});
										  } else {
										  	// Participant sends Hello to the other end to initiate a negotiation there
										  	log.debug("Sending Hello by participant for " + callId);
										  	sendHello().then(function() {
								  				log.debug("Sent Hello by participant for " + callId);
								  				// Participant on the other end is ready for negotiation and waits for an offer message
										  		negotiation.resolve(localStream).then(function() {
														log.debug("Started exchange (participant) media information for " + callId);
													});
								  			});
										  }
											// TODO should we do this only on connection done, in the resolved promise below?
											// if user had saved audio/video disabled, mute them accordingly
											if (getPreference("audio.disable") == "true") {
												enableAudio(false);
												$muteAudio.addClass("on");
											}
											if (getPreference("video.disable") == "true") {
												enableVideo(false);
												$muteVideo.addClass("on");
											}
											connection.then(function() {
												webConferencing.updateUserCall(callId, "joined").done(function() {
													log.debug("Joined a call " + callId);
												}).fail(function(err) {
													log.error("Error joining call " + callId, err);
												});
											});
										}).catch(function(err) {
											log.error("User media error: " + JSON.stringify(err), err);  
											showError(webrtc.message("mediaDevicesError"), webConferencing.errorText(err));
										});
									}).fail(function(err) {
										log.error("Media devices discovery failed", err);
										showError(webrtc.message("audioVideoRequired"), webConferencing.errorText(err));
									});
									// Resolve this in any case of above media devices discovery result
									process.resolve("started");
								} catch(err) {
									log.error("Failed to create RTC peer connection for " + callId, err);
									process.reject(webrtc.message("connectionFailed"), err);
									showError(webrtc.message("connectionFailed"), webConferencing.errorText(err));
									stopCall();
								}
							}
						} else {
							log.error("WebRTC call not supported in this browser: " + navigator.userAgent);
							process.reject(webrtc.message("yourBrowserNotSupportWebrtc") + ": " + navigator.userAgent);
							showError(webrtc.message("notSupportedPlatform"), webrtc.message("yourBrowserNotSupportWebrtc") + ".");
						}						
					} else {
						log.error("Provider not initialized");
						process.reject(webrtc.message("notInitialized") + ": " + navigator.userAgent);
						showError(webrtc.message("providerError"), webrtc.message("notInitialized") + ".");
					}
				}).fail(function(err) {
					log.error("Provider not available", err);
					process.reject(webrtc.message("providerNotAvailable"), err);
				});
			});
			return process.promise();
		};
		
		log.trace("< Loaded at " + location.origin + location.pathname);
	})(eXo.webConferencing);
} else {
	window.console && window.console.log("eXo.webConferencing not defined for webrtc-call.js");
}
