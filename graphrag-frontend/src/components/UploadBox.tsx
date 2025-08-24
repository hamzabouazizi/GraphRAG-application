import React, { useState, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { uploadPdf, PdfUploadResponse } from "../api/pdf";

const UploadBox: React.FC = () => {
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState<{ text: string; type: 'success' | 'error' | 'warning' | null }>({ text: '', type: null });
  const { token } = useContext(AuthContext);


  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setFile(e.target.files[0]);
      setMessage({ text: '', type: null });
    }
  };

  const handleUpload = async () => {
    if (!file || !token) return;

    const formData = new FormData();
    formData.append('file', file);

    setUploading(true);
    setMessage({ text: '', type: null });
    try {
      const response: PdfUploadResponse = await uploadPdf(file, token);

      if (response.message?.includes("already been uploaded")) {
        setMessage({ text: "This PDF has already been uploaded by this user.", type: "warning" });
      } else {
        setMessage({ text: "PDF uploaded successfully!", type: "success" });
      }

    } catch (error) {
      console.error(error);
      setMessage({ text: 'Failed to upload PDF', type: 'error' });
    } finally {
      setUploading(false);
    }
  };

  return (

    <div style={styles.card}>
      <h2 style={styles.title}>Upload your PDF</h2>
      <p style={styles.text}>This PDF will be processed and linked to your knowledge graph.</p>
      <input type="file" accept="application/pdf" onChange={handleFileChange} style={styles.input} />
      <button
        onClick={handleUpload}
        disabled={uploading || !file}
        style={{
          ...styles.button,
          opacity: uploading || !file ? 0.6 : 1,
          cursor: uploading || !file ? "not-allowed" : "pointer",
        }}
      >
        {uploading ? "Uploading..." : "Upload"}
      </button>


      {message.text && (
        <p
          style={{
            ...styles.message,
            ...(message.type === "success" ? styles.success : {}),
            ...(message.type === "error" ? styles.error : {}),
            ...(message.type === "warning" ? styles.warning : {}),
          }}
        >
          {message.text}
        </p>
      )}
    </div>
  );
};

export default UploadBox;

const styles: { [key: string]: React.CSSProperties } = {
  card: {
    background: "#fff",
    borderRadius: "12px",
    border: "1px solid #eee",
    padding: "1.25rem",
    boxShadow: "0 1px 8px rgba(0,0,0,0.06)",
    display: "flex",
    flexDirection: "column",
    gap: "0.75rem",
  },
  title: {
    fontSize: "1.2rem",
    margin: 0,
    color: "#5A4FCF",
    fontWeight: 600,
  },
  text: {
    fontSize: "0.95rem",
    color: "#555",
  },
  input: {
    padding: "0.6rem",
    borderRadius: "6px",
    border: "1px solid #ccc",
    fontSize: "0.95rem",
  },
  button: {
    padding: "0.8rem",
    background: "linear-gradient(to right, #5A4FCF, #7E6DE0)",
    color: "white",
    border: "none",
    borderRadius: "6px",
    fontWeight: "bold",
  },
  message: {
    marginTop: "0.5rem",
    fontSize: "0.95rem",
    fontWeight: 500,
  },
  success: { color: "green" },
  error: { color: "red" },
  warning: { color: "orange" },
};
