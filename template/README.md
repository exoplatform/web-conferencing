# eXo Web Conferencing :: Connector Template

eXo Web Conferencing addon is plugging and virtually can manage any web conferencing provider. Each connector should implement a Connector SPI (Service Provider Interface) which consists of Java and Javascript parts. Connector provider code organzied such way that offers maximum freedom to developers for implementing a call provider functions on top of Web Confrencing core [architecture](/documentation/ARCHITECTURE.md).

## Template Project

An easiest way to [get started](/documentation/PROVIDER_GUIDE.md) it's to copy a template project from Web Conferencing addon sources and start implementing its code. This template project already offers ready implementaions of classes required for a call provider based on embedable widgets (what may be offered by a Javascript SDK of particular video call solution).
Below a main features that the project offers in code:
* integration in all places where Web Conferencing Call Button possible: spaces, user popovers and profiles, chat rooms
<img src="https://raw.github.com/exoplatform/web-conferencing/develop/documentation/template/user_popover.png" width="412" alt="User popover">

* call page for a call, it will open in a new browser window where call widget can be added by a developer
<img src="https://raw.github.com/exoplatform/web-conferencing/develop/documentation/template/call_window.png" width="512" alt="Call page">

* call notifications between participants to show an incoming call popover on the Platform pages
* a tooling to organize any data exchange between clients (based on CometD/WebSocket), it may be useful for connectivity settings or business logic exchanging via subscriber/publisher channel
* an Instant Messenger type for user profile, where user will add his real account and optionally enter his credentials or other personal settings
* configuration for a connector settings
* configuration to deploy to eXo Platform server

All the template code has helpful comments in the code - follow them to transform the template in your connector.

You may want to build a connector and deploy to your eXo Platform server to see how it appears and generate an idea of your implementation. Afte successful build find in _/template/packaging/target_ file _web-conferencing-myconnector.zip_, extract it to the Platform root folder and start the server.

## Java SPI placeholders

Template project contains a provider class: `MyConnectorProvider` it extends `CallProvider` and offers a type, title, all supported types (if actual) and a version of the connector. Provider specific settings covered by `getSettings()` method and used in servlet and portlet of the provider. 
Provider class also is responsible for reading a configuration from XML file and keeping it up to date in runtime in case of changes (e.g. by admininistrator).

User profile IM type defined by `MyConnectorProvider` itself (see in constructor), but if you need additional settings user interface in user profile then you can do this via `MyConnectorIMRenderer` class, you will add there a HTML markup and Javascript to handle your settings UI. It is registred in XML configuration as a plugin of `UserProfileRenderingService` and it will be invoked when user profile rendered. If you don't need additional settings in user profile - remove this plugin from the configuration.

Template project uses servelt with JSP page for showing a call page. For a purpose of clean URL it uses a filter `MyCallFilter` which will redirect user requests to _/portal/myconnector_ URLs to the servlet.

Call page servlet `MyCallServlet` can be used to add additional logic and transfer settings to Javascript client (see _call.jsp_).

Portlet class `MyConnectorPortlet` is for loading and initializing Javascript module of the provider (_webconferencing-myconnector.js_) wich will create call buttons for adding to Platform pages and handle incoming calls from other users.

## Javascript SPI placeholders

Client side of the template connector consists of two parts: 
* Javascript module (_webconferencing-myconnector.js_)  that provides implementation of Connector SPI logic and will be loaded on Platform pages to initialize call buttons and handle incoming calls
* Call page (_call.jsp_), will open in a new browser page, with an user interface and Javascript placeholder to organize user interactions and media streaming.

You can user the call page and add your connector widget/UI to it if it's embeddable. If your video call solution offers an own approach, open a call page directly on the remote server without this call page in eXo Platform.

Note: if your call page will need large Javascript code, it's recommened to move it to a dedicated file and keep JSP page for markup only. 

## Provider Development

Use this template project and follow [provider developer guide](/documentation/PROVIDER_GUIDE.md) to create a call connector for Web Conferencing add-on.






