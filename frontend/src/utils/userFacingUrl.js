const TECHNICAL_DOMAIN = 'xn--aramzdan-wkb.com';
const USER_FACING_DOMAIN = 'aramızdan.com';

export function toUserFacingUrl(url) {
  if (typeof url !== 'string' || url.length === 0) {
    return url;
  }

  return url.replace(
    new RegExp(`^(https?://)${TECHNICAL_DOMAIN}(?=[:/?#]|$)`, 'i'),
    `$1${USER_FACING_DOMAIN}`,
  );
}
