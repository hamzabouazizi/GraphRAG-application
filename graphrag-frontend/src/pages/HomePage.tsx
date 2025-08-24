import React, { useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import UploadBox from '../components/UploadBox';
import ChatBox from '../components/ChatBox';

const HomePage: React.FC = () => {
  const { email, logout } = useContext(AuthContext) as any;
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <div style={styles.page}>
      <div style={styles.container}>
        <div style={styles.headerRow}>
          <h1 style={styles.heading}>
            You are logged in as <strong>{email}</strong>
          </h1>
          <button onClick={handleLogout} style={styles.outlineBtn}>
            Logout
          </button>
        </div>

        <div style={styles.grid}>
          <div style={styles.card}>
            <UploadBox />
          </div>
          <div style={styles.card}>
            <ChatBox />
          </div>
        </div>
      </div>
    </div>
  );
};

export default HomePage;

const styles: { [key: string]: React.CSSProperties } = {
  page: {
    background: '#f7f7ff',
    minHeight: '100vh',
    padding: '2rem',
  },
  container: {
    background: '#fff',
    padding: '2rem',
    borderRadius: '12px',
    boxShadow: '0 0 20px rgba(0,0,0,0.1)',
    maxWidth: '1000px',
    margin: '0 auto',
  },
  headerRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '1.5rem',
  },
  heading: {
    fontSize: '1.5rem',
    fontWeight: 600,
    color: '#5A4FCF',
    margin: 0,
  },
  outlineBtn: {
    padding: '0.6rem 1rem',
    borderRadius: '6px',
    background: 'white',
    color: '#5A4FCF',
    border: '1px solid #5A4FCF',
    cursor: 'pointer',
    fontWeight: 'bold',
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: '1fr 1fr',
    gap: '1.5rem',
  },
  card: {
    background: '#fff',
    borderRadius: '12px',
    border: '1px solid #eee',
    padding: '1.25rem',
    boxShadow: '0 1px 8px rgba(0,0,0,0.06)',
    display: 'flex',
    flexDirection: 'column',
    gap: '0.75rem',
  },
};
