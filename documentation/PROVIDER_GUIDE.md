eXo Web Conferencing: Connector Development Guide
=========================================

eXo Web Conferencing provides a service provider interface (SPI) for building Connectors for plugging third party video calling service providers inside the eXo Platform interface. A call connector it is a portal extensions installed in eXo Platform. Any connector consists of server code and client app with user interface parts for running actual calls. This guide contains quick tutorial with a code how to start creating a custom Connector and add a call button to eXo Platform using the SPI of Web Conferencing.

Getting started
===============

When running, the Web Conferencing adds a Call Button of each available connector to users, spaces and chat rooms in eXo Platform. But connector should provide an implementation of this Call Button, including its markup in HTML, CSS styles and action handlers. It's not required for connector to care how and where the button will appear - this will be done by the core functionality of Web Conferencing add-on. Instead connector handles user clicks for its button, may change its style to reflect the call state and finally provide a call interface to an user. It's depends on a call provider how a call will appear to an user: in a new window or embeded to the current page. 

Additionally the Web Conferencing core offers an API to help maintaining conversation state and exchange call information between all clients of call participants. This API may work as a scaffolding for a new connector implementation and it covers following things: 
* Context information (call provider, user status, current user, space, chat room) - required to find call type (group or one-on-one) and its participants, other contextual data
* Call settings and state storage in eXo Platform database - if call provider requires external maintenance of conversation state (started, stopped etc) and participants, it can be saved in the database and accessed by all parties of the call
* Publishing user and call updates to all paprties - can be useful for notifying incoming/started or stopped call, joined/leaved participant; it's also possible to exchange call data (can be connectivity and media settings, chatting or files exchange or any other information related to a call)
* Log runtime information to eXo Platform log - it's Javascript logger that prints trace/debug/info/warn/error messages to the browser console and optionally can swap these logs to the server log file
* Adding connector settings in Administrator menu - allows to add an optional button to Web Conferencing administrator page for invoking a connector settings form
* Utility methods such as opening a new window for a call, showing messages and notices on the Platform page, finding user IM account etc.

In this tutorial we assume that you've already created and configured a portal extension for your provider and starting to implement the logic. 
TBD Show config here also?

For more details refer to the architecture document where [connector development] described in advance.

Java API
========

TBD

```java
package org.exoplatform.webconferencing.myconnector;
....

/**
 * My Connector provider implementation.
 * 
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: MyConnectorProvider.java 00000 Mar 30, 2017 pnedonosko $
 */
public class MyConnectorProvider extends CallProvider {
  
  /**
   * IM info for user profile.
   */
  public class MyConnectorIMInfo extends IMInfo {
    ....
  }

  /**
   * Instantiates a new My Call provider.
   *
   * @param profileSettings the profile settings
   * @param params the params (from configuration.xml)
   * @throws ConfigurationException the configuration exception
   */
  public MyConnectorProvider(UserProfileSettingsService profileSettings, InitParams params) throws ConfigurationException {
    super(params);

    this.url = "/myconnector";
    // if need set this from external you can introduce a configuration parameter
    // this.url = url;

    // Read other required settings... 
    String apiClientId = this.config.get(CONFIG_CLIENTID);
    if (apiClientId == null || (apiClientId = apiClientId.trim()).length() == 0) {
      throw new ConfigurationException(CONFIG_CLIENTID + " required and should be non empty.");
    }
    this.apiClientId = apiClientId;
    ....
    
    // Initialize IM type in Social app 
    if (profileSettings != null) {
      // add plugin programmatically as it's an integral part of the provider
      profileSettings.addIMType(new IMType(TYPE, TITLE));
    }
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public IMInfo getIMInfo(String imId) {
    // Here you can also validate, extend or do any other IM id preparations
    return new MyConnectorIMInfo(imId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getType() {
    return TYPE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String[] getSupportedTypes() {
    return new String[] { getType() };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTitle() {
    return TITLE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getVersion() {
    return VERSION;
  }
}
```

And then need register this provider as a plugin of `WebConferencingService` as show above in Configuration section.

Javascript API
==============

To add a call button create `callButton` method in your provider module. This method should return [jQuery Promise](http://api.jquery.com/deferred.promise/) object which should be resolved with a JQuery element of a button container. When clicked, this button will open a call user interface.

```javascript
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
```












