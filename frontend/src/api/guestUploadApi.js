import { apiFetch } from './client.js';

export class GuestApiError extends Error {
  constructor(status) {
    super('Guest API request failed.');
    this.status = status;
  }
}

async function request(path, options) {
  let response;

  try {
    response = await apiFetch(path, options);
  } catch {
    throw new GuestApiError();
  }

  if (!response.ok) {
    throw new GuestApiError(response.status);
  }

  return response;
}

function postJson(path, body) {
  return request(path, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(body),
  });
}

export async function getPublicEvent(token) {
  const response = await request(`/api/public/events/${encodeURIComponent(token)}`);
  return response.json();
}

export async function createPresignedUpload(token, file) {
  const response = await postJson(`/api/public/events/${encodeURIComponent(token)}/uploads/presign`, {
    filename: file.name,
    contentType: file.type,
    sizeBytes: file.size,
  });

  return response.json();
}

export function registerMedia(token, storageKey, originalFilename) {
  return postJson(`/api/public/events/${encodeURIComponent(token)}/media`, {
    storageKey,
    originalFilename,
  });
}
