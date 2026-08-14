import { getAdminSession } from '../auth/adminSession.js';
import { apiFetch } from './client.js';

export class AdminApiError extends Error {
  constructor(status) {
    super('Admin API request failed.');
    this.status = status;
  }
}

async function request(path, options) {
  let response;

  try {
    response = await apiFetch(path, options);
  } catch {
    throw new AdminApiError();
  }

  if (!response.ok) {
    throw new AdminApiError(response.status);
  }

  return response;
}

export async function login(email, password) {
  const response = await request('/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ email, password }),
  });

  return response.json();
}

async function authenticatedRequest(path, options = {}) {
  const session = getAdminSession();
  if (!session) {
    throw new AdminApiError(401);
  }

  return request(path, {
    ...options,
    headers: {
      ...options.headers,
      Authorization: `Bearer ${session.token}`,
    },
  });
}

export async function getAdminEvents() {
  const response = await authenticatedRequest('/api/events');
  return response.json();
}

export async function getAdminEvent(eventId) {
  const response = await authenticatedRequest(`/api/events/${encodeURIComponent(eventId)}`);
  return response.json();
}

export async function getEventMedia(eventId) {
  const response = await authenticatedRequest(`/api/events/${encodeURIComponent(eventId)}/media`);
  return response.json();
}

async function download(path, options) {
  const response = await authenticatedRequest(path, options);
  return response.blob();
}

export function downloadSingleMedia(eventId, mediaId) {
  return download(`/api/events/${encodeURIComponent(eventId)}/media/${encodeURIComponent(mediaId)}/download`);
}

export function downloadSelectedMedia(eventId, mediaIds) {
  return download(`/api/events/${encodeURIComponent(eventId)}/media/download`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ mediaIds }),
  });
}

export function downloadAllMedia(eventId) {
  return download(`/api/events/${encodeURIComponent(eventId)}/media/download-all`);
}
