import React, { useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import AuthForm from '../components/AuthForm';
import { login, signup } from '../api/auth';
import { AuthContext } from '../context/AuthContext';

const LoginPage: React.FC = () => {
  const { login: setAuthToken } = useContext(AuthContext);
  const navigate = useNavigate();

  const handleAuthSubmit = async (
    email: string,
    password: string,
    mode: 'login' | 'signup'
  ) => {
    try {
      let token: string;

      if (mode === 'login') {
        const response = await login(email, password);
        token = response.token;
      } else {
        await signup(email, password);
        const response = await login(email, password);
        token = response.token;
      }

      if (!token) throw new Error('No token returned from backend');

      setAuthToken(token);
      navigate('/home');
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Authentication failed');
    }
  };


  return (
    <div>
      <AuthForm onSubmit={handleAuthSubmit} />
    </div>
  );
};

export default LoginPage;
