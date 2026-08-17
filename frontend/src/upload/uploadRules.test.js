import assert from 'node:assert/strict';
import test from 'node:test';
import { MAX_IMAGE_SIZE_BYTES, MAX_VIDEO_SIZE_BYTES, validateFileSelection } from './uploadRules.js';

function file(name, type, size) {
  return { name, type, size };
}

test('accepts videos up to the 500 MiB limit and rejects one byte above it', () => {
  assert.equal(validateFileSelection([file('video.mp4', 'video/mp4', MAX_VIDEO_SIZE_BYTES)]).files.length, 1);
  assert.equal(validateFileSelection([file('video.mp4', 'video/mp4', MAX_VIDEO_SIZE_BYTES + 1)]).files.length, 0);
});

test('keeps the 20 MiB image limit unchanged', () => {
  assert.equal(validateFileSelection([file('image.jpg', 'image/jpeg', MAX_IMAGE_SIZE_BYTES)]).files.length, 1);
  assert.equal(validateFileSelection([file('image.jpg', 'image/jpeg', MAX_IMAGE_SIZE_BYTES + 1)]).files.length, 0);
});
