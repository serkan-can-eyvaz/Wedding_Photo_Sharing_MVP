const MEBIBYTE = 1024 * 1024;
const MAX_FILES_PER_BATCH = 30;

const rulesByMimeType = new Map([
  ['image/jpeg', { category: 'Fotoğraf', maxSizeBytes: 20 * MEBIBYTE }],
  ['image/png', { category: 'Fotoğraf', maxSizeBytes: 20 * MEBIBYTE }],
  ['image/heic', { category: 'Fotoğraf', maxSizeBytes: 20 * MEBIBYTE }],
  ['image/heif', { category: 'Fotoğraf', maxSizeBytes: 20 * MEBIBYTE }],
  ['video/mp4', { category: 'Video', maxSizeBytes: 250 * MEBIBYTE }],
  ['video/quicktime', { category: 'Video', maxSizeBytes: 250 * MEBIBYTE }],
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
