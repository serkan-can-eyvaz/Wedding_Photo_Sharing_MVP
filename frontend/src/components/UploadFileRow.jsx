import { formatFileSize } from '../upload/uploadRules.js';

const statusLabels = {
  ready: 'Hazır',
  uploading: 'Yükleniyor',
  registering: 'Kaydediliyor',
  completed: 'Tamamlandı',
  failed: 'Başarısız',
};

export default function UploadFileRow({ item, onRemove, onRetry, retryDisabled }) {
  const canRemove = item.status === 'ready';
  const canRetry = item.status === 'failed' && !retryDisabled;

  return (
    <li className="upload-file-row">
      <div className="upload-file-details">
        <strong>{item.file.name}</strong>
        <span>{item.category} · {formatFileSize(item.file.size)}</span>
        <span className={`upload-status upload-status-${item.status}`}>
          {statusLabels[item.status]}
          {item.status === 'uploading' && item.progress !== null ? ` · ${item.progress}%` : ''}
        </span>
        {item.message && <span className="upload-message">{item.message}</span>}
      </div>

      {item.status === 'uploading' && item.progress !== null && (
        <progress value={item.progress} max="100">{item.progress}%</progress>
      )}

      {canRemove && (
        <button type="button" className="secondary-button" onClick={() => onRemove(item.id)}>
          Kaldır
        </button>
      )}
      {canRetry && (
        <button type="button" className="secondary-button" onClick={() => onRetry(item.id)}>
          Tekrar dene
        </button>
      )}
    </li>
  );
}
