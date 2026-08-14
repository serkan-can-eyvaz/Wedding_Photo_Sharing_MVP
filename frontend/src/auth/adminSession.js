const SESSION_STORAGE_KEY = 'wedding-share-admin-session';

export function getAdminSession() {
  try {
    const session = JSON.parse(sessionStorage.getItem(SESSION_STORAGE_KEY));
    const expirationTime = Date.parse(session?.expiresAt);
    if (!session?.token || !session?.expiresAt || Number.isNaN(expirationTime) || expirationTime <= Date.now()) {
      clearAdminSession();
      return null;
    }

    return {
      token: session.token,
      expiresAt: session.expiresAt,
    };
  } catch {
    clearAdminSession();
    return null;
  }
}

export function saveAdminSession({ token, expiresAt }) {
  sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify({ token, expiresAt }));
}

export function clearAdminSession() {
  sessionStorage.removeItem(SESSION_STORAGE_KEY);
}
