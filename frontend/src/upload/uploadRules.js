export const MEBIBYTE = 1024 * 1024;
export const MAX_FILES_PER_BATCH = 30;
export const MAX_IMAGE_SIZE_BYTES = 20 * MEBIBYTE;
export const MAX_VIDEO_SIZE_BYTES = 500 * MEBIBYTE;

const rulesByMimeType = new Map([
  ['image/jpeg', { category: 'Fotoğraf', maxSizeBytes: MAX_IMAGE_SIZE_BYTES }],
  ['image/png', { category: 'Fotoğraf', maxSizeBytes: MAX_IMAGE_SIZE_BYTES }],
  ['image/heic', { category: 'Fotoğraf', maxSizeBytes: MAX_IMAGE_SIZE_BYTES }],
  ['image/heif', { category: 'Fotoğraf', maxSizeBytes: MAX_IMAGE_SIZE_BYTES }],
  ['video/mp4', { category: 'Video', maxSizeBytes: MAX_VIDEO_SIZE_BYTES }],
  ['video/quicktime', { category: 'Video', maxSizeBytes: MAX_VIDEO_SIZE_BYTES }],
]);

export function validateFileSelection(files) {
  if (files.length > MAX_FILES_PER_BATCH) {
    return {
      error: `Bir seferde en fazla ${MAX_FILES_PER_BATCH} dosya seçebilirsiniz.`,
      files: [],
    };
  }

  const validFiles = [];
  const errors = [];

  files.forEach((file) => {
    const mimeType = file.type.trim().toLowerCase();
    const rule = rulesByMimeType.get(mimeType);

    if (!rule) {
      errors.push(file.type ? `${file.name}: dosya türü desteklenmiyor.` : `${file.name}: dosya türü doğrulanamadı.`);
      return;
    }

    if (file.size <= 0 || file.size > rule.maxSizeBytes) {
      errors.push(`${file.name}: dosya boyutu desteklenen sınırı aşıyor.`);
      return;
    }

    validFiles.push({ file, category: rule.category });
  });

  return { error: errors.join(' '), files: validFiles };
}

export function formatFileSize(sizeBytes) {
  if (sizeBytes < MEBIBYTE) {
    return `${Math.max(1, Math.round(sizeBytes / 1024))} KiB`;
  }

  return `${(sizeBytes / MEBIBYTE).toFixed(1)} MiB`;
}
