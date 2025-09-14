import React, { JSX, useContext, useState } from 'react';
import { motion, Variants } from "framer-motion";
import { AuthContext } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import UploadBox from '../components/UploadBox';
import ChatBox from '../components/ChatBox';


const tabContentVariants: Variants = {
  hidden: { opacity: 0, x: 20 },
  enter: { opacity: 1, x: 0 },
  exit: { opacity: 0, x: -20 },
};
// NOTE: replace your TabButton implementation with this (uses motion)
const TabButton: React.FC<{
  label: string;
  active: boolean;
  onClick: () => void;
}> = ({ label, active, onClick }) => {
  return (
    <motion.button
      onClick={onClick}
      // keep positioning so underline sits inside the button
      style={{
        ...styles.tabBtn,
        position: "relative",
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
      }}
      // hover/tap micro-interactions
      whileHover={{ scale: 1.03 }}
      whileTap={{ scale: 0.98 }}
      transition={{ type: "spring", stiffness: 400, damping: 28 }}
      aria-pressed={active}
    >
      <span style={{ pointerEvents: "none" }}>{label}</span>

      {/* animated underline that slides across buttons using a shared layoutId */}
      {active && (
        <motion.div
          layoutId="tab-underline"
          style={{
            position: "absolute",
            height: 4,
            left: 8,
            right: 8,
            bottom: 4,
            borderRadius: 4,
            background: "linear-gradient(90deg,#1544ff,#3a8dff)", // blue gradient
            boxShadow: "0 6px 18px rgba(50,60,120,0.14)",
          }}
          transition={{ type: "spring", stiffness: 600, damping: 30 }}
        />
      )}
    </motion.button>
  );
};


const HomePage: React.FC = () => {
  const { email, logout } = useContext(AuthContext) as any;
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<number>(1);

  const handleLogout = () => {
    logout();
    navigate('/');
  };
  const tabVariants = {
    hidden: { opacity: 0, x: 20 },
    enter: { opacity: 1, x: 0 },
    exit: { opacity: 0, x: -20 },
  };

  const renderLeft = (): React.ReactElement => {
    return activeTab === 1 ? (
      <motion.div
        key="left-tab-1"
        variants={tabContentVariants}
        initial="hidden"
        animate="enter"
        exit="exit"
        transition={{ duration: 0.33 }}
        style={{ width: "100%" }}
      >
        <UploadBox />
      </motion.div>
    ) : (
      <motion.div
        key={`left-tab-${activeTab}`}
        variants={tabContentVariants}
        initial="hidden"
        animate="enter"
        exit="exit"
        transition={{ duration: 0.33 }}
        style={styles.placeholder}
      >
        Content for Tab {activeTab} (Left)
      </motion.div>
    );
  };


  const renderRight = (): React.ReactElement => {
    return activeTab === 1 ? (
      <motion.div
        key="right-tab-1"
        variants={tabContentVariants}
        initial="hidden"
        animate="enter"
        exit="exit"
        transition={{ duration: 0.33 }}
        style={{ width: "100%" }}
      >
        <ChatBox />
      </motion.div>
    ) : (
      <motion.div
        key={`right-tab-${activeTab}`}
        variants={tabContentVariants}
        initial="hidden"
        animate="enter"
        exit="exit"
        transition={{ duration: 0.33 }}
        style={styles.placeholder}
      >
        Content for Tab {activeTab} (Right)
      </motion.div>
    );
  };


  return (
    <div style={styles.page}>
      <div style={styles.container}>
        <div style={styles.headerRow}>
          <h1 style={styles.heading}>
            You are logged in as <strong>{email}</strong>
          </h1>
          <button onClick={handleLogout} style={styles.outlineBtn}>Logout</button>
        </div>


        <div style={styles.tabBar}>
          <TabButton label="Tab 1" active={activeTab === 1} onClick={() => setActiveTab(1)} />
          <TabButton label="Tab 2" active={activeTab === 2} onClick={() => setActiveTab(2)} />
          <TabButton label="Tab 3" active={activeTab === 3} onClick={() => setActiveTab(3)} />
          <TabButton label="Tab 4" active={activeTab === 4} onClick={() => setActiveTab(4)} />
        </div>


        <div style={styles.grid}>
          <div style={styles.card}>{renderLeft()}</div>
          <div style={styles.card}>{renderRight()}</div>
        </div>
      </div>
    </div>
  );
};

export default HomePage;

const styles: { [key: string]: React.CSSProperties } = {
  outlineBtn: {
    padding: '0.6rem 1rem',
    borderRadius: '6px',
    background: 'white',
    color: '#5A4FCF',
    border: '1px solid #5A4FCF',
    cursor: 'pointer',
    fontWeight: 'bold',
  },
  tabBar: {
    display: 'flex',
    justifyContent: 'center',
    gap: '1rem',
    marginBottom: '2rem',
  },
  tabBtn: {
    background: '#e6ecff',
    border: 'none',
    padding: '0.75rem 1.5rem',
    borderRadius: '8px',
    cursor: 'pointer',
    fontWeight: 600,
    color: '#3a57e8',
    transition: 'all 0.2s ease',
  },
  tabBtnActive: {
    background: '#3a57e8',
    color: '#fff',
    boxShadow: '0 4px 12px rgba(58,87,232,0.3)',
    transform: 'translateY(-2px)',
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
  placeholder: {
    textAlign: 'center',
    color: '#999',
    fontStyle: 'italic',
    padding: '2rem 0',
  },
};