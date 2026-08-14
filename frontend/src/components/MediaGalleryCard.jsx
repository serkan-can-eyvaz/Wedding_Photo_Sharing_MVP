import { formatFileSize } from '../upload/uploadRules.js';

export default function MediaGalleryCard({ media, selected, onToggle }) {
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
        <img src={media.previewUrl} alt={media.originalFilename} referrerPolicy="no-referrer" />
      ) : (
        <div className="video-media-placeholder" aria-label="Video dosyası">Video</div>
      )}
      <div className="media-gallery-details">
        <strong>{media.originalFilename}</strong>
        <span>{isImage ? 'Fotoğraf' : 'Video'} · {formatFileSize(media.sizeBytes)}</span>
        <span>{createdAt}</span>
      </div>
    </article>
  );
}
