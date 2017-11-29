<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
	<meta charset="UTF-8">
	<title>My Call</title>
	<!-- My Connector call page style -->
	<link href="/myconnector/skin/myconnector-call.css" rel="stylesheet" type="text/css"/>
	<!-- jQuery from Platform -->
	<script type="text/javascript" src="/eXoResources/javascript/jquery-3.2.1.js"></script>
	<!-- CometD from Platform, required by Web Conferencing module -->
	<script type="text/javascript" src="/cometd/org/cometd.js"></script>
	<script type="text/javascript" src="/cometd/jquery/jquery.cometd.js"></script>
	<!-- jQueryUI and Pnotify from Web Conferencing, require by it -->
	<script type="text/javascript" src="/webconferencing/js/jquery-ui.min.js"></script>
	<script type="text/javascript" src="/webconferencing/js/jquery.pnotify.min.js"></script>
		<!-- My Connector call page scripts -->
	<script type="text/javascript">
	  // Deferred object that will be resolved when all scripts below loaded and then used by startCall() below. 
	  var loading = $.Deferred();
		// Report any error to browser log
		loading.fail(function(message, e) {
			if (e) {
				window.console && window.console.log(message, e);
			} else {
				window.console && window.console.log(message);
			}
		});
	  
		// We do a workaround: starting from Cometd3 helper module, we need simulate AMD loading as in the Platform portal then 
	  // initialize Web Conferencing and My Connector provider modules.
		$.get("/cometd/javascript/eXo/commons/commons-cometd3.js").done(function(code) {
			try {
				// we got the script code and need evaluate it to obtain the module in a variable
				window.cCometD = eval(code);
				// then we just load the scripts in order of dependency
				$.getScript("/webconferencing/js/webconferencing.js").done(function(data) {
					window.console && window.console.log("OK webconferencing.js loaded");
					$.getScript("/myconnector/js/webconferencing-myconnector.js").done(function(data) {
						window.console && window.console.log("OK webconferencing-myconnector.js loaded");
						// Wait a bit for scripts finishing load/initialize
						if (eXo.webConferencing) {
							(function(webConferencing) {
							  "use strict";
							  // userInfo and contextInfo set in Call servlet
							  webConferencing.init(${userInfo}, ${contextInfo});
							  webConferencing.myconnector.configure(${settings}); 
							  webConferencing.addProvider(webConferencing.myconnector);
							  // Resolve with webConferencing instance
						  	loading.resolve(webConferencing);							  	
							})(eXo.webConferencing);
						} else {
							loading.reject("eXo.webConferencing not defined for My Call page");
						}
					}).fail(function(err) {
						loading.reject("Error loading webconferencing-myconnector.js: " + err);
					});
				}).fail(function(err) {
					loading.reject("Error loading webconferencing.js: " + err);
				});
			} catch(e) {
				loading.reject("Error evaluating cometd3.js: " + e, e);
			}
		}).fail(function(err) {
			loading.reject("Error loading cometd3.js: " + err);
		});
		
		// Used by Close button and on remote stop even
		var closeWindow = function() {
			setTimeout(function() {
				window.close();
			}, 1000);
		};
	  
		// A method that should be called from external page to start a call
	  window.startCall = function(call, isNewCall) {
			// Deferred to return to the caller and inform about a status (done or error)
	  	var start = $.Deferred();
	  	var callId = call.id;
			// Set title of the call
			$("#myconnector-call-title h1").text(call.title);
			loading.done(function(webConferencing) {
				// When Web Conferencing module loaded we use it.
				
				/** For debug logging. */
				var objId = Math.floor((Math.random() * 1000) + 1);
				var logPrefix = "[mycall_" + objId + "] ";
				var log = function(msg, e) {
					webConferencing.log(msg, e, logPrefix);
				};
				
				var myconnector = webConferencing.myconnector;
			
				var currentUserId = webConferencing.getUser().id;
				// here we rely on our logic from myconnector module script: group call ID starts with 'g/'
				var isGroup = callId.startsWith("g/"); 

				// Ringtone for outgoing call indication
				var $ring;
				var stopRingtone = function() {
					if ($ring) {
						$ring.remove();
					}
				};
				
				// Delete call on window close and on Close button click
				var stopped = false;
				var stopCall = function() {
					// We consult the 'stopping' flag as it may be invoked twice (we have two unload listeners) 
					if (!stopped) {
						stopped = true;
						// Stop ringtone for already stopped call
						stopRingtone();
						var $done = $("<audio autoplay style='display: none;'>" 
									+ "<source src='/myconnector/audio/done.mp3' type='audio/mpeg'>"  
									+ "Your browser does not support the audio element.</audio>");
						$(document.body).append($done);
						
						// If it's 1:1 call we can freely delete it, but for group calls we leave them - 
						// call will be deted by the server and it send such message to all subscribed clients (see below).
						if (isGroup) {
							webConferencing.updateUserCall(callId, "leaved").fail(function(err, status) {
								log("<< ERROR leaving call: " + callId + ". " + JSON.stringify(err) + " [" + status + "]");
							});							
						} else {
							webConferencing.deleteCall(callId).done(function() {
								log("<< Deleted " + callId);
							}).fail(function(err) {
								if (err && (err.code == "NOT_FOUND_ERROR" || (typeof(status) == "number" && status == 404))) {
									// already deleted
									log("<< Call not found " + callId);
								} else {
									log("ERROR deleting call " + callId + ": " + JSON.stringify(err));
								}
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
				
				// Close button - to stop the call
				$(".call-close").click(function() {
					stopCall();
				});
				
				// Subscribe to user calls to know if this call updated/stopped remotely
			  webConferencing.onUserUpdate(currentUserId, function(update, status) {
					if (update.eventType == "call_state") {
						var callId = update.callId;
						if (update.callState == "stopped" && update.callId == callId) {
							log(">>> Call stopped remotelly: " + JSON.stringify(update) + " [" + status + "]");
							$(".status").text("Call stopped.");
							stopCall();
							closeWindow();
						}							
					} else if (update.eventType == "call_joined") {
						if (currentUserId != update.part.id) {
							// Stop ringtone if some user joined the call
							log(">>> Call joined remotelly: " + JSON.stringify(update) + " [" + status + "]");
							stopRingtone();
						}
						$(".status").text("Call started.");
					} else if (update.eventType == "call_leaved") {
						if (currentUserId != update.part.id) {
							log(">>> Call leaved remotelly: " + JSON.stringify(update) + " [" + status + "]");
							// TODO action on user leaving, but take in accoun that group call will be stopped on the server side
							// when last user leave it - don't need stop it explicitly.
						}
					}
				}, function(err) {
					log("ERROR User calls subscription failure: " + err, err);
				});
				
			  if (isNewCall) {
					// For a new call it's outgoing call and we play such ringtone until the call will be accpted or declined.
					$ring = $("<audio loop autoplay style='display: none;'>" 
								+ "<source src='/myconnector/audio/outgoing.mp3' type='audio/mpeg'>"  
								+ "Your browser does not support the audio element.</audio>");
					$(document.body).append($ring);
				} else {
					// For incoming call mark a call as joined - do this if only have added the call before
					webConferencing.updateUserCall(callId, "joined").fail(function(err, status) {
						log("<< Error joining call: " + callId + ". " + JSON.stringify(err) + " [" + status + "]");
					});
				}
			  
			  // You also can exchange any data (in JSON) between caller and parties, as well as any other apps related the call.
			  // WebConferencing offers subscription mechanism (based on CometD).
			  // Subscribe to messages (for yourData object content)
			  webConferencing.onCallUpdate(callId, function(message) {
					if (message.provider == myconnector.getType()) {
						if (message.sender != currentUserId) {
							log(">>> Received call update for " + callId + ": " + JSON.stringify(message));
							if (message.yourData) {
								// TODO work with your data
							}
						}
					}
			  });
			  // Publish messages (with yourData object content)
			  // To distinguish all Web Conferencing clients (each user page/browser/app etc.) you may need an unique ID for each.
			  // Here we build it using API client ID from settings on server side appended by random number, but you can apply any logic here.
			  // In your implementation you may exchange connectivity settings this way as well as any business logic data (e.g. texting or
			  // documents exchange).
			  var rnd = Math.floor((Math.random() * 10000) + 1);
			  var clientId = myconnector.getClientId() + "-" + rnd;
				webConferencing.toCallUpdate(callId, {
	    		"provider" : myconnector.getType(), // required field
	    		"sender" : currentUserId,  // required field, see use of onCallUpdate() above
	    		"yourData" : { // your data here
	    			"myCallKey" : currentUserId + "@" + clientId
	    		}
				}).done(function() {
	      	log("<< Sent call update");
	      });
			  
				start.resolve(isNewCall ? "started" : "joined");
			});
			return start.promise();
		};
		
		$(function() {
			$(".call-close").click(function() {
				// Also close the window on stop, but give it a moment to stop a call.
				closeWindow();
			});			
		});
	</script>	
</head>
<body>
	<div id="myconnector-call-container">
		<div id="myconnector-call-title"><h1></h1></div>
		<div id="myconnector-call-conversation">
			<div class="description">It is an empty call page. Place your video call handler here.</div>
			<div class="placeholder"></div>
			<div class="status">Calling...</div>
		</div>
		<div id="myconnector-call-controls">
			<button class="call-close">Close</button>			
		</div>
	</div>
</body>
</html>