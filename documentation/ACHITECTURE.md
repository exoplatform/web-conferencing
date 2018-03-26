eXo Web Conferencing: Architecture
====================================

eXo Web Conferencing provides a service provider interface (SPI) for building connectors for plugging third party video calling service providers inside the eXo Platform interface. Connectors may be added programmatically in the form of eXo Platform extensions. Several providers may be packaged into a single connector extension.
Connectors may only support a subset of the interface and keep working properly. For example a connector may only support 1:1 calls, while another one would support also group calls and presence. This guide contains description of Web Conferencing architectire and a step by step tutorial with a code samples for a custom Connector (that leverages the SPI).

Introduction
===============

Web Conferencing consists of two main parts: 
* a core that add call buttons in Platform pages, offers common API and registration of call connectors build using Connector SPI
* set of call Connectors, each of them is implementing the SPI and handles actual calls (including UI, user actions, presense etc)

Thechnically, the Web Conferencing add-on it is a [portal extension](https://docs.exoplatform.org/PLF50/PLFDevGuide.eXoAdd-ons.PortalExtension.html) that offers a core functionality and a SPI for call connectors integration in eXo Platform apps. A call connector itself, it is an another portal extension that depends on the Web Conferencing. Both extensions should be installed to a Platform server for successful work. 

Web Conferencing Core
-----------

Web Conferencing core adds a Call Button to users, spaces and chat rooms in eXo Platform. Call Button can be used respectively to place a 1:1 call or start or join a group call. As the core offers a call button integration to user interface of the platform, a connector implementation doesn't need to care about such aspects as finding a place in the UI, dealing with other buttons alignement. Instead, a connector provides a markup of its call button and action handlers for it. A contract between Web Conferencing core and connector prescribed in Connector SPI which defines integration points and a way of connector registration in the system.

Additionally core offers an API to help build a call conversation, maintain its state and exchange information between all participants. This API may work as a scaffolding for a new connector implementation and it covers following functionality: 
* Context information (call provider, user status, current user, space, chat room) - required to find call type (group or one-on-one) and its participants, other contextual data
* Call settings and state storage in eXo Platform database - if call requires external maintenance of conversation state (started, stopped etc) and participants, it can be saved in the database and accessed by all parties of the call
* Publishing user and call updates to all paprties - can be useful for notifying incoming/started or stopped call, joined/leaved participant; it's also possible to exchange call data (can be connectivity and media settings, chatting or files exchange or any other information related to a call)
* Log runtime information to eXo Platform log - it's Javascript logger that prints trace/debug/info/warn/error messages to the browser console and optionally can swap these logs to the server log file
* Adding connector settings in Administrator menu - allows to add an optional button to Web Conferencing administrator page for invoking a connector settings form
* Utility methods such as opening a new window for a call, showing messages and notices on the Platform page, finding user IM account etc.

Both Connector SPI and common API will be overviewed below in 'Creating a connector' section.

Call Connectors
-----------

Connector SPI requires a connector to register itself as a plugin(s) in Web Confrencing component in eXo container. This plugin implements server-side logic of call provider activation and configuration. Next, a provider needs to be loaded and initialized in the Platform UI - this can be done by a portlet that will run for each page and initialize the provider for an user. At this stage provider may load its resources, call external services or make user authorization to get prepared for future calls (incoming and outgoing).

Connector extension consists of _services JAR_ and _web application WAR_. Services JAR contains Java implemenation of the SPI and related resources for supported providers. Web app WAR contains configurations, portlets and servlets (if required) for UI with Javascript client module that implements the SPI for supported providers types. 

Where available, a call provider implements incoming call via its library or external services. But it's also possible to implement incoming logic using Web Conferencing core API. In this case need register a listener for user notification channel and provide an action when incoming call will start, show a notification and offer functionality of accepting or declining the call by an user.
If call needs exchange extra data, such as communication establishment or network settings of the peers, use a call channel from the Web Conferencing core API.
Each connector presented in the Platform can be enabled or disabled by an administrator. If a connector needs additional global settings, then it is possible to provide Settings user interface which will appear for platform administrators.
If connector has a need of instant messenger (IM) account, which will be used to sign-in an user to external service or software, then the connector can register such IM type and optionally provide an UI for its settings per user and for the platform administrators where applicable. 

To bootstrat the development there is a [template connector](/template): it is a sample project with a structure that already follows the conventions and has SPI stubs implemented for a provider features. In the template code you may find use of many features that Web Conferencing core API offers.

Architecture
============

Server-side part of Web Conferencing consists of a eXo container component `WebConferencingService`, it handles core Java API functionality and register providers via plugins. External clients (web, mobile etc.) can access the core via REST services covered by `RESTWebConferencingService` and CometD channels/calls covered by `CometdWebConferencingService`. REST services cover mainly administrative functions and CometD channels work for user/call notifications and data exchange. Call providers can deploy own services for specific needs. Another server-side part of Web Conferencing core it's Remote Logger component `CallLogService` - it is exposed via CometD channel and allows print log messages to the server log for errors diagnostic and gathering statistics from clients (web and mobile).

Web Conferencing at client-side consists of core Javascript module `webConferencing` that should be used for call providers registration in Platform UI. The core module also exposes an API for gettings contextual information (current user, space, chat room) and calling REST and CometD services of Web Conferencing. This way a connector developer doesn't need directly access the web services of the core, but use Javascript methods instead and concentrate on its provider logic.

Below a diagram of Web Conferencing architecture. TBD

![Web Conferencing architecture](/documentation/images/architecture.png) 

Conventions
-----------

**Web Conferencing core**

Web Conferencing core it is a registry of all call connectors in the system. It offers a common API for maintaining call state, settings and links with a space/room in Platform database; it also has methods for listening on calls and getting users, spaces and chat room entities suitable for use with calls. Core also has REST and CometD services for accessing its data from external clients. There is a remote logger CometD channel for spooling client logs to the server log file.

**Connector SPI**

A service provider interface for implementing call connectors to plug into Web Conferencing core. This SPI consists of provider configuration for registration and Java and Javascript implementaions of provider logic. As connector may offer a support for several call providers (usually similar or of several calling methods: e.g. VoiP and landline but by single service), the SPI should be implemented by each provider of the connector. Then these providers will be managed separatelly and Platform administrator will be able activate them independently.

**Call connector**

Connector it is a portal extension, it's a package of single or several call providers with its configuration and resources (specific server services, storages, state support, UI and internationalization messages). Portal extension mechanism allows simply install and uninstall the connector in Platform server. Connector extension should be installed together with Web Conferencing add-on. Connector dependencies (Java libraries) should be outside the extension WAR file and be deployed directly to _libraries_ folder of the Platform server (this will be done during installation by eXo Add-ons Manager or extension installer script).

**Call provider**

A call provider should conform the Connector SPI to be registered, loaded and successfully initialized within Web Conferencing features in eXo Platform. Each provider should extend `CallProvider` class and implements its abstract methods. Here can be initiated other works related to the provider, like initialize from configuration or saved settings, preconfigure specific settings and do other preparations before starting serve users.

**Provider ID**

Call providers are identified by an ID. It is a _string_ value. This ID used to look up a provider both in Java and Javascript APIs. Create provider ID using following rules:
* ANSI characters without spaces and punctuation - it should be a single word (e.g. _skype_ or _my\_call_),
* lowercase - don't use character in upper case.

**Provider title**

Provider needs a human readable name which will well distunguish it among others. It will appear in administrative screen and may be used for regular users. Often it based on a service or software name providing video calls support.

**Provider configuration**

A call provider may need an extra settings that will let users connect a right server or local app. While it's optional, each provider that needs such settings should add them in the connector extension and then the provider will load them in runtime and use for a work. Component plugin configiration should be done in XML by following eXo Container [configuration](https://docs.exoplatform.org/public/topic/PLF50/PLFAdminGuide.Configuration.html) scheme.

Creating a connector
========

Web Conferencing designed so, to allow maximum freedom to its connectors for implementing UI and logic parts. As shown above the core of Web Conferencing only covers placing call buttons of its connectors in eXo Platform interface and also provides an API for common operations that may be needed for making calls. Such aspects as UI markup with styles and logic that handles them are fully managed by a connector and call providers that it offer. While provider can be registred in the system via declarative XML configuration, in runtime it relies on object instances offered by the connector to the core.

A call connector, being a portal extension, registers a plugin of the core component `WebConferencingService`. Provider should be configured in the connector extension and extend `CallProvider` class. It is a programmatic entry point on the server-side for each provider. To make the provider work need implement required parts of the Connector SPI (Java and Javascript parts) and provide a configuration to load it in running Platform server.

A [template connector](/template) project already contains Maven modules with Web Conferencing dependencies and packaging. Copy template sub-folder to your new location and change its name to your connector name in source files and configuration, rename respectively package, class names and variables in the code. Complete the sources with a logic to work with your connector services. Add required third-party libraries to the Maven dependencies and assembly of the packaging. Then build the connector and use its packaging artifact as a connector extension.

Extension configuration
-------------

Connector extension should depend on Web Conferencing extension in its `PortalContainerConfig` settings. The web app WAR needs following configuration in `META-INF/exo-conf/configuration.xml` file: use `PortalContainerDefinitionChange$AddDependenciesAfter` type to add the extension itself as a dependency to the Web Conferencing (_MyConnector_ from the template project here):

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
* web app descriptor (web.xml) with proper `display-name` (as WAR name) and other [portal settings](https://docs.exoplatform.org/PLF50/PLFDevGuide.eXoAdd-ons.PortalExtension.Howto.html) (follow the template extension),
* components configuration in `WEB-INF/conf/myconnector/configuration.xml`, what includes connector as plugin of `WebConferencingService` and other required container components and plugins.

If the configuration requires external values (such as host name, authentication keys etc), these values can be [variablized](https://docs.exoplatform.org/PLF50/Kernel.ContainerConfiguration.VariableSyntaxes.html) in XML configuration of the connector and then provided in _configuration.properties_ or/and via JVM parameters of the Platform server.

Provider configuration
----------

Provider plugin for `WebConferencingService` configuration defines its specific settings (settings below only for an example, your connector will have own ones):

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

The provider portlet (it will load provider into Platform UI) should present on each portal page, thus we load it to the Platform toolbar using `AddOnService` component:
```xml
  <!-- Add My Connector portlet to portal pages with a toolbar -->
  <external-component-plugins>
    <target-component>org.exoplatform.commons.addons.AddOnService</target-component>
    <component-plugin>
      <name>addPlugin</name>
      <set-method>addPlugin</set-method>
      <type>org.exoplatform.commons.addons.AddOnPluginImpl</type>
      <description>add application Config</description>
      <init-params>
        <value-param>
          <name>priority</name>
          <value>10</value>
        </value-param>
        <value-param>
          <name>containerName</name>
          <value>middle-topNavigation-container</value>
        </value-param>
        <object-param>
          <name>MyConnectorPortlet</name>
          <description>My Connector portlet</description>
          <object type="org.exoplatform.portal.config.serialize.PortletApplication">
            <field name="state">
              <object type="org.exoplatform.portal.config.model.TransientApplicationState">
                <field name="contentId">
                  <string>myconnector/MyConnectorPortlet</string>
                </field>
              </object>
            </field>
          </object>
        </object-param>
      </init-params>
    </component-plugin>
  </external-component-plugins>
```

Note, that here we assume that `MyConnectorPortlet` properly configured as a [portelt for eXo Platiorm](https://docs.exoplatform.org/PLF50/PLFDevGuide.DevelopingApplications.DevelopingPortlet.html). 

Implementing Java SPI
-----------

Java SPI is a mandatory part of any connector provider. By extending `CallProvider` abstratc class and implementing required method your create a new provider and then plug it to the Web Conferencing via configiration as described above. 

Template project contains `MyConnectorProvider` class which shows how to implement a sample "mycall" provider. This class goal is to load configuration, define provider type name and title with all supported types, register IM type in Social's user profile. 

```java
package org.exoplatform.webconferencing.myconnector;
....

/**
 * My Connector provider implementation.
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
  
  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isLogEnabled() {
    // TODO maintain remote logger setting and return it here to let Web Conferencing core to know 
    // and spool client logs to the server logger (see 'Remote logger' section below)
    return myConfiguration.isLogEnabled();
  }
}
```

At a next step, create a portlet that will load and initialize the provider in Platform UI.

```java
  /**
   * My Connector provider portlet should be added to the portal pages (configuration.xml) where we need add the
   * provider to Web Conferencing call buttons.
   * This portlet loads Javascript module of this connector and register its provider(s) in the Web
   * Conferencing core. By doing this we add the connector to call buttons on the page. And this connector
   * script should implement call button element and logic on clicking it.
   */
  public class MyConnectorPortlet extends GenericPortlet {
    /**
     * {@inheritDoc}
     */
    @Override
    public void init() throws PortletException {
      // Get eXo container and Web Conferencing service once per portlet initialization
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      this.webConferencing = container.getComponentInstanceOfType(WebConferencingService.class);
      try {
        this.provider = (MyConnectorProvider) webConferencing.getProvider(MyConnectorProvider.TYPE);
      } catch (ClassCastException e) {
        LOG.error("Provider " + MyConnectorProvider.TYPE + " isn't an instance of " + MyConnectorProvider.class.getName(), e);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doView(final RenderRequest request, final RenderResponse response) throws PortletException, IOException {
      if (this.provider != null) {
        try {
          // If we have settings to send to a client side
          String settingsJson = asJSON(provider.getSettings());
          JavascriptManager js = ((WebuiRequestContext) WebuiRequestContext.getCurrentInstance()).getJavascriptManager();
          // first load Web Conferencing itself,
          js.require("SHARED/webConferencing", "webConferencing")
            // load our connector module to myProvider variable
            .require("SHARED/webConferencing_myconnector", "myProvider")
            // check if the variable contains an object to ensure the provider was loaded successfully
            .addScripts("if (myProvider) { "
                // optionally configure the provider with settings (from the server-side)
                + "myProvider.configure(" + settingsJson + "); "
                // then add an instance of the provider to the Web Conferencing client
                + "webConferencing.addProvider(myProvider); "
                // and force Web Conferencing client update (to update call buttons and related stuff)
                + "webConferencing.update(); " + "}");
        } catch (Exception e) {
          LOG.error("Error processing My Connector calls portlet for user " + request.getRemoteUser(), e);
        }
      }
    }
  }
```

Finally configure call provider and its portlet as described in 'Provider configuration' section. The provider class as a plugin of `WebConferencingService` and portlet via plugin of `AddOnService` (also need configure it in `WEB-INF/portlet.xml` of the extension WAR).

If you build a call page outside Platform portal, then you will need to create a servlet/JSP, or use similar technology, for your page and initialize core `webConferencing` Javascript module (see in Common API below), configure your connector module and register your provider in the core as it is shown in the portlet code above. In a [template connector](/template) you can find call _servlet_ and _call.jsp_ where this done.

Implementing Javascript SPI
--------------------

Web Conferencing core has Javascript module `webConferencing`, it is required for integration in user interface of eXo Platform. A connector's module should depend on the core and register the provider in it. The dependency can be defined via connector extension's `gatein-resources.xml`:

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

In Web Conferencing API most of methods return a [promise](http://api.jquery.com/deferred.promise/) object which can be used for callbacks registration for waiting an asynchronous operation completion. 

Provider object *must* implement following SPI methods:
 * getType() - required, a major call type name
 * getSupportedTypes() - required, all supported call types (at least a major type here)
 * getTitle() - required, human-readable title for UI
 * callButton(context) - required, provider should offer an implementation of a Call button and call invoker in it, it returns a promise, it should be resolved with a JQuery element of a button container. 
 
 A provider *may* support following SPI methods:
* init() - if available, it will be called when eXo user will be initialized, this method returns a promise, when resolved it means provider successfully initialized and can be used by Web Conferencing core. It's optional method, but very handy for doing provider initialization after the core will be loaded.
* showSettings() - if available, it will cause showing a settings button for this provider in Web Conferencing Administration page and when button clicked this method will be invoked to show an UI with settings. It's up to the provider how its settings will be read, filled and saved.  

It is an example how a connector module code could look (simplified Template connector for My Call sample), follow comments in the code to see the logic of implementing Connector SPI:

```javascript
/**
 * My Connector provider module for Web Conferencing. This script will be used to add a provider to Web Conferencing module and then
 * handle calls for portal's user/groups.
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
     * MUST be implemented by a connector provider to build a Call button and handle clicks on it. 
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
     * This method returns a jQuery promise. When it resolved (done) it should offer a jQuery element of a button(s) container.
     * When rejected (failed), need return an error description text (it may be shown directly to an user), the connector
     * will not be added to the call button and user will not see it.
     */
    this.callButton = function(context) {
      var button = $.Deferred();
      if (settings && context && context.currentUser) {
        // You may obtain the user IM Id via this method. Can be useful when connector supports several call types.
        // But it's optionally to get an IM account. If you connector doesn't have IM types in user profile - don't do this.
        // IM object is a serialized to JSON Java class IMInfo. It has id and type fields. Where id is for an user IM ID.
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
              // Resolve with our button - return jQuery object here, so it will be appended to Call Button UI in the Platform
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
    
    /**
     * OPTIONAL method. If implemented, it will cause showing a settings button in Web Conferencing Administration page 
     * and when button clicked this method will be invoked. In this method you can show a popup to an admin user 
     * with provider specific settings.
     */
    this.showSettings = function() {
			// load HTML with settings
			var $popup = $("#myconnector-settings-popup");
			if ($popup.length == 0) {
				$popup = $("<div class='uiPopupWrapper' id='myconnector-settings-popup' style='display: none;'><div>");
				$(document.body).append($popup);
			}
			$popup.load("/myconnector/settings", function(content, textStatus) {
				if (textStatus == "success" || textStatus == "notmodified") {
				  var $settings = $popup.find(".settingsForm");
          // fill settings form and handle its changes to update the settings on the server (e.g. by using your provider REST service)
          //.....
          // Show the settings popup when ready
          $popup.show();
        } // otherwise it's error
      });
    };
  }

  var provider = new MyProvider();

  return provider;
})($, webConferencing);
```

Implementing CSS
--------------------

For portal pages you will use connector's extension `gatein-resources.xml` to declare CSS for the connector UI. You may need to care about `Default` and `Enterprise` skins if your UI needs differences. Add to this CSS only styles for call button and other UI parts added by the connector. It's strongly recommened to follow [UX guidelines](http://exoplatform.github.io/ux-guidelines/) for eXo Platform to build universal UIs that will work for all skins. 

```xml
  <!-- CSS for MyConnector Calls support in Platform -->
  <portlet-skin>
    <application-name>myconnector</application-name>
    <portlet-name>MyConnectorPortlet</portlet-name>
    <skin-name>Default</skin-name>
    <css-path>/skin/myconnector-default.css</css-path>
    <overwrite>true</overwrite>
    <css-priority>1</css-priority>
  </portlet-skin>
  <portlet-skin>
    <application-name>myconnector</application-name>
    <portlet-name>MyConnectorPortlet</portlet-name>
    <skin-name>Enterprise</skin-name>
    <css-path>/skin/myconnector-enterprise.css</css-path>
    <overwrite>true</overwrite>
    <css-priority>1</css-priority>
  </portlet-skin>
```

Administration and Settings
===========

Each registered call provider will appear in Web Conferencing Administration page. Provider doesn't need implement anything to make it happen as administrative settings separated from a provider implementation. Administration page allows to activate and deactivate a provider. Activated status available in the provider class as method `isActive()`, an implementation may consult this method for its state in runtime. 

If call provider has settings that should be configured by a Platform administrator, then it may add them to the Administration page by implementing `showSettings()` method in its Javascript module as show above in 'Implementing Javascript SPI' section. If this method found, then a settings button will be show for this provider in Administration page and when the button clicked this method will be invoked. Provider is responsible for showing its settings UI, editing and persisting them.

![Administration page](/documentation/images/adminPage.png)

Common API
===========

Web Conferencing API designed to help build new connectors for making calls. Its Java part is mainly for registering a new connectors, via configuration or in runtime. Its Javascript part is a set of useful tools for buidling UI and handing call state.

Java API
-------------------

All classes availabe in `org.exoplatform.webconferencing` package are designed to be used by call providers. A main one, it's `WebConferencingService` a component registered in eXo container, a connector code can depend on it and use for building a custom solutions. But in most of cases it's not required. Even an advanced connector may work without directly using this service as most of its methods are exposed to Javascript SPI/API and can be used directly from client side. 

Below a brief overview of `WebConferencingService`:
* Maintaining calls in Platform database: 
  * `addCall()` - add a new call with ID, owner (user, space or chat room), title, provider type and collection of participants (eXo user ID or any string reference)
  * `getCall()` - get call by ID
  * `joinCall` - join existing call by ID for used denoted by ID and a client ID (of Web Conferencing Javascript module)
  * `leaveCall` - leave existing call by ID for used denoted by ID and a client ID (of Web Conferencing Javascript module)
  * `startCall` - start existing call (usually it's group calls linked to space or chat room) that being stopped, requires call and client ID
  * `stopCall` - stop existing call by its ID and optionaly can remove the call if boolean flag is `true`
* Info objects for making calls:  
  * `getUserInfo` - return `UserInfo` object for given user ID from organization service, this object describes an user for making calls
  * `getSpaceInfo` - return `SpaceInfo` object for given space pretty name, this object describes a space for group calls
  * `getRoomInfo` - return `RoomInfo` object for given room ID, title and members, this object describes a room for group calls
* Registration of call providers:
  * `addProvider` - register provider instance of `CallProvider`
  * `getProvider` - get provider by its type name
* Listen on call updates:
  * `addUserCallListener` - register a listener of `UserCallListener` type, this listener will be informed on call state changes (started, stopped) and on its parties joining or leaving.
  * `removeUserCallListener` - remove previously registred call listener

Further details of classes used by the service you may find in source code and Javadocs.

Javascript API
-------------------

Core Javascript module `webConferencing` contains set of helpful functions for buidling call providers. It includes methods for core initialization and accessing server-side storage to save and maintaining call state and exchange related information. There are also several utility methods for accessing context, showing messages to users and log technical information.

Methods of `webConferencing` module:

* Initialization and context methods (synchronous):
  * `init(user, context)` - core initialization used internally by `WebConferencingPortlet` when loading the module, it needs user object (`UserInfo` serialized to JSON) and context (`ContextInfo` in JSON). Usually a connector doesn't need invoke this method as it's already done by the core portlet. When this method invoked it will initialize all already registered providers. Others will be initialized when added
  * `update()` - update Platform UI with Web Conferencing features (call buttons etc). This method should be invoked after adding a call provider. But it will does nothing until the core initialzied (via init())
  * `getUser()` - returns currently initialized user (by init() method) or nothing if not initialized yet
  * `getCurrentSpaceId()` - returns current space pretty name if page shows a space
  * `getCurrentRoomTitle` - returns current chat room title if page runs in Chat app
  * `getBaseUrl()` - base portal URL useful for buidling REST service URLs
  * `initRequest(request)` - initialize jQuery Ajax request object within unificated done/fail arguments order: data, jqXHR status, jQuery text status or error, jqXHR object)
  * `addProvider(provider)` - register a new provider object (requirements see in 'Implementing Javascript SPI' section), this call provider will be initialized, if the core itself initialized (otherwise initialization will be postponed to init() call of the core module)
  * `findProvider(type)` - lookups for a provider registration in the core by its type name, will return a provider object if it was registered (no matter was it configured, initialized or not)
* Asynchronous methods that return a [promise](http://api.jquery.com/deferred.promise/) when resolved (with result object) or failed (with error object):
  * `getProvider(type)` - returns a promise that will be resolved with a provider found by given type name, the promise will be only resolved if provider successfully initialized, otherwise it will be in failed state with an message describing a problem. Second parameter of the resolved promise is a boolean flag: will be `true` if the provider has `init()` method and it was called successfully or it is without such method; flag will be `false` if provider's `init()` failed or provider not activated by administrator.
  * `getCall(id)` - read call object from server storage by its ID
  * `updateUserCall(id, state)` - update current user call by its ID with given status (one of 'started', 'stopped' or 'joined', 'leaved' by the user)
  * `deleteCall(id)` - delete call by its ID
  * `addCall(id, callInfo)` - create a new call by ID and call object
  * `getUserGroupCalls()` - return all groups call current user had joined, including started and stopped ones
  * `getUserStatus(id)` - return user status in eXo Platform by its ID, this data will be read from `RESTUserService` of the Platform, the response object has `status` field
* Subscription methods that use callback(s): `onUpdate` will be called on a new data published in the channel, `onError` if subscription error happen, `onReady` when successfully subscribed:
  * `onUserUpdate(userId, onUpdate, onError, onReady)` - subscribe to user channel for its call updates (started-incoming, stopped etc) 
  * `onCallUpdate(callId, onUpdate, onError, onReady)` - subscribe to a particular call channel for its updates (receive any data related the call: connectivity, media or user information)
* Subscription (publishing) methods that use promise: when data published a promise will be resolved, if failed then promise rejected with an error:
  * `toCallUpdate(callId, data)` - publish data to a aprticular call channel (use this method to send call related data to other peers, for reading use `onCallUpdate()`)
* Utility methods (synchronous):
  * `showCallPopup(url, name)` - a helper to show a new browser page as a popup window (not a new tab) with width of 80% of the screen available dimensions and with given window name (can be used  latef for focusing a window)
  * `imAccount(user, type)` - a helper to find in user object an IM ofr given type name
  * `getLog()` - return logger that can spool messages to server log file (for details see 'Remote log' section below)
  * `showInfo(title, text)`, `showConfirm(title, text)`, `showWarn(title, text)`,  `showError(title, text, errorRef)` - show a popup to user with title and text. For error it also can show a error reference (see also 'Remote log' section below)
  * `noticeWarn(title, text, onInit)`, `noticeError(title, text, onInit)`, `noticeInfo(title, text, onInit)` - will show a notice bar using embedded Pnotify framework

Remote logger
===========

While establishing a call between peers it may be useful to collect related information and, especially, errors that happen at client side (in Javascript). Web Conferencing core module has a tool that gives a simple way to log messages at different levels in client browser console and, optionaly, it allows to spool these logs to a Platfrom server log. These logs will have a predefined format and all reported as `CallLog` logger messages at the server side. At the same time it's possible to customize message prefix to keep it unique to a some call provider or its instance (e.g. a call window).

Web Conferencing logger supports following log levels: _info_, _warn_, _error_, _debug_ and _trace_. Take in account that _trace_ messages will not be send to server log even if remote logger enabled.

In Javascript code samples of 'Creating a connection' section, you would find use of the logger. Here is a more detailed code of logger initialization:

```javascript
  // Obtain a logger instance for provider type 'myconnector'. This can be done in a provider's module.
  var log = webConferencing.getLog("myconnector");
  ....
  
  log.info("Incoming call: " + callId);
```

Then this info invocation will print following message to a client browser console:

```
  | INFO  | [myconnector_393215] Incoming call: mary@peter -- 2018-03-21T16:05:38.958Z
```

And server log will contains this:

```
  2018-03-21 18:06:48,202 | INFO  | [myconnector] mary-393215 Incoming call: mary@peter -- 2018-03-21T16:05:38.958Z [o.e.webconferencing.support.CallLog<org.exoplatform.webconferencing.support.CallLog-flusher>]
```

As seen, server log date later a bit after the actual message, it's because of logs caching, but the message contain client timestamp which is exactly the same in browser and server. There is also a client ID `393215` which uniquely identify a Javascript core module loaded and all its provider instances. And in the server log this ID prepended with a eXo user name for better clarity.

If your provider will open a new window/tab for actual call, then you may need distinguish logs reported by your provider module on Platform page and a call page module. Client IDs already will differ for both modules, but often we need a better, human readable, marker. For this purpose each logger can be customized with a prefix. In example below, we create a logger for call window:

```javascript
  var log = webConferencing.getLog("myconnector").prefix("call");
  ....
  
  log.info("Starting call: " + callId);
```

Then the message will looks like this:

```
  | INFO  | [myconnector.call_637565] Starting call: mary@peter -- 2018-03-21T16:05:39.546Z
```

And its server version:

```
  2018-03-21 18:06:48,202 | INFO  | [myconnector.call] peter-637565 Starting call: mary@peter -- 2018-03-21T16:05:39.546Z [o.e.webconferencing.support.CallLog<org.exoplatform.webconferencing.support.CallLog-flusher>]
```

Enable remote logger
-------------

Spooling logs to a server log is disabled by default. If a call provider requires this feature, then it should be enabled at Java SPI level. In a provider class, that extends `CallProvider`, need implement `isLogEnabled()` method that will find is remote logs spooling enabled or disabled for this provider.  It's recommended to keep this settings interactive and let Platform administrators to enable or disable remote logs, as this feature may be resource consumable and may affect production system. Persist this flag in a database or eXo's `SettingService` and reuse after server restart in the provider class. A good place for this flag in UI is in a provider settings form that can be added to Web Conferencing administrator page. 

Showing errors to users
-------------

When an error happens you may want to inform user about it. Often we don't need to show technical details to end-users - it may be not efficient and even confusing. But what is important, it's build a reference between a message user sees and a report to administrators or support and a actual (technical) error that occured. For this purpose Web Conferencing core introduces `showError()` method in its logger API. Note that __logger supports only reporting and showing of errors__ (messages will have _error_ level in the log). Find details in the code comments below:

```javascript
  try {
    // some code where error may happen
    throw "Call failure exception";
  } catch(err) {
    // Messages to log and show to an user may differ, thus this method accepts them separately in arguments:
    // 1. Message that will be printed in log (browser console and server log), it will be prepended the error message
    // 2. Error object, it can be an exception instance caught in Javascript runtime or returned by a failed promise or just a text string
    //    In case if it's an object, the logger will try to extract its message assuming that the object 
    //    has 'message' or 'name' fields or toString() method.
    //    Error can be 'null' or empty text, then it will be ignored.
    // 3. Title of a popup shown to an user
    // 4. OPTIONAL: Text to show in a popup for an user. If not given (undefined, null or empty) and error object found, 
    //    then message will be extracted from the error.
    log.showError("Message to log", err, "Error title", "Error message for user");
  }
```

This code will show following popup to an user:

![Error popup for user](/documentation/images/logShowError.png)

This message contains an error reference: `peter-673034-2018-03-23T09:02:07.801Z`. This reference, we show to an user, is build from current user name, Web Conferencing client ID and appended timestamp. All this data will be printed to the logger, making finding of technical message straightforward.

The browser console will print this:

```
  | ERROR | [myconnector_673034] Message to log. Error: Call failure exception -- 2018-03-23T09:02:07.801Z
```

Platform server log will contain this:

```
  2018-03-23 11:02:23,838 | ERROR | [myconnector] peter-673034 Message to log. Error: Call failure exception -- 2018-03-23T09:02:07.801Z [o.e.webconferencing.support.CallLog<org.exoplatform.webconferencing.support.CallLog-flusher>]
```

In a case if you build an own UI and cannot use the predefined popup (e.g. on a brand styled call page), then logger API offers another method to find a reference of just logged error and use it in user messages or for other provider needs. You can log error and get the logged error reference in a call-back function as shown below:

```javascript
  log.error("Message to log", err, function(errRef) {
    // In the callback show your custom message to an user
    showMyCustomError("Error title", "Error message for user", errRef);
  });
```








