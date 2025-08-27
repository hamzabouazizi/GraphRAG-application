import React, { createContext, useState, useEffect } from 'react';
import { jwtDecode } from 'jwt-decode';

type DecodedToken = {
  sub?: string;
  exp?: number;
  [key: string]: any;
};

interface AuthContextProps {
  token: string | null;
  email: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (token: string) => void;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextProps>({
  token: null,
  email: null,
  isAuthenticated: false,
  isLoading: true,
  login: () => { },
  logout: () => { },
});

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [token, setToken] = useState<string | null>(localStorage.getItem('token'));
  const [email, setEmail] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const decodeAndValidate = (tkn: string | null): boolean => {
    if (!tkn) return false;
    try {
      const decoded = jwtDecode<DecodedToken>(tkn);
      if (decoded?.exp && Date.now() / 1000 > decoded.exp) {
        return false;
      }
      setEmail(decoded.sub || decoded.email || null);
      return true;
    } catch (err) {
      console.error('Failed to decode token', err);
      return false;
    }
  };

  useEffect(() => {
    const stored = localStorage.getItem('token');
    if (stored && decodeAndValidate(stored)) {
      setToken(stored);
    } else {
      localStorage.removeItem('token');
      setToken(null);
      setEmail(null);
    }
    setIsLoading(false);
  }, []);

  useEffect(() => {
    if (!token) {
      setEmail(null);
      return;
    }
    if (!decodeAndValidate(token)) {
      localStorage.removeItem('token');
      setToken(null);
      setEmail(null);
    }
  }, [token]);

  const login = (newToken: string) => {
    localStorage.setItem('token', newToken);
    setToken(newToken);
    decodeAndValidate(newToken);
  };

  const logout = () => {
    setToken(null);
    setEmail(null);
    localStorage.removeItem('token');
  };
  const isAuthenticated = !!token && !!email;

  return (
    <AuthContext.Provider value={{ token, email, isAuthenticated, isLoading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};
