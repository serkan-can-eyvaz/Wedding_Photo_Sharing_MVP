import { useCallback, useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import { GuestApiError, createPresignedUpload, getPublicEvent, registerMedia } from '../api/guestUploadApi.js';
import UploadFileRow from '../components/UploadFileRow.jsx';
import { uploadToR2 } from '../upload/r2Upload.js';
import { validateFileSelection } from '../upload/uploadRules.js';

const MAX_CONCURRENT_UPLOADS = 3;

export default function GuestEventPage() {
  const { token } = useParams();
  const [eventState, setEventState] = useState({ status: 'loading' });
  const [uploads, setUploads] = useState([]);
  const [selectionError, setSelectionError] = useState('');
  const [isBatchRunning, setIsBatchRunning] = useState(false);
  const uploadId = useRef(0);
  const uploadsRef = useRef([]);

  const updateUploads = useCallback((updater) => {
    setUploads((current) => {
      const next = updater(current);
      uploadsRef.current = next;
      return next;
    });
  }, []);

  const updateUpload = useCallback((id, changes) => {
    updateUploads((current) => current.map((item) => (
      item.id === id ? { ...item, ...changes } : item
    )));
  }, [updateUploads]);

  useEffect(() => {
    let cancelled = false;

    setEventState({ status: 'loading' });
    getPublicEvent(token)
      .then((event) => {
        if (!cancelled) {
          setEventState({ status: 'ready', event });
        }
      })
      .catch((error) => {
        if (cancelled) {
          return;
        }

        if (error instanceof GuestApiError && error.status === 404) {
          setEventState({ status: 'not-found' });
          return;
        }

        setEventState({ status: 'error' });
      });

    return () => {
      cancelled = true;
    };
  }, [token]);

  const processUpload = useCallback(async (id, retryRegistration) => {
    const item = uploadsRef.current.find((candidate) => candidate.id === id);
    if (!item || (!retryRegistration && item.status !== 'ready') || (retryRegistration && item.status !== 'failed')) {
      return;
    }

    let storageKey = retryRegistration && item.uploadedToR2 ? item.storageKey : null;
    let objectUploaded = Boolean(storageKey);

    try {
      if (!storageKey) {
        updateUpload(id, { status: 'uploading', progress: null, message: '', uploadedToR2: false, storageKey: null });
        const presignedUpload = await createPresignedUpload(token, item.file);
        storageKey = presignedUpload.storageKey;
        updateUpload(id, { storageKey, progress: 0 });

        await uploadToR2({
          uploadUrl: presignedUpload.uploadUrl,
          file: item.file,
          requiredHeaders: presignedUpload.requiredHeaders,
          onProgress: (progress) => updateUpload(id, { progress }),
        });
        objectUploaded = true;
        updateUpload(id, { status: 'registering', progress: 100, uploadedToR2: true });
      } else {
        updateUpload(id, { status: 'registering', message: '' });
      }

      try {
        await registerMedia(token, storageKey, item.file.name);
      } catch (error) {
        // M8 currently reserves 409 for an already registered, unique storage key.
        if (error instanceof GuestApiError && error.status === 409 && storageKey && objectUploaded) {
          updateUpload(id, { status: 'completed', message: '', progress: 100, uploadedToR2: true });
          return;
        }
        throw error;
      }

      updateUpload(id, { status: 'completed', message: '', progress: 100, uploadedToR2: true });
    } catch {
      updateUpload(id, {
        status: 'failed',
        message: 'Yükleme tamamlanamadı. Lütfen tekrar deneyin.',
        progress: null,
        storageKey: storageKey ?? null,
        uploadedToR2: objectUploaded,
      });
    }
  }, [token, updateUpload]);

  const runBatch = useCallback(async (jobs) => {
    if (jobs.length === 0) {
      return;
    }

    setIsBatchRunning(true);
    let nextJobIndex = 0;

    async function worker() {
      while (nextJobIndex < jobs.length) {
        const job = jobs[nextJobIndex];
        nextJobIndex += 1;
        await processUpload(job.id, job.retryRegistration);
      }
    }

    await Promise.all(Array.from({ length: Math.min(MAX_CONCURRENT_UPLOADS, jobs.length) }, worker));
    setIsBatchRunning(false);
  }, [processUpload]);

  const handleSelection = (event) => {
    const selected = Array.from(event.target.files ?? []);
    event.target.value = '';
    const result = validateFileSelection(selected);
    setSelectionError(result.error);

    if (result.files.length === 0) {
      return;
    }

    const newUploads = result.files.map(({ file, category }) => ({
        id: `upload-${uploadId.current++}`,
        file,
        category,
        status: 'ready',
        progress: null,
        message: '',
        storageKey: null,
        uploadedToR2: false,
      }));
    updateUploads((current) => [...current, ...newUploads]);
  };

  const handleUpload = () => {
    const jobs = uploadsRef.current
      .filter((item) => item.status === 'ready')
      .map((item) => ({ id: item.id, retryRegistration: false }));
    runBatch(jobs);
  };

  const handleRetry = (id) => {
    if (!isBatchRunning) {
      runBatch([{ id, retryRegistration: true }]);
    }
  };

  const handleRemove = (id) => {
    updateUploads((current) => current.filter((item) => item.id !== id));
  };

  if (eventState.status === 'loading') {
    return <p className="guest-page-state">Etkinlik yükleniyor...</p>;
  }

  if (eventState.status === 'not-found') {
    return <p className="guest-page-state">Bu etkinlik bulunamadı veya artık aktif değil.</p>;
  }

  if (eventState.status === 'error') {
    return <p className="guest-page-state">Etkinlik bilgisi alınamadı. Lütfen daha sonra tekrar deneyin.</p>;
  }

  const hasReadyFiles = uploads.some((item) => item.status === 'ready');
  const allCompleted = uploads.length > 0 && uploads.every((item) => item.status === 'completed');
  const eventDate = new Intl.DateTimeFormat('tr-TR', { dateStyle: 'long' }).format(new Date(`${eventState.event.eventDate}T00:00:00`));

  return (
    <section className="guest-upload-page">
      <header className="guest-event-header">
        <p className="guest-event-date">{eventDate}</p>
        <h1>{eventState.event.name}</h1>
        <p>Anılarınızı bizimle paylaşın.</p>
      </header>

      <label className="file-picker-button">
        Fotoğraf veya video seç
        <input
          type="file"
          multiple
          accept="image/jpeg,image/png,image/heic,image/heif,video/mp4,video/quicktime"
          onChange={handleSelection}
        />
      </label>
      <p className="upload-hint">Bir seferde en fazla 30 dosya seçebilirsiniz.</p>
      {selectionError && <p className="guest-error" role="alert">{selectionError}</p>}

      {uploads.length > 0 && (
        <ul className="upload-file-list">
          {uploads.map((item) => (
            <UploadFileRow
              key={item.id}
              item={item}
              onRemove={handleRemove}
              onRetry={handleRetry}
              retryDisabled={isBatchRunning}
            />
          ))}
        </ul>
      )}

      {hasReadyFiles && (
        <button type="button" className="primary-button" onClick={handleUpload} disabled={isBatchRunning}>
          Yüklemeyi başlat
        </button>
      )}
      {allCompleted && <p className="guest-success" role="status">Fotoğraf ve videolarınız başarıyla yüklendi.</p>}
    </section>
  );
}
