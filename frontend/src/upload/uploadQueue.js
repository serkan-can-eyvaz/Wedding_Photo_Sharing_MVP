export function createReadyUploadJobs(uploads) {
  return uploads
    .filter((item) => item.status === 'ready')
    .map((item) => ({ id: item.id, isRetry: false }));
}

export function createRetryUploadJobs(uploads, uploadIds) {
  const requestedIds = uploadIds ? new Set(uploadIds) : null;

  return uploads
    .filter((item) => item.status === 'failed' && (!requestedIds || requestedIds.has(item.id)))
    .map((item) => ({ id: item.id, isRetry: true }));
}
