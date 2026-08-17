import { formatFileSize } from '../upload/uploadRules.js';

const statusLabels = {
  ready: 'Hazır',
  uploading: 'Yükleniyor',
  registering: 'Galeriye ekleniyor',
  completed: 'Tamamlandı',
  failed: 'Başarısız',
};

export default function UploadFileRow({ item, onRemove, onRetry, retryDisabled }) {
  const canRemove = item.status === 'ready';
  const canRetry = item.status === 'failed' && !retryDisabled;
  const showsProgress = ['uploading', 'registering', 'completed'].includes(item.status);
  const progress = item.status === 'completed' || item.status === 'registering'
    ? 100
    : item.progress ?? 0;

  return (
    <li className={`upload-file-row upload-file-row-${item.status}`}>
      <span className={`upload-file-thumbnail upload-file-thumbnail-${item.category.toLowerCase()}`} aria-hidden="true">
        {item.category === 'Video' ? '▶' : '▧'}
      </span>
      <div className="upload-file-details">
        <div className="upload-file-title-row"><strong title={item.file.name}>{item.file.name}</strong><span className={`upload-status upload-status-${item.status}`}>{statusLabels[item.status]}</span></div>
        <span>{item.category} · {formatFileSize(item.file.size)}</span>
        {showsProgress && (
          <div className="upload-file-progress" aria-label={`${statusLabels[item.status]} ${progress}%`}>
            <span style={{ width: `${progress}%` }} />
          </div>
        )}
        {item.status === 'uploading' && item.progress !== null && <small>{item.progress}% yükleniyor</small>}
        {item.status === 'completed' && <small>Galeriye eklendi</small>}
        {item.message && <span className="upload-message">{item.message}</span>}
      </div>

      <div className="upload-file-actions">
        {canRemove && <button type="button" className="secondary-button" onClick={() => onRemove(item.id)}>Kaldır</button>}
        {canRetry && <button type="button" className="secondary-button" onClick={() => onRetry(item.id)}>Tekrar dene →</button>}
      </div>
    </li>
  );
}
