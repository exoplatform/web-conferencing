this.callButton = function(context) {
  var button = $.Deferred();
  context.details().done(function(target) {
    var $button = $("<a title='" + target.title + "' href='javascript:void(0)' class='myCallAction'>"
          + "<i class='uiIconMyCall uiIconVideoPortlet uiIconLightGray'></i>"
          + "<span class='callTitle'>My Call</span></a>");
    // Add click handler to the button and add logic to open a link of call UI
    $button.click(function() {
      // When user click the button - create an actual call by ID you know of just built.
      var callId = "my_call_2we34aldfg9876cdasqwdd";
      // Ensure this call not yet already started (e.g. by another party)
      webConferencing.getCall(callId).done(function(call) {
        // Call already running - we join it
        webConferencing.updateUserCall(callId, "joined").done(function() {
          // TODO Show call UI to an user
        }).fail(function(err) {
          webConferencing.showError("Joining call error", webConferencing.errorText(err));
        });
      }).fail(function(err) {
        if (err && err.code == "NOT_FOUND_ERROR") {
          // this call not found - start a new one,
          var callInfo = {
            owner : target.group ? target.id : context.currentUser.id,
            ownerType : target.type,
            provider : self.getType(),
            title : target.title,
            participants : "john;marry" // users separated by ';'
          };
          webConferencing.addCall(callId, callInfo).done(function(call) {
            log.info("Call created: " + callId);
            // TODO Show call UI to an user
          });
        } else {
          webConferencing.showError("Joining call error", webConferencing.errorText(err));
        }
      });
    });
    // Resolve with our button
    button.resolve($button);
  }).fail(function(err) {
    // On error, we don't show the button
    button.reject("Error getting context details", err);
  });
  // Return a promise, when resolved it will be used by Web Conferencing core to add a button to a required places
  return button.promise();
};