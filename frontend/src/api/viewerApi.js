import { apiFetch } from './client.js';
import { mediaPagePath } from '../utils/mediaPagination.js';

export class ViewerApiError extends Error {
  constructor(status) {
    super('Viewer API request failed.');
    this.status = status;
  }
}

async function request(path, options) {
  let response;
  try {
    response = await apiFetch(path, options);
  } catch {
    throw new ViewerApiError();
  }

  if (!response.ok) {
    throw new ViewerApiError(response.status);
  }
  return response;
}

function eventPath(viewerToken) {
  return `/api/viewer/events/${encodeURIComponent(viewerToken)}`;
}

export async function getViewerEvent(viewerToken) {
  return (await request(eventPath(viewerToken))).json();
}

export async function getViewerMedia(viewerToken, cursor) {
  return (await request(mediaPagePath(`${eventPath(viewerToken)}/media`, cursor))).json();
}

async function download(path, options) {
  return (await request(path, options)).blob();
}

export function downloadViewerSingleMedia(viewerToken, mediaId) {
  return download(`${eventPath(viewerToken)}/media/${encodeURIComponent(mediaId)}/download`);
}

export function downloadViewerSelectedMedia(viewerToken, mediaIds) {
  return download(`${eventPath(viewerToken)}/media/download`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ mediaIds }),
  });
}

export function downloadViewerAllMedia(viewerToken) {
  return download(`${eventPath(viewerToken)}/media/download-all`);
}
