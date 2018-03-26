eXo Web Conferencing: Connector Development Guide
=========================================

eXo Web Conferencing provides a service provider interface (SPI) for building connectors for plugging third party video calling service providers inside the eXo Platform interface. This guide contains quick tutorial with a code snippets to show how to start creating a custom connector and add a call button to eXo Platform using Connector SPI.

Getting started
===============

Web Conferencing consist of a core and call connectors implemented Connector SPI. Web Conferencing add-on it is a portal extension installed in eXo Platform. A call connector it is an another portal extensions using Web Conferencing core. Any connector consists of a server code and a client app with user interface for running actual calls.
When running, the Web Conferencing adds a Call Button of each available connector to users, spaces and chat rooms in eXo Platform. But connector should provide an implementation of its call button, including markup in HTML, CSS styles and user action handlers. It's not required for connector to care how and where the button will appear - this will be done by the core functionality of Web Conferencing. Instead, connector handles user clicks for its button, may change its style to reflect the call state and finally provide a call interface to an user. It's depends on a call provider how a call will appear to an user: in a new window or embeded to the current page. 

Additionally the Web Conferencing core offers a [Common API](/documentation/ARCHITECTURE.md#common-api) to help maintaining conversation state and exchange call information between all clients of the call participants. This API may work as a scaffolding for a new connector implementation and it covers following things: provide context information, settings and state storage in Platform database, exchanging user and call data, sending client logs to server log, connector settings in Administrator menu, showing message and call popups to an user.

For more details and configuration refer to the architecture document where [connector creation](/documentation/ARCHITECTURE.md#creating-a-connector) described in advance.

Create Connector Project
========

A quickest way to start a new connector project - make a copy of [template](/template) project. It already contains a Maven modules with Web Conferencing dependencies and packaging. Make a copy of this folder to your new location and change its names in source files and configuration, rename respectively package, class names and variables in the code. Go to the sources and fill with a logic for your connector. If need, add required third-party libraries to the Maven dependencies and assembly of the packaging. Finally build the project and use its packaging artifact as a connector extension. You may copy JAR files to libraries of your Platform server and copy WAR to webapp folder. Alternatively, you may use [eXo Add-ons manager](https://docs.exoplatform.org/public/index.jsp?topic=%2FPLF50%2FPLFAdminGuide.AddonsManagement.html) _install_ command in _offline_ mode and modify it's local catalog to use your local packaging (find add-ons manager catalog in `/addons/catalogs` folder of your server).


Implement Java SPI
========

First you create a Java class of your provider: it should extend `CallProvider` and implement abstract methods. In our case the provider class is very simple and works as a POJO for provider settings such as type and title. Below a stripped code of such class. 

```java
package org.exoplatform.webconferencing.myconnector;

/**
 * My Connector provider implementation.
 */
public class MyConnectorProvider extends CallProvider {
  
  /**
   * Instantiates a new My Call provider.
   *
   * @param params the params (from configuration.xml)
   * @throws ConfigurationException the configuration exception
   */
  public MyConnectorProvider(InitParams params) throws ConfigurationException {
    super(params);
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
}
```

At a next step, create a portlet that will load and initialize the provider in Platform UI.

```java
  /**
   * My Connector provider portlet that loads Javascript module of this connector and register its provider(s).
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
          JavascriptManager js = ((WebuiRequestContext) WebuiRequestContext.getCurrentInstance()).getJavascriptManager();
          // first load Web Conferencing itself,
          js.require("SHARED/webConferencing", "webConferencing")
            // load our connector module to myProvider variable
            .require("SHARED/webConferencing_myconnector", "myProvider")
            // check if the variable contains an object to ensure the provider was loaded successfully
            .addScripts("if (myProvider) { "
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

Finally configure connector extension, provider plugin and its portlet as described in [configuration](/documentation/ARCHITECTURE.md#extension-configuration) section.

Implement Javascript SPI
==============

Client side of a connector contains a logic to build a call button and return it to Web Conferencing core. Add an own `callButton` function to your provider module code. This method should be public in provider object and return a [promise](http://api.jquery.com/deferred.promise/) object which will be resolved with a JQuery element of a button container. When clicked, this button will open a call user interface. There are also other mandatory function of Connector SPI to get the provider type, with all supported types (which contains a single type in our case) and a title. 

```javascript
/**
 * My Connector provider module for Web Conferencing
 */
(function($, webConferencing) {
  /** 
   * An implementation of Web Conferencing Connector SPI contract for a call provider.
   */
  function MyProvider() {
    var self = this; // to referene this provider instance later
    
    /**
     * Return a call type name.
     */
    this.getType = function() {
      return "mycall";
    };

    /**
     * Return all call types supported by a connector.
     */
    this.getSupportedTypes = function() {
      return [ self.getTitle() ];
    };

    /**
     * Return human-readable title of a connector.
     */
    this.getTitle = function() {
      return "My Call";
    };
    
    /**
     * Build a Call button and handle clicks on it. 
     */
    this.callButton = function(context) {
      var button = $.Deferred();
      context.details().done(function(target) {
        var $button = $("<a title='" + target.title + "' href='javascript:void(0)' class='myCallAction'>"
              + "<i class='uiIconMyCall uiIconVideoPortlet uiIconLightGray'></i>"
              + "<span class='callTitle'>My Call</span></a>");
        // Add click handler to the button and add logic to open a link of call UI
        $button.click(function() {
          // When user click the button - create an actual call by ID you know or just built.
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
              webConferencing.showError("Joining call error", err);
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
  }
  // Finally return an instance of provider 
  return new MyProvider();
})($, webConferencing);
```

Then you need register this Javascript module in portal as described [here](/documentation/ACHITECTURE.md#implementing-javascript-spi);

Running All Together
=========

First, ensure that Web Conferencing add-on already installed (it is a part of Enterprise packaging, but may not exist in Community bersion). 
Run command in root folder of the server:
```
  ./addon install exo-web-conferencing
```
It will warn if the add-on already installed. 
 
After you've created Java and Javascript parts of your provider, you need to build it to make a package ready for installation in eXo Platform server. In the packaging folder of the project, in `target` sub-folder, you'll find a zip file, e.g. `web-conferencing-myconnector.zip`, it's your connector extension binary. You can extract it and copy `lib` folder, with JAR files, to libraries of your Platform server and copy `webapps`, with a WAR, to the web apps folder. Alternatively, you may use [eXo Add-ons manager](https://docs.exoplatform.org/public/index.jsp?topic=%2FPLF50%2FPLFAdminGuide.AddonsManagement.html) _install_ command in _offline_ mode and modify it's local catalog to use your local packaging (find add-ons manager catalog in `/addons/catalogs` folder of your server).
When extension installed start the server and login under some user and check places where your button appear: in user profile and popovers, in a space banner. If you installed eXo Chat, your button also will appear in the chat rooms. 

![My Call in user popover](/documentation/template/user_popover.png)











