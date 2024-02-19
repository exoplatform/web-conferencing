
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

export function saveActiveProvider(provider) {

  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/webconferencing/provider`, {
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


