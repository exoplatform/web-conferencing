# eXo Web Conferencing :: Connector Template

eXo Web Conferencing addon is plugging and virtually can manage any web conferencing provider. Each connector should implement a SPI (Service Provider Interface) which consists of Java and Javascript APIs. Connector provider code organzied such way that offers maximum freedom to developers for implementing a Call provider functions within Web Confrencing core.

## Template Project

An easist way to get get started it's copy a template project from Web Conferencing addon sources and start implmenting specifis in this code. This template project already offers ready implementaions of classes required for a call provider based on embedable widgets (what may be offered by a Javascript SDK of particular video call solution).
Below a main features that the project offers in code:
* integration in all places where Web Conferencing Call Button possible: spaces, user popovers and profiles, chat rooms
<img src="https://raw.github.com/exo-addons/web-conferencing/master/documentation/template/user_popover.png" width="512" alt="User popover">

* call page for a call, it will open in a new browser window where call widget can be added by a developer
<img src="https://raw.github.com/exo-addons/web-conferencing/master/documentation/template/call_window.png" width="512" alt="Call page">

* call notifications between participants to show an incoming call popover on the Platform pages
* a tooling to organize any data exchange between clients (based on CometD/WebSocket), it may be useful for connectivity settings or business logic exchanging via subscriber/publisher channel
* an Instant Messenger type for user profile, where user will add his real account and optionally enter his credentials or other personal settings
* configuration for a connector settings
* configuration to deploy to eXo Platform server

All the template code has helpful comments in the code - follow them to transform the template in your connector.

You may want to build a connector and deploy to your eXo Platform server to see how it appears and generate an idea of your implementation. Afte successful build find in _/template/packaging/target_ file _web-conferencing-myconnector.zip_, extract it to the Platform root folder and start the server.

## Java API

First place where to start developing in the template project, it's connector provider class: _MyConnectorProvider_ it extends _CallProvider_ and offers a type, title, all supported types (if actual) and a version of the connector. You can add any number of additional fields to this class - al them will be serialized to JSON and transfered to Javascript client when Web Conferencing load the connector. This serialization will be done from _MyConnectorSettings_ instance which provided by _getSettings()_ method and used in related servlet and portlet of the connector. 
Connector provider also is responsible for reading a configuration from XML file and keeping it up to date in runtime in case of changes (e.g. by admininistrator).

User profile IM type is in _MyConnectorIMRenderer_. 
TODO

Template project uses servelt with JSP page for showing a new call page. For a purpose of clean URL it uses a filter _MyCallFilter_ which will redirect user requests to _/portal/myconnector_ URLs to the servlet.

Call page servlet _MyCallServlet_ can be used to add additional logic and transfer settings to Javascript client (see _call.jsp_).

Portlet class _MyConnectorPortlet_ is for initializing Javascript module (_webconferencing-myconnector.js_) wich will add call buttons on the Platform pages and handle incoming calls from other users.

## Javascript API

Template connector consists of two client parts: 
* Javascript module (_webconferencing-myconnector.js_)  that provides integration logic and will be loaded on Platform pages to initialize call buttons and handle incoming calls
* Call page (_call.jsp_) with an user interface and Javascript to organize user interactions and media streaming (a placeholder for it).

Add your connector widget/UI to the call page if it is an embeddable stuff. Or, if video call solution offers it, open a call page directly on the remote server without a call page in eXo Platform.







