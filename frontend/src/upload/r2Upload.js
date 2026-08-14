export function uploadToR2({ uploadUrl, file, requiredHeaders, onProgress }) {
  return new Promise((resolve, reject) => {
    const request = new XMLHttpRequest();

    request.open('PUT', uploadUrl);
    request.withCredentials = false;

    Object.entries(requiredHeaders ?? {}).forEach(([name, value]) => {
      request.setRequestHeader(name, value);
    });

    request.upload.onprogress = (event) => {
      if (event.lengthComputable) {
        onProgress(Math.round((event.loaded / event.total) * 100));
      }
    };

    request.onload = () => {
      if (request.status >= 200 && request.status < 300) {
        resolve();
        return;
      }

      reject(new Error('R2 upload failed.'));
    };
    request.onerror = () => reject(new Error('R2 upload failed.'));
    request.onabort = () => reject(new Error('R2 upload aborted.'));

    request.send(file);
  });
}
