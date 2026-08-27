import { formatFileSize } from '../upload/uploadRules.js';

export default function MediaGalleryCard({ media, selected, onToggle, onDownload, onPreview, downloadDisabled }) {
  const isImage = media.mimeType.startsWith('image/');
  const createdAt = new Intl.DateTimeFormat('tr-TR', { dateStyle: 'medium', timeStyle: 'short' })
    .format(new Date(media.createdAt));

  return (
    <article className="media-gallery-card">
      <label className="media-selection-control">
        <input type="checkbox" checked={selected} onChange={() => onToggle(media.mediaId)} />
        Seç
      </label>
      {isImage && media.previewUrl ? (
        onPreview ? <button type="button" className="media-preview-trigger" onClick={() => onPreview(media)}><img src={media.previewUrl} alt={media.originalFilename} loading="lazy" decoding="async" referrerPolicy="no-referrer" /></button>
          : <img src={media.previewUrl} alt={media.originalFilename} loading="lazy" decoding="async" referrerPolicy="no-referrer" />
      ) : (
        <div className="video-media-placeholder" aria-label="Video dosyası">Video</div>
      )}
      <div className="media-gallery-details">
        <strong>{media.originalFilename}</strong>
        <span>{isImage ? 'Fotoğraf' : 'Video'} · {formatFileSize(media.sizeBytes)}</span>
        <span>{createdAt}</span>
      </div>
      <button type="button" className="secondary-button media-download-button" onClick={() => onDownload(media)} disabled={downloadDisabled}>
        İndir
      </button>
    </article>
  );
}
