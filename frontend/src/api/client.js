const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;

function createUrl(path) {
  if (!apiBaseUrl) {
    throw new Error('VITE_API_BASE_URL is required before making API requests.');
  }

  return `${apiBaseUrl.replace(/\/$/, '')}/${path.replace(/^\//, '')}`;
}

export function apiFetch(path, options) {
  return fetch(createUrl(path), options);
}
