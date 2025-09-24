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
            background: "white",
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

  const renderContent = (): React.ReactElement => {
    if (activeTab === 1) {
      return (
        <motion.div
          key="tab-1"
          variants={tabContentVariants}
          initial="hidden"
          animate="enter"
          exit="exit"
          transition={{ duration: 0.33 }}
          style={{ width: "100%" }}
        >
          <UploadBox />
        </motion.div>
      );
    }

    if (activeTab === 2) {
      return (
        <motion.div
          key="tab-2"
          variants={tabContentVariants}
          initial="hidden"
          animate="enter"
          exit="exit"
          transition={{ duration: 0.33 }}
          style={{ width: "100%" }}
        >
          <ChatBox />
        </motion.div>
      );
    }

    if (activeTab === 3) {
      return (
        <motion.div
          key="tab-3"
          variants={tabContentVariants}
          initial="hidden"
          animate="enter"
          exit="exit"
          transition={{ duration: 0.33 }}
          style={styles.placeholder}
        >
          Here you will see your uploaded documents.
        </motion.div>
      );
    }

    if (activeTab === 4) {
      return (
        <motion.div
          key="tab-4"
          variants={tabContentVariants}
          initial="hidden"
          animate="enter"
          exit="exit"
          transition={{ duration: 0.33 }}
          style={{ width: "100%", textAlign: "center" }}
        >
          <h2>
            You are logged in as <strong>{email}</strong>
          </h2>
          <button onClick={handleLogout} style={styles.outlineBtn}>
            Logout
          </button>
        </motion.div>
      );
    }

    return <></>;
  };



  return (
    <div style={styles.page}>
      <div style={styles.container}>


        <div style={styles.tabBar}>
          <TabButton label="Upload your Document" active={activeTab === 1} onClick={() => setActiveTab(1)} />
          <TabButton label="Ask your Document" active={activeTab === 2} onClick={() => setActiveTab(2)} />
          <TabButton label="My Documents" active={activeTab === 3} onClick={() => setActiveTab(3)} />
          <TabButton label="Account" active={activeTab === 4} onClick={() => setActiveTab(4)} />
        </div>

        <div style={styles.grid}>
          <div style={styles.card}>{renderContent()}</div>
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
    background: 'linear-gradient(to right, #5A4FCF, #7E6DE0)',
    color: 'white',
    border: 'none',
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
    background: 'linear-gradient(to right, #5A4FCF, #7E6DE0)',
    border: 'none',
    padding: '0.75rem 1.5rem',
    borderRadius: '8px',
    cursor: 'pointer',
    fontWeight: 600,
    color: 'white',
    transition: 'all 0.2s ease',
  },

  tabBtnActive: {
    background: 'linear-gradient(to right, #5A4FCF, #7E6DE0)',
    color: 'white',
    boxShadow: '0 4px 12px rgba(255,255,255,0.6)',
    transform: 'translateY(-2px)',
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: '1fr',
    placeItems: 'center',
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
    alignItems: 'center',
    justifyContent: 'center',
    gap: '0.75rem',
  },
  placeholder: {
    textAlign: 'center',
    color: '#5A4FCF',
    fontStyle: 'italic',
    padding: '2rem 0',
  },
};