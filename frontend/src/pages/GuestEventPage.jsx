import { useCallback, useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import { GuestApiError, createPresignedUpload, getPublicEvent, registerMedia } from '../api/guestUploadApi.js';
import UploadFileRow from '../components/UploadFileRow.jsx';
import { uploadToR2 } from '../upload/r2Upload.js';
import { createReadyUploadJobs, createRetryUploadJobs } from '../upload/uploadQueue.js';
import { validateFileSelection } from '../upload/uploadRules.js';

const MAX_CONCURRENT_UPLOADS = 3;

function GuestBrandHeader() {
  return (
    <header className="guest-brand-header">
      <div className="guest-brand" aria-label="Marka Adı">
        <span className="guest-brand-mark" aria-hidden="true">✦</span>
        <span>Marka Adı</span>
      </div>
      <span className="guest-brand-context">ETKİNLİK GALERİSİ</span>
    </header>
  );
}

function GuestPageState({ title, message }) {
  return (
    <section className="guest-page-state">
      <GuestBrandHeader />
      <div className="guest-state-copy">
        <p className="guest-state-label">ETKİNLİK GALERİSİ</p>
        <h1>{title}</h1>
        <p>{message}</p>
      </div>
    </section>
  );
}

export default function GuestEventPage() {
  const { token } = useParams();
  const [eventState, setEventState] = useState({ status: 'loading' });
  const [uploads, setUploads] = useState([]);
  const [selectionError, setSelectionError] = useState('');
  const [isBatchRunning, setIsBatchRunning] = useState(false);
  const uploadId = useRef(0);
  const uploadsRef = useRef([]);
  const batchRunningRef = useRef(false);

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

  const processUpload = useCallback(async (id, isRetry) => {
    const item = uploadsRef.current.find((candidate) => candidate.id === id);
    if (!item || (!isRetry && item.status !== 'ready') || (isRetry && item.status !== 'failed')) {
      return;
    }

    let storageKey = isRetry && item.uploadedToR2 ? item.storageKey : null;
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
    if (jobs.length === 0 || batchRunningRef.current) {
      return;
    }

    batchRunningRef.current = true;
    setIsBatchRunning(true);
    try {
      let nextJobIndex = 0;

      async function worker() {
        while (nextJobIndex < jobs.length) {
          const job = jobs[nextJobIndex];
          nextJobIndex += 1;
          await processUpload(job.id, job.isRetry);
        }
      }

      await Promise.all(Array.from({ length: Math.min(MAX_CONCURRENT_UPLOADS, jobs.length) }, worker));
    } finally {
      batchRunningRef.current = false;
      setIsBatchRunning(false);
    }
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
    runBatch(createReadyUploadJobs(uploadsRef.current));
  };

  const handleRetry = (id) => {
    runBatch(createRetryUploadJobs(uploadsRef.current, [id]));
  };

  const handleRetryFailed = () => {
    runBatch(createRetryUploadJobs(uploadsRef.current));
  };

  const handleRemove = (id) => {
    updateUploads((current) => current.filter((item) => item.id !== id));
  };

  if (eventState.status === 'loading') {
    return <GuestPageState title="Etkinlik hazırlanıyor." message="Galeri bilgileri yükleniyor." />;
  }

  if (eventState.status === 'not-found') {
    return <GuestPageState title="Bu etkinlik şu anda kullanılamıyor." message="Bağlantıyı kontrol edip tekrar deneyebilirsiniz." />;
  }

  if (eventState.status === 'error') {
    return <GuestPageState title="Galeriye şu anda ulaşılamıyor." message="Lütfen bağlantınızı kontrol edip daha sonra tekrar deneyin." />;
  }

  const hasReadyFiles = uploads.some((item) => item.status === 'ready');
  const hasFailedFiles = uploads.some((item) => item.status === 'failed');
  const allCompleted = uploads.length > 0 && uploads.every((item) => item.status === 'completed');
  const completedCount = uploads.filter((item) => item.status === 'completed').length;
  const eventDate = new Intl.DateTimeFormat('tr-TR', { dateStyle: 'long' }).format(new Date(`${eventState.event.eventDate}T00:00:00`));

  return (
    <section className="guest-upload-page">
      <GuestBrandHeader />

      <main className="guest-upload-content">
        <header className="guest-event-header">
          <p className="guest-event-eyebrow">ETKİNLİK GALERİSİ</p>
          <h1>{eventState.event.name}</h1>
          <p className="guest-event-date">{eventDate}</p>
          <p className="guest-event-intro">Çektiğiniz fotoğraf ve videoları buraya ekleyin.</p>
        </header>

        <section className="guest-upload-intro" aria-labelledby="guest-upload-title">
          <p className="guest-upload-eyebrow">ANILARI PAYLAŞIN</p>
          <h2 id="guest-upload-title">Bu geceyi sizin gözünüzden de görelim.</h2>
          <p>Fotoğraf ve videolarınızı seçin. Yüklenen tüm anılar çiftin özel galerisinde toplanır.</p>
        </section>

        <div className="guest-upload-surface">
          <label className="file-picker-button guest-file-picker">
            <span className="guest-file-picker-mark" aria-hidden="true">+</span>
            <span className="guest-file-picker-copy"><strong>Fotoğraf veya video ekle</strong><span>Birden fazla dosya seçebilirsiniz</span></span>
            <input
              type="file"
              multiple
              accept="image/jpeg,image/png,image/heic,image/heif,video/mp4,video/quicktime"
              onChange={handleSelection}
            />
          </label>
          <p className="guest-upload-limits"><span>30 DOSYAYA KADAR</span><span>FOTOĞRAF 20 MB</span><span>VİDEO 500 MB</span></p>
          {selectionError && <p className="guest-error" role="alert">{selectionError}</p>}
        </div>

        {uploads.length > 0 && (
          <section className="guest-upload-selection" aria-label="Seçilen dosyalar">
            <div className="guest-upload-selection-header"><span>SEÇİLEN DOSYALAR</span><strong>{uploads.length} dosya</strong></div>
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
          </section>
        )}

        <div className="guest-upload-actions">
          {hasReadyFiles && (
            <button type="button" className="primary-button guest-upload-primary" onClick={handleUpload} disabled={isBatchRunning}>
              Yüklemeyi başlat <span aria-hidden="true">→</span>
            </button>
          )}
          {hasFailedFiles && (
            <button type="button" className="secondary-button guest-retry-all" onClick={handleRetryFailed} disabled={isBatchRunning}>
              Başarısız dosyaları tekrar dene <span aria-hidden="true">→</span>
            </button>
          )}
        </div>

        {hasFailedFiles && <div className="guest-upload-feedback guest-upload-feedback-error" role="alert"><span>BAZI DOSYALAR YÜKLENEMEDİ</span><p>Bağlantınızı kontrol edip tekrar deneyebilirsiniz.</p></div>}
        {allCompleted && <div className="guest-upload-feedback guest-upload-feedback-success" role="status"><span>ANILAR EKLENDİ</span><p>Teşekkürler. Fotoğraf ve videolarınız galeriye ulaştı.</p><small>{completedCount} dosya başarıyla yüklendi.</small></div>}

        <p className="guest-upload-trust">Yüklenen anılar etkinlik galerisine eklenir.</p>
      </main>
    </section>
  );
}
