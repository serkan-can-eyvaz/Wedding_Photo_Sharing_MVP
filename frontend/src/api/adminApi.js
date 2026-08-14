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

async function authenticatedRequest(path) {
  const session = getAdminSession();
  if (!session) {
    throw new AdminApiError(401);
  }

  return request(path, {
    headers: {
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
