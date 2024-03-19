
export function getActiveProvidersForSpace(identityId) {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/webconferencing/${identityId}/providers`, {
    credentials: 'include',
    method: 'GET'
  }).then(resp => {
    if (resp.ok) {
      return resp.json();
    } else {
      throw new Error('Error when retrieving active providers for space');
    }
  });
}

export function saveActiveProvider(provider, spaceId) {

  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/webconferencing/provider?spaceId=${spaceId}`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(provider),
  }).then((resp) => {
    if (!resp || !resp.ok) {
      throw new Error('Error saving ActiveProvider');
    }
  });
}

export function updateVideoConferenceEnabled(spaceId, enabled) {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/webconferencing/updateVideoConferenceEnabled?spaceId=${spaceId}&enabled=${enabled}`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json'
    },
  }).then((resp) => {
    if (!resp || !resp.ok) {
      throw new Error('Error updating a VideoConference enabled');
    }
  });
}

export function isVideoConferenceEnabled(spaceId) {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/webconferencing/isVideoConferenceEnabled?spaceId=${spaceId}`, {
    credentials: 'include',
    method: 'GET'
  }).then(resp => {
    if (resp.ok) {
      return resp.json();
    } else {
      throw new Error('Error checking a VideoConference enabled for space');
    }
  });
}


