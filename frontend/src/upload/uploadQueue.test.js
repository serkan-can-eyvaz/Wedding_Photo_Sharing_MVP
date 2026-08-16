import assert from 'node:assert/strict';
import test from 'node:test';
import { createReadyUploadJobs, createRetryUploadJobs } from './uploadQueue.js';

test('retry queues only failed files and preserves completed files', () => {
  const uploads = [
    { id: 'A', status: 'completed', progress: 100 },
    { id: 'B', status: 'completed', progress: 100 },
    { id: 'C', status: 'failed', progress: null },
    { id: 'D', status: 'failed', progress: null },
    { id: 'E', status: 'failed', progress: null },
  ];
  const calls = new Map(uploads.map(({ id }) => [id, { presign: 0, put: 0, register: 0 }]));

  createRetryUploadJobs(uploads).forEach(({ id }) => {
    const fileCalls = calls.get(id);
    fileCalls.presign += 1;
    fileCalls.put += 1;
    fileCalls.register += 1;
  });

  assert.deepEqual(calls.get('A'), { presign: 0, put: 0, register: 0 });
  assert.deepEqual(calls.get('B'), { presign: 0, put: 0, register: 0 });
  assert.deepEqual(calls.get('C'), { presign: 1, put: 1, register: 1 });
  assert.deepEqual(calls.get('D'), { presign: 1, put: 1, register: 1 });
  assert.deepEqual(calls.get('E'), { presign: 1, put: 1, register: 1 });
  assert.equal(uploads[0].status, 'completed');
  assert.equal(uploads[1].progress, 100);
});

test('single-file retry rejects a completed item and permits a failed item again', () => {
  const uploads = [
    { id: 'completed', status: 'completed' },
    { id: 'failed', status: 'failed' },
  ];

  assert.deepEqual(createRetryUploadJobs(uploads, ['completed']), []);
  assert.deepEqual(createRetryUploadJobs(uploads, ['failed']), [{ id: 'failed', isRetry: true }]);
  assert.deepEqual(createRetryUploadJobs(uploads, ['failed']), [{ id: 'failed', isRetry: true }]);
});

test('normal upload queues only ready files', () => {
  const uploads = [
    { id: 'ready', status: 'ready' },
    { id: 'completed', status: 'completed' },
    { id: 'failed', status: 'failed' },
  ];

  assert.deepEqual(createReadyUploadJobs(uploads), [{ id: 'ready', isRetry: false }]);
});
