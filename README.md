# eXo Web Conferencing add-on

eXo Web Conferencing offers a core support for making video calls in eXo Platform. It is a foundation for plugging and managing virtually any web conferencing solutions. 

Read our [blog post](https://www.exoplatform.com/blog/2018/01/23/platform-5-0-sneak-peek-web-conferencing-exo-platform) about Web Conferencing support in eXo Platform. This add-on is a part of eXo Platform Enterprise edition.

## Call directly from Platform page

Web Conferencing extends eXo Platform interface by adding call buttons of registrered connectors to users, spaces and chat rooms. Some connectors, such as Skype may require user to fill its profile with Skype IM account. But the add-on offers a free, no plugin required, video calls solution based on [WebRTC](https://en.wikipedia.org/wiki/WebRTC) technology. eXo Platform users don't need any additional setup to use it. 

![Call button in user popover](/documentation/images/callButton.png)

## Pluggable connectors

Web Conferencing offers WebRTC connector out of the box - this video call provider currently supports 1:1 calls in user profiles and in eXo Chat user rooms. 
For Skype connector refer to this [project](https://github.com/exo-addons/skype). 

## Developing a new connector

It's possible to create new connectors for Web Conferencing. There is a service provider interface (SPI) for building connectors for plugging third party video calling service providers inside the eXo Platform. Add-on also offers a [template project](/template) to bootsrap a new connector creation and show where to implement the SPI parts.
Developers can get started from [provider developer guide](/documentation/PROVIDER_GUIDE.md) to create a call connector for Web Conferencing and refer to [architecture](/documentation/ARCHITECTURE.md) for technical details. 
