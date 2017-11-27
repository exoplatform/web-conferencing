/**
 * WebConferencing Admin portlet in eXo Platform.
 */
(function($, webConferencing) {
	"use strict";

	/** For debug logging. */
	var objId = Math.floor((Math.random() * 1000) + 1);
	var logPrefix = "[webconferencing_admin_" + objId + "] ";
	var log = function(msg, e, logPrefix) {
		webConferencing.log(msg, e, logPrefix);
	};
	// log("> Loading at " + location.origin + location.pathname);

	/**
	 * Admin class.
	 */
	function Admin() {

		var self = this;
		var user;

		this.init = function(theUser, context) {
			if (theUser) {
				user = theUser;
				log("Initialized for user " + theUser.id);
			}

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

			$(function() {
				// TODO it's WebRTC provider related stuff, move to the connector code
				/* OPEN HIDE POPUPs */
				$("#webConfPopupClique").click(function() {
					$(".maskPopup").removeClass("hide");
				});

				$("#webConfPopup .uiIconClose.pull-right").click(function() {
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
