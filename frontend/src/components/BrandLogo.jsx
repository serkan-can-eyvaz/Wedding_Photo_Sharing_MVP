export const BRAND_NAME = 'aramızdan';
export const BRAND_TAGLINE = 'HER AN. BİR ARADA.';

export default function BrandLogo({ variant = 'lockup', className = '', decorative = false }) {
  const isMark = variant === 'mark';

  return (
    <img
      className={`brand-logo brand-logo-${variant}${className ? ` ${className}` : ''}`}
      src={isMark ? '/brand/aramizdan-mark.png' : '/brand/aramizdan-logo.png'}
      alt={decorative ? '' : (isMark ? `${BRAND_NAME} simgesi` : `${BRAND_NAME} — ${BRAND_TAGLINE}`)}
      aria-hidden={decorative || undefined}
    />
  );
}
