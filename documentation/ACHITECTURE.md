eXo Web Conferencing: Architecture
====================================

eXo Web Conferencing provides a service provider interface (SPI) for building Connectors for plugging third party video calling service providers inside the eXo Platform interface. Connectors may be added programmatically in the form of eXo Platform extensions. Several  connectors may be packaged by a single provider add-on.
Connectors may only support a subset of the interface and keep working properly. For example a connectors may only support 1:1 calls, while another one would support also group calls and presence. This guide contains a step by step tutorial with a code provided to implement a provider of custom Connector (that leverages the SPI).

Getting started
===============

Web Conferencing add-on it is a [portal extension](https://www.exoplatform.com/docs/PLF43/PLFDevGuide.eXoAdd-ons.PortalExtension.Mechanism.html) that offers a core functionality and a SPI for call connectors integration in eXo Platform apps. A connectors provider itself it is an another portal extension that depends on the Web Conferencing. Both add-ons should be installed to Platform server for successful work. Each provider consists of _services JAR_ and _web application WAR_. Services JAR contains Java API implemenation of the SPI and related resources. Web app WAR contains configurations with an UI for supported connector types. 

Web Conferencing core adds a Call Button to users, spaces and chat rooms in eXo Platform. Call Button can be used respectively to place a 1:1 call or start or join a group call. As the core add-on offers a call button integration in to user interface of the platform, a connector implementation doesn't need to care about such aspects as finding a place in the UI, dealing with other buttons around it. Insetad a connector need provide a markup of the button that should be added with click handler for it.

Each provider presenting in the Platform can be enabled or disabled by administrator. If a connector needs additional global settings, then it is possible to provide Settings user interface which will appear for platform administrators.

If connector has a need of instant messenger (IM) account, which will be used to sign-in an user to external service of software, then the connector should register such IM type and optionally provide an UI for its settings per user and for the platform administrators where applicable. 

A connector should also implement incoming call logic if applicable. Register a listener for using user notification channel from the Web Conferencing core and provide an action when remote call started, a 1:1 or group one, in both cases need show a notification about incoming call and offer functionality of accepting or declining the call by an user.

If call needs exchange extra data, such as communication establishment or network settings of the peers, it's possible to use a call channel from the Web Conferencing core.

To bootstrat the development there is a [template provider](https://github.com/exo-addons/web-conferencing/tree/develop/template): it is a sample project with a structure that already follows the conventions and has SPI stubs implemented for a provider features. In the template code you may find use of all features that Web Conferencing core offers.

Architecture
============

Being a portal extension, the Web Conferencing provider it is a plugin of the add-on's core component `WebConferencingService`. Plugin should be configured in the connector extension and extend `CallProvider` class. It is a programmatic entry point for each provider. To make the provider to work need implement major parts of the Java API and provide required configuration.

Below a diagram of Web Conferencing architecture. TBD

![Web Conferencing architecture](https://raw.github.com/exo-addons/web-conferencing/tree/develop/architecture.png) 

Conventions
-----------

**Provider ID**

Connectors are pluggable and identified by provider ID. This ID used to look up a provider both in Java and Javascript APIs. Create provider ID using following rules:
* ANSI characters without spaces and punctuation - it should be a single word (e.g. skype or my\_call),
* lowercase - don't use character in upper case.

**Provider title**

Provider needs a human readable name which will distunguish it among others. Often it based on a service or software name providing video calls support.

Web Conferencing Service
-------------------

Component of `WebConferencingService` available in eXo container and it provides top-level methods to create, start, stop, join, leave and remove calls. It also saves group calls linked to spaces and chat rooms. This component also used to register new providers (as plugins via configuration or in runtime). 

Connectors Provider
---------------

A provider should extend `CallProvider` class and implements its abstract methods. Here should be initiated other works related to the provider, like initialize from configuration or saved settings, save specific settings and do other preprartions before starting to server users.

Connector SPI
-------------

Connector SPI consists of following parts:
* Java API (provider class implementation and related server code)
* Javascript API (provider module)

A connector or several, combined as Web Conferencing provider, should be packaged as a portal extension with its configuration in it. Below you can find details about both APIs.

Provider extension
-------------------

Web Conferencing provider should be a portal extension that simply can be installed and uninstalled. 

All required configurations and resources should be packaged in the provider extension files. Provider dependencies should be outside the WAR file and deployed directly to _libraries_ folder of the Platform server (this already will be done by installation via eXo Add-ons Manager or extension installer script).

Configuration
-------------

Provider extension should depend on Web Conferencing extension in its `PortalContainerConfig` settings. The web app WAR needs following configuration in `META-INF/exo-conf/configuration.xml` file: use `PortalContainerDefinitionChange$AddDependenciesAfter` type to add itself as a dependency to the Web Conferencing (MyConnector from the template project here):

```xml
<configuration xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.exoplaform.org/xml/ns/kernel_1_0.xsd http://www.exoplaform.org/xml/ns/kernel_1_0.xsd"
  xmlns="http://www.exoplaform.org/xml/ns/kernel_1_0.xsd">

  <!-- My Connector portal extension configuration -->
  <external-component-plugins>
    <target-component>org.exoplatform.container.definition.PortalContainerConfig</target-component>
    <component-plugin>
      <name>Change PortalContainer Definitions</name>
      <set-method>registerChangePlugin</set-method>
      <type>org.exoplatform.container.definition.PortalContainerDefinitionChangePlugin</type>
      <init-params>
        <value-param>
          <name>apply.default</name>
          <value>true</value>
        </value-param>
        <object-param>
          <name>change</name>
          <object type="org.exoplatform.container.definition.PortalContainerDefinitionChange$AddDependenciesAfter">
            <field name="dependencies">
              <collection type="java.util.ArrayList">
                <value>
                  <string>myconnector</string>
                </value>
              </collection>
            </field>
            <field name="target">
              <string>webconferencing</string>
            </field>
          </object>
        </object-param>
      </init-params>
    </component-plugin>
  </external-component-plugins>
</configuration>
```

The web app WAR configuration contains:
* web app descriptor (web.xml) with proper `display-name` (as WAR name) and other [portal settings](http://docs.exoplatform.com/PLF40/PLFDevGuide.eXoPlatformExtensions.CreatingExtensionProject.html) (follow the template extension),
* components configuration in `WEB-INF/conf/myconnector/configuration.xml`, what includes connector as plugin of `WebConferencingService` and other required container components and plugins.

If the configuration requires in-place values (such as host name, authentication keys etc), these values can be [variablized](http://docs.exoplatform.com/PLF40/Kernel.ContainerConfiguration.VariableSyntaxes.html) in XML configuration of the connector and then provided in _configuration.properties_ or/and via JVM parameters of the Platform server.

Provider plugin for `WebConferencingService` configuration defines connector specific settings:

```xml
  <!-- Portal extension configuration for YOUR PROVIDER NAME -->
  <external-component-plugins>
    <target-component>org.exoplatform.webconferencing.WebConferencingService</target-component>
    <component-plugin>
      <name>add.callprovider</name>
      <set-method>addPlugin</set-method>
      <type>org.exoplatform.webconferencing.myconnector.MyConnectorProvider</type>
      <description>Call provider description here.</description>
      <init-params>
        <properties-param>
          <name>provider-configuration</name>
          <property name="my-apiKey" value="${webconferencing.myconnector.apiKey:myApiKey}" />
          <property name="my-clientId" value="${webconferencing.myconnector.clientId:myClientId}" />
          <property name="active" value="${webconferencing.myconnector.active:true}" />
        </properties-param>
      </init-params>
    </component-plugin>
  </external-component-plugins>
```

For the above configuration we may set following settings in _configuration.properties_:

```
######### My Connector ###########
webconferencing.myconnector.apiKey=myApiKey
webconferencing.myconnector.clientId=myClientId
webconferencing.myconnector.serviceUrl=https://mycall.acme.com/myconnector
```

Java API
========

Java API is mandatory part of any connector. By implementing Java interfaces and extending basic abstract classes your create a new connector and plug it to the Web Conferencing. 

Template project contains `MyConnectorProvider` class which shows how to implement a sample "mycall" provider. This class goal is to load configuration, provide provider type name and title with all supported types, register IM type in Social's user profile. 

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

Web Conferencing core has Javascript module for integration in user interface of eXo Platform applications: add a Call Button to users, spaces and chat rooms. A connector's module should depend on the core and register the provider in it. The dependency can be defined via portal `gatein-resources.xml`:

```xml
  <module>
    <name>webConferencing_myconnector</name>
    <load-group>webConferencingMyGRP</load-group>
    <script>
      <path>/js/webconferencing-myconnector.js</path>
    </script>
    <!-- My Call depends on jQuery and webConferencing modules -->
    <depends>
      <module>jquery</module>
    </depends>
    <depends>
      <module>webConferencing</module>
    </depends>
  </module>
```

Most of methods return [jQuery Promise](http://api.jquery.com/deferred.promise/) object which can be used for callbacks registration for waiting an asynchronous operation. 

Provider module *should* implement set of SPI methods:
 * getType() - required, a major call type name
 * getSupportedTypes() - required, all supported call types
 * getTitle() - required, human-readable title for UI
 * callButton(context) - required, provider should offer an implementation of a Call button and call invoker in it, it returns a promise, it should be resolved with a JQuery element of a button(s) container. 
 
 A provider *may* support following of API methods:
* init() - will be called when web conferencing user will be initialized, this method returns a promise, when resolved it means provider successfully initialized and can be used by Web Conferencing core.

It is an example how a connector module code could look (simplified Template connector for My Call sample):
```javascript
/**
 * My Connector provider module for Web Conferencing. This script will be used to add a provider to Web Conferencing module and then
 * handle calls for portal user/groups.
 */
(function($, webConferencing) {
  /** 
   * An object that implements Web Conferencing SPI contract for a call provider.
   */
  function MyProvider() {

    /**
     * MUST return a call type name. If several types supported, this one is assumed as major one
     * and it will be used for referring this connector in getProvider() and similar methods. 
     * This type also should listed in getSupportedTypes(). 
     * Call type is the same as used in user profile as IM type.
     */
    this.getType = function() {
      return settings.type;
    };

    /**
     * MUST return all call types supported by a connector.
     */
    this.getSupportedTypes = function() {
      return settings.supportedTypes;
    };

    /**
     * MUST return human-readable title of a connector.
     */
    this.getTitle = function() {
      return settings.title;
    };

    /**
     * MUST be implemented by a connector provider to build a Call button and call invoked by it. 
     * Web Conferencing core provides a context object where following information can be found:
     * - currentUser - username of an user that will run the call
     * - userId - if found, it's 1:1 call context, it's an username of another participant for the call
     * - spaceId - if found, it's space call, it contains a space's pretty name
     * - roomId - if found, it's eXo Chat room call, it contains a room (target) id (e.g. team-we3o23o12eldm)
     *   - roomTitle - if roomId found, then roomTitle will contain a human readable title
     *   - roomName - if roomId found, then roomName will contain a no-space name of the room for use with Chat APIs 
     *                or to build connector URLs where need refer a room by its name (in addition to the ID).
     *                NOTE: in case of space room, the name will contain the space's pretty name prefixed with 'space-' text.
     * - isGroup - if true, it's a group call, false then 1-one-1
     * - details - it's asynchronous function to call, it returns jQuery promise which when resolved (done) 
     *             will provide an object with call information. In general it is a serialized to JSON 
     *             Java class, extended from IdentityInfo - consult related classes for full set of available bean fields.
     *             
     * Thus method returns a jQuery promise. When it resolved (done) it should offer a jQuery element of a button(s) container.
     * When rejected (failed), need return an error description text (it may be shown directly to an user), the connector
     * will not be added to the call button and user will not see it.
     */
    this.callButton = function(context) {
      var button = $.Deferred();
      if (settings && context && context.currentUser) {
        // You may obtain the user IM Id via this method. Can be useful when connector supports several call types.
        // But it's optionally to get an IM account. If you connector doesn't have IM types in user profile - don't do
        // this.
        // IM object is a serialized to JSON Java class IMInfo. It has id and type fields. Where id is for an user IM
        // ID.
        var currentUserIMID = webConferencing.imAccount(context.currentUser, "myconnector");
        // In the code below, it's assumed that My Connector has IM type 'myconnector' and calls only possible with
        // users having the same IM type in their profiles.
        if (currentUserIMID) {
          context.details().done(function(target) {
            var ims = [];
            // Collect all participants (ims) for a call
            // ....
            if (ims.length > 1) {
              // If we have more than single user, then we have participants for a call.
              // Build a call button: It can be an anchor or button. It may use any custom CSS class (like
              // myCallAction) we know that Web Conferencing may add btn class (from PLF's styles) if this connector will be a single
              // compatible for an user. You need provide an icon and title for the button.
              var $button = $("<a title='" + target.title + "' href='javascript:void(0)' class='myCallAction'>"
                    + "<i class='uiIconMyCall uiIconVideoPortlet uiIconLightGray'></i>"
                    + "<span class='callTitle'>My Call</span></a>");
              // Add click handler to the button and add logic to open a link of call UI
              $button.click(function() {
                // When user clicked the button - create an actual call.
                // You can save this call in eXo to inform other parts and be able restore the call in case of page
                // reload or on other Platform pages. Respectively, you'll need to delete the call - this could be done from a
                // call page, but also may be done from server-side (on some event, external call, timer, etc.).
                // Find a way informing end of the call from your actual connector capabilities.
                //
                // Adding (and then removing) a call is not mandatory. If your call provider inform other parts
                // about the call by itself (e.g. via native app), you can skip adding/removing calls.
                //
                // To save a new call we need an ID with some info about an owner, its type, provider, title and participants.
                // Call ID should be generated by a connector, there is no restrictions for how ID should look,
                // but it's recommended to keep it without spaces and friendly to URLs.
                // Below we construct an ID to simply identify call on both portal and chat pages:
                // * for group call we prefix with (g/) with group ID (effectively:
                // - for spaces we want space_name (known as pretty name, e.g. product_team) and
                // if it's space room in chat we use roomName from the context
                // - for chat room its room-name (e.g. space-121218554... or team-8978676565...)
                // * for 1:1 prefix (p/) appended with participant IDs sorted always in same order.
                // XXX Call ID should only contain characters supported by CometD,
                // find more in https://docs.cometd.org/current/reference/#_bayeux_protocol_elements
                var callId;
                if (target.group) {
                  callId = "g/" + (target.type == "chat_room" ? context.roomName : target.id);
                } else {
                  // Sort IMs to have always the same ID for two parts independently on who started the call
                  var imsAsc = ims.slice();
                  imsAsc.sort();
                  callId = "p/" + imsAsc.join("-");
                }
                // Next we need ensure this call not yet already started (e.g. remotely),
                // it's actual especially for group calls where user can join already running conversations
                // As we have two cases: new call and joining a call, we use promise to complete the call page for
                // any of cases depending on asynchronous requests to the server.

                // Try get a call by the ID to know is it exists already - it why we need stable ID clearly defining the target
                webConferencing.getCall(callId).done(function(call) {
                  // Call already running - join it
                  log.info("Joining call: " + callId);
                  webConferencing.updateUserCall(callId, "joined").done(function() {
                    // Show call UI to an user
                    // ...
                  }).fail(function(err) {
                    log.error("Failed to join call: " + callId, err);
                    webConferencing.showError("Joining call error", webConferencing.errorText(err));
                  });
                }).fail(function(err) {
                  if (err) {
                    if (err.code == "NOT_FOUND_ERROR") {
                      // OK, this call not found - start a new one,
                      var callInfo = {
                        // for group calls an owner is a group entity (space or room), otherwise it's 1:1 and who
                        // started is an owner
                        owner : target.group ? target.id : context.currentUser.id,
                        // ownerType can be 'user' for 1:1 calls, 'space' for group call in space, 'chat_room' for group call in Chat room
                        ownerType : target.type, // use target type
                        provider : self.getType(),
                        // tagret's title is a group or user full name
                        title : target.title,
                        // In general, not all group members can be participants, see above ims variable
                        participants : ims.join(";") // string build from array separated by ';'
                      };
                      webConferencing.addCall(callId, callInfo).done(function(call) {
                        log.info("Call created: " + callId);
                        // Show call UI to an user
                        // ...
                      });
                    } else {
                      log.error("Failed to get call info: " + callId, err);
                      webConferencing.showError("Joining call error", webConferencing.errorText(err));
                    }
                  } else {
                    log.error("Failed to get call info: " + callId);
                    webConferencing.showError("Joining call error", "Error read call information from the server");
                  }
                });
              });
              // Resolve with our button
              button.resolve($button);
            } else {
              // If not users compatible with My Connector IM type found, we reject, thus don't show the button for this context
              var msg = "No " + self.getTitle() + " users found for " + target.id;
              log.warn(msg);
              button.reject(msg);
            }
          }).fail(function(err) {
            // On error, we don't show the button for this context
            var msg = "Error getting context details";
            log.error(msg, err);
            button.reject(msg, err);
          });
        } else {
          // If current user has no My Connector IM - we don't show the button to him
          var msg = "Not My Connector user " + context.currentUser.id;
          log.debug(msg);
          button.reject(msg);
        }
      } else {
        // If not initialized, we don't show the button for this context
        var msg = "Not configured or empty context";
        log.error(msg);
        button.reject(msg);
      }
      // Return a promise, when resolved it will be used by Web Conferencing core to add a button to a required places
      return button.promise();
    };

    /**
     * OPTIONAL method. If implemented, it will be called by Web Conferencing core on addProvider() method. It is assumed that the connector
     * will initialize internals depending on the given context. 
     */
    this.init = function(context) {
      var process = $.Deferred();
      if (eXo && eXo.env && eXo.env.portal) {
        // We want initialize call buttons and incoming calls dialog only for portal pages (including Chat)
        var currentUserId = webConferencing.getUser().id;
        // ....
        // Subscribe to user updates (incoming calls will be notified here)
        webConferencing.onUserUpdate(currentUserId, function(update) {
          // Handle the update data: find is it an incoming or a join of some user to already running call
          // This connector cares only about own provider events
          if (update.providerType == self.getType()) {
            if (update.eventType == "call_state") {
              var callId = update.callId;
              // A call state changed (can be 'started', 'stopped', 'paused' (not used for the moment)
              if (update.callState == "started") {
                // When call started it means we have an incoming call for this particular user
                log.info("Incoming call: " + callId);
                // Get call details by ID
                webConferencing.getCall(callId).done(function(call) {
                  var callerId = call.owner.id;
                  var callerLink = call.owner.profileLink;
                  var callerAvatar = call.owner.avatarLink;
                  var callerMessage = call.owner.title + " is calling you...";
                  var callerRoom = callerId;
                  call.title = call.owner.title; // for callee the call title is a caller name
                  var isGroup = callId.startsWith("g/"); // using logic from callButton() above
                  // Get current user status, we need this to figure out a need of playing ringtone
                  // we'll do for users with status 'Available' or 'Away', but ones with 'Do Not Disturb' will not hear an incoming ring.
                  webConferencing.getUserStatus(currentUserId).done(function(user) {
                    // Build own UI to ask user for incoming call and show it to the user:
                    var accepted = true;
                    if (accepted) {
                      // When user accept the call, need call:
                      webConferencing.updateUserCall(callId, "joined").fail(function(err) {
                        log.error("Failed to join call: " + callId, err);
                      });
                    } else {
                      if (isGroup) {
                        // When user decline the call, for group call need update the call with 'leaved' state:
                        webConferencing.updateUserCall(callId, "leaved").fail(function(err) {
                          log.error("Failed to leave call: " + callId, err);
                        });
                      } else {
                        // For 1:1 we delete the call:
                        webConferencing.deleteCall(callId).done(function() {
                          log.info("Deleted call: " + callId);
                        }).fail(function(err) {
                          if (err && err.code == "NOT_FOUND_ERROR") {
                            log.debug("Call not found: " + callId); // already deleted
                          } else {
                            log.error("Failed to delete call: " + callId, err);
                          }
                        });
                      }
                    }
                  }).fail(function(err) {
                    log.error("Failed to get user status: " + currentUserId, err);
                    if (err) {
                      webConferencing.showError("Incoming call error", webConferencing.errorText(err));
                    } else {
                      webConferencing.showError("Incoming call error",
                            "Error read user status information from the server");
                    }
                  });
                }).fail(function(err) {
                  log.error("Failed to get call info: " + callId, err);
                  if (err) {
                    webConferencing.showError("Incoming call error", webConferencing.errorText(err));
                  } else {
                    webConferencing.showError("Incoming call error", "Error read call information from the server");
                  }
                });
              } else if (update.callState == "stopped") {
                log.info("Call stopped remotelly: " + callId);
              }
            } else if (update.eventType == "call_joined") {
              log.debug("User call joined: " + update.callId);
            } else if (update.eventType == "call_leaved") {
              log.debug("User call leaved: " + update.callId);
            }
          }
        }, function(err) {
          log.error("Failed to listen on user updates", err);
        });
      }
      process.resolve();
      return process.promise();
    };
  }

  var provider = new MyProvider();

  return provider;
})($, webConferencing);
```

A [_template_ connector](https://github.com/exo-addons/cloud-drive-extension/tree/master/connectors/template) project already contains Maven modules with Web Conferencing dependencies and packaging. Copy template sub-folder to your new location and change its name to your connector name in source files and configuration, rename respectively package, class names and variables. Fill the sources with a logic to work with your connector services. Add required third-party libraries to the Maven dependencies and assembly of the packaging. Then build the connector and use its packaging artifact as a connector extension.


Remote logger
===========

TBD











