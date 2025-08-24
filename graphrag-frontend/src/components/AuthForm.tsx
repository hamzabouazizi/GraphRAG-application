import React, { useState } from 'react';
import { Link } from 'react-router-dom';


interface AuthFormProps {
  onSubmit: (email: string, password: string, mode: 'login' | 'signup') => void;
}

const AuthForm: React.FC<AuthFormProps> = ({ onSubmit }) => {
  const [mode, setMode] = useState<'login' | 'signup'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await onSubmit(email, password, mode);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.container}>
      <h2 style={styles.title}>Login Form</h2>
      <div style={styles.toggle}>
        <button
          style={mode === 'login' ? styles.activeToggleBtn : styles.inactiveToggleBtn}
          onClick={() => setMode('login')}
        >
          Login
        </button>
        <button
          style={mode === 'signup' ? styles.activeToggleBtn : styles.inactiveToggleBtn}
          onClick={() => setMode('signup')}
        >
          Signup
        </button>
      </div>
      <form onSubmit={handleSubmit} style={styles.form}>
        <input
          type="email"
          placeholder="Email Address"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
          style={styles.input}
        />
        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          style={styles.input}
        />


        <Link to="/forgot-password" className="text-sm text-[#5A4FCF] hover:underline block mt-2 text-right">
          Forgot password?
        </Link>


        <button type="submit" style={styles.loginButton} disabled={loading}>
          {loading ? <div style={spinnerStyle}></div> : (mode === 'login' ? 'Login' : 'Signup')}
        </button>

      </form>
      <p style={styles.footerText}>
        Not a member?{' '}
        <span
          style={styles.link}
          onClick={() => setMode(mode === 'login' ? 'signup' : 'login')}
        >
          {mode === 'login' ? 'Signup now' : 'Login here'}
        </span>
      </p>
    </div>
  );
};

export default AuthForm;


const styles: { [key: string]: React.CSSProperties } = {
  container: {
    background: '#fff',
    padding: '2rem',
    borderRadius: '12px',
    boxShadow: '0 0 20px rgba(0,0,0,0.1)',
    maxWidth: '400px',
    margin: '2rem auto',
    textAlign: 'center',
  },
  title: {
    fontSize: '1.8rem',
    marginBottom: '1rem',
    fontWeight: 600,
    color: '#5A4FCF',
  },
  toggle: {
    display: 'flex',
    marginBottom: '1rem',
    borderRadius: '6px',
    overflow: 'hidden',
    border: '1px solid #ccc',
  },
  activeToggleBtn: {
    flex: 1,
    padding: '0.5rem',
    background: 'linear-gradient(to right, #5A4FCF, #7E6DE0)',
    color: 'white',
    border: 'none',
    fontWeight: 'bold',
    cursor: 'pointer',
  },
  inactiveToggleBtn: {
    flex: 1,
    padding: '0.5rem',
    background: 'white',
    color: '#5A4FCF',
    border: 'none',
    cursor: 'pointer',
  },
  form: {
    display: 'flex',
    flexDirection: 'column',
    gap: '1rem',
  },
  input: {
    padding: '0.8rem',
    borderRadius: '6px',
    border: '1px solid #ccc',
    fontSize: '1rem',
  },
  forgot: {
    textAlign: 'left',
    color: '#cc2e8c',
    cursor: 'pointer',
    fontSize: '0.9rem',
  },
  loginButton: {
    padding: '0.8rem',
    background: 'linear-gradient(to right, #5A4FCF, #7E6DE0)',
    color: 'white',
    border: 'none',
    borderRadius: '6px',
    fontWeight: 'bold',
    fontSize: '1rem',
    cursor: 'pointer',
  },
  footerText: {
    marginTop: '1rem',
    fontSize: '0.9rem',
  },
  link: {
    color: '#5A4FCF',
    cursor: 'pointer',
    fontWeight: 'bold',
  },
};

const spinnerStyle: React.CSSProperties = {
  border: "3px solid #f3f3f3",
  borderTop: "3px solid #5A4FCF",
  borderRadius: "50%",
  width: "18px",
  height: "18px",
  animation: "spin 1s linear infinite",
  margin: "0 auto",
};


