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
	//log("> Loading at " + location.origin + location.pathname);
	
	/** 
	 * Admin class.
	 */
	function Admin() {
		
		var self = this;
		var user;
		
		this.init = function(theUser, context) {
			user = theUser;
		};
		
		// TODO add more logic and UI here
	}
	
	return new Admin();
})($, webConferencing);
