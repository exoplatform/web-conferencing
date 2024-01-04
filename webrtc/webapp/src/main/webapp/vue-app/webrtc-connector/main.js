window.require(['SHARED/webConferencing','SHARED/webConferencingPortlet', 'SHARED/webConferencing_webrtc'], function(webConferencing, webConferencingPortlet, webConferencing_webrtc) {
  if (webConferencing_webrtc) {
    fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/webrtc/webconferencing/connectorsettings`, {
      credentials: 'include',
      method: 'GET',
    }).then((resp) => {
      if (!resp || !resp.ok) {
        throw new Error('Error while getting webrtc provider configuration');
      } else {
        return resp.json();
      }
    }).then((data) => {
      webConferencingPortlet.start();
      webConferencing_webrtc.configure(data);
      webConferencing.addProvider(webConferencing_webrtc);
    });
  }
});



