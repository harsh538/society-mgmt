import { createContext, useContext, useState, useMemo, useCallback } from 'react';

const AuthContext = createContext(null);

function readJson(key) {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('accessToken'));
  const [refreshToken, setRefreshToken] = useState(() =>
    localStorage.getItem('refreshToken'),
  );
  const [user, setUser] = useState(() => readJson('user'));

  const login = useCallback((data) => {
    // Expected shape from /auth/login: { accessToken, refreshToken, member }
    const accessToken = data?.accessToken ?? null;
    const refresh = data?.refreshToken ?? null;
    const member = data?.member ?? data?.user ?? null;

    if (accessToken) localStorage.setItem('accessToken', accessToken);
    if (refresh) localStorage.setItem('refreshToken', refresh);
    if (member) localStorage.setItem('user', JSON.stringify(member));

    setToken(accessToken);
    setRefreshToken(refresh);
    setUser(member);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    setToken(null);
    setRefreshToken(null);
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({ user, token, refreshToken, login, logout, isAuthenticated: !!token }),
    [user, token, refreshToken, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
