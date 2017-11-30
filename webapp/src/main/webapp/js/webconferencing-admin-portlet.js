/**
 * WebConferencing Admin portlet in eXo Platform.
 */
(function($, webConferencing) {
	"use strict";

	/** For debug logging. */
	var objId = Math.floor((Math.random() * 1000) + 1);
	var logPrefix = "[webconferencing_admin_" + objId + "] ";
	var log = function(msg, e) {
		webConferencing.log(msg, e, logPrefix);
	};
	// log("> Loading at " + location.origin + location.pathname);

	/**
	 * Admin class.
	 */
	function Admin() {

		var self = this;

		this.init = function() {
			// UI init and action handlers
			/**
			 * OPEN HIDE POPUPs String id; contain the Id of element to duplicate INT inputID; Contain the number of elements
			 * 
			 * TODO it's WebRTC provider related stuff, move to the connector code 
			 */
			function addInput(id, inputID) {
				var domToAdd = $("#" + id + " > .group-container" + inputID).html();
				newId = $('#urlGroup > div').length;
				console.log(newId);
				$("#" + id).append("<div class='group-container" + newId + "'>" + domToAdd + "</div>");
				$("#" + id + " > .group-container" + newId + " > input").attr("id", "url" + newId);
				$("#" + id + " > .group-container" + newId + " > i.uiIconPlus ").attr("onclick",
							"addInput('urlGroup', " + newId + ")");

				if ($("#" + id + " > .group-container" + newId + " > i.uiIconTrash").length > 0) {
					$("#" + id + " > .group-container" + newId + " > i.uiIconTrash").attr("onclick",
								"removeInput(" + newId + ")");
				} else {
					$("<i class='uiIconTrash uiIconLightGray' onclick='removeInput(" + newId + ")'>&nbsp;</i>")
								.insertBefore("#" + id + " > .group-container" + newId + " > i.uiIconPlus ");
				}
			}

			/**
			 * TODO it's WebRTC provider related stuff, move to the connector code
			 */
			function removeInput(inputID) {
				$(".group-container" + inputID).remove();
			}
			
			//////

			$(function() {
				log("Initializing Web Conferencing Admin "); // webConferencing.getUser().id
				
				var $admin = $("#webconferencingAdmin");
				var $tbody = $admin.find(".content table tbody");
				var $template = $tbody.find(".callProvider.template");
				
				// Load available providers: take in account that provider modules loading asynchronously in parallel to this call.
				webConferencing.getProvidersConfig().done(function(configs) {
					$.each(configs, function(i, conf) {
						var $provider = $template.clone();
						$provider.removeClass("template");
						$provider.find(".title").text(conf.title);
						$provider.find(".description").html(conf.description);
						
						// Activation handler
						var $checkbox = $provider.find(".active input[type=checkbox]");
						$checkbox[0].checked = conf.active;
						$checkbox.click(function() { // TODO $.change ?
							var checkbox = this;
							webConferencing.postProviderConfig(conf.type, this.checked).done(function(conf) {
								// Ensure we show actual value
								if (conf.active != checkbox.checked) {
									checkbox.checked = conf.active; 
								}
							}).fail(function(err) {
								log("ERROR updating provider configuration");
							});
						});
						
						// Action handles
						var $actions = $provider.find(".actions");
						// Settings (optional)
						var $settings = $actions.find(".settings");
						webConferencing.getProvider(conf.type).done(function(provider) {
							if (provider.showSettings && provider.hasOwnProperty("showSettings")) {
								$settings.click(function() {
									provider.showSettings(); // TODO context will be valuable?
								});
								$settings.show();
							}							
						}).fail(function(err) {
							log("WARN Provider not available " + conf.type + ": " + err);
						});
						
						// Add provider to the table
						$tbody.append($provider);
						$provider.show();
					});	
				}).fail(function(err) {
					log("ERROR loading providers configuration: ", err);
				});	
				
				/////
				// TODO it's WebRTC provider related stuff, move to the connector code
				/* OPEN HIDE POPUPs */
				// selector was #webConfPopupClique
				$(".callProvider > .actions > a.settings").click(function() {
					//$(".maskPopup").removeClass("hide");
				});

				$("#webrtcPopup .uiIconClose.pull-right").click(function() {
					$(".maskPopup").addClass("hide");
				});

				$("#check").click(function() {
					if ($("#check").is(":checked")) {
						$(".toggleInputs").removeClass("hideInputs");
					} else {
						$(".toggleInputs").addClass("hideInputs");
					}
				});
				/* TOOLTIP */
				$("[data-toggle='tooltip']").tooltip();
			});
		};

		// TODO add more logic and UI here
	}

	return new Admin();
})($, webConferencing);
