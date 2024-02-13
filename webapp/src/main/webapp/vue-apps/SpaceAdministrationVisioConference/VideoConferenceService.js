
export function getActiveProvidersForSpace(identityId) {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/webconferencing/${identityId}/providers`, {
    credentials: 'include',
    method: 'GET'
  }).then(resp => {
    if (resp.ok) {
      return resp.json();
    } else {
      throw new Error('Error when retrieving external visio settings');
    }
  });
}


