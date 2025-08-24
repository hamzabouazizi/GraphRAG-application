import React, { useState, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { sendChatMessage } from '../api/chat';

interface ChatMessage {
  sender: 'user' | 'bot';
  text: string;
}

const ChatBox: React.FC = () => {
  const { token } = useContext(AuthContext);
  const [input, setInput] = useState('');
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string>('');

  const handleSend = async () => {
    if (!input.trim() || !token) return;

    setErrorMessage('');
    const userMessage: ChatMessage = { sender: 'user', text: input };
    setMessages((prev) => [...prev, userMessage]);
    setInput('');
    setLoading(true);

    try {
      const data = await sendChatMessage(input, token);
      const botMessage: ChatMessage = { sender: 'bot', text: data.answer };
      setMessages((prev) => [...prev, botMessage]);
    } catch (error) {
      console.error(error);
      setErrorMessage('Failed to get response from chat backend.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.card}>
      <h2 style={styles.title}>Ask your document</h2>

      <div style={styles.chatArea}>
        {messages.map((msg, idx) => (
          <div
            key={idx}
            style={msg.sender === 'user' ? styles.userMsg : styles.botMsg}
          >
            {msg.text}
          </div>
        ))}
      </div>

      <div style={styles.inputArea}>
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSend()}
          placeholder="Ask a question..."
          style={styles.input}
        />
        <button
          onClick={handleSend}
          disabled={loading}
          style={{
            ...styles.button,
            opacity: loading ? 0.6 : 1,
            cursor: loading ? 'not-allowed' : 'pointer',
          }}
        >
          {loading ? 'Thinking...' : 'Send'}
        </button>
      </div>

      {errorMessage && <p style={styles.error}>{errorMessage}</p>}
    </div>
  );
};

export default ChatBox;


const styles: { [key: string]: React.CSSProperties } = {
  card: {
    background: '#fff',
    borderRadius: '12px',
    border: '1px solid #eee',
    padding: '1.25rem',
    boxShadow: '0 1px 8px rgba(0,0,0,0.06)',
    display: 'flex',
    flexDirection: 'column',
    gap: '0.75rem',
    width: '80%',
    maxWidth: '800px',
    margin: '0 auto',
  },
  title: {
    fontSize: '1.2rem',
    margin: 0,
    color: '#5A4FCF',
    fontWeight: 600,
  },
  chatArea: {
    flex: 1,
    minHeight: '200px',
    maxHeight: '300px',
    overflowY: 'auto',
    marginBottom: '0.5rem',
    padding: '0.75rem',
    backgroundColor: '#f9f9f9',
    borderRadius: '8px',
  },
  userMsg: {
    textAlign: 'right',
    marginBottom: '0.5rem',
    color: '#333',
    background: '#e6e6ff',
    padding: '6px 10px',
    borderRadius: '8px',
    display: 'inline-block',
    maxWidth: '80%',
  },
  botMsg: {
    textAlign: 'left',
    marginBottom: '0.5rem',
    color: '#5A4FCF',
    background: '#f0ebff',
    padding: '6px 10px',
    borderRadius: '8px',
    display: 'inline-block',
    maxWidth: '80%',
  },
  inputArea: {
    display: 'flex',
    gap: '0.5rem',
    alignItems: 'center',
    width: '100%',
  },
  input: {
    flex: 1,
    padding: '0.6rem',
    borderRadius: '6px',
    border: '1px solid #ccc',
    fontSize: '0.95rem',
    height: '42px',
  },
  button: {
    padding: '0 1.25rem',
    background: 'linear-gradient(to right, #5A4FCF, #7E6DE0)',
    color: 'white',
    border: 'none',
    borderRadius: '6px',
    fontWeight: 'bold',
    height: '42px',
    whiteSpace: 'nowrap',
  },
  error: {
    marginTop: '0.5rem',
    fontSize: '0.95rem',
    fontWeight: 500,
    color: 'red',
  },
};
