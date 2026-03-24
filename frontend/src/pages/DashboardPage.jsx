import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  askQuestion,
  clearStoredAuth,
  deleteDocument,
  getDocuments,
  getStoredAuth,
  uploadDocument
} from "../services/api";
import Toast from "../components/Toast";
import ConfirmDialog from "../components/ConfirmDialog";
import LoadingSkeleton from "../components/LoadingSkeleton";

export default function DashboardPage() {
  const navigate = useNavigate();
  const auth = getStoredAuth();
  const [documents, setDocuments] = useState([]);
  const [selectedDocumentId, setSelectedDocumentId] = useState("");
  const [question, setQuestion] = useState("");
  const [answer, setAnswer] = useState("");
  const [error, setError] = useState("");
  const [loadingDocs, setLoadingDocs] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [asking, setAsking] = useState(false);
  const [toast, setToast] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [deleteConfirm, setDeleteConfirm] = useState(null);
  const [copied, setCopied] = useState(false);
  const answerPanelRef = useRef(null);

  useEffect(() => {
    loadDocuments();
  }, []);

  useEffect(() => {
    if (answer && answerPanelRef.current) {
      answerPanelRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [answer]);

  async function loadDocuments() {
    setLoadingDocs(true);
    setError("");
    try {
      const response = await getDocuments();
      setDocuments(response);
      if (!selectedDocumentId && response.length > 0) {
        setSelectedDocumentId(String(response[0].id));
      }
    } catch (loadError) {
      const errorMsg = resolveErrorMessage(loadError);
      setError(errorMsg);
      setToast({ message: errorMsg, type: "error" });
    } finally {
      setLoadingDocs(false);
    }
  }

  async function handleUpload(event) {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }

    setUploading(true);
    setError("");
    try {
      await uploadDocument(file);
      await loadDocuments();
      event.target.value = "";
      setToast({ message: "PDF uploaded successfully!", type: "success" });
    } catch (uploadError) {
      const errorMsg = resolveErrorMessage(uploadError);
      setError(errorMsg);
      setToast({ message: errorMsg, type: "error" });
    } finally {
      setUploading(false);
    }
  }

  async function handleAsk(event) {
    event.preventDefault();
    setAsking(true);
    setError("");
    setAnswer("");

    try {
      const response = await askQuestion(
        question,
        selectedDocumentId ? Number(selectedDocumentId) : null
      );
      setAnswer(response.answer);
      setQuestion("");
    } catch (askError) {
      const errorMsg = resolveErrorMessage(askError);
      setError(errorMsg);
      setToast({ message: errorMsg, type: "error" });
    } finally {
      setAsking(false);
    }
  }

  async function handleConfirmDelete() {
    if (!deleteConfirm) return;
    
    const documentId = deleteConfirm;
    setDeleteConfirm(null);
    setError("");
    try {
      await deleteDocument(documentId);
      const nextDocuments = documents.filter((document) => document.id !== documentId);
      setDocuments(nextDocuments);
      if (String(documentId) === selectedDocumentId) {
        setSelectedDocumentId(nextDocuments[0] ? String(nextDocuments[0].id) : "");
      }
      setToast({ message: "Document deleted successfully", type: "success" });
    } catch (deleteError) {
      const errorMsg = resolveErrorMessage(deleteError);
      setError(errorMsg);
      setToast({ message: errorMsg, type: "error" });
    }
  }

  function copyToClipboard() {
    navigator.clipboard.writeText(answer);
    setCopied(true);
    setToast({ message: "Answer copied to clipboard!", type: "success" });
    setTimeout(() => setCopied(false), 2000);
  }

  function handleLogout() {
    clearStoredAuth();
    navigate("/login", { replace: true });
  }

  const filteredDocuments = documents.filter((doc) =>
    doc.originalFilename.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <main className="page dashboard-page">
      <header className="dashboard-header">
        <div>
          <p className="eyebrow">Authenticated workspace</p>
          <h1>Welcome, {auth?.username || "user"}</h1>
          <p>Upload PDFs, select a document, and ask grounded questions.</p>
        </div>
        <div className="header-actions">
          <Link className="button secondary" to="/">
            Home
          </Link>
          <button className="button ghost" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </header>

      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}

      {deleteConfirm && (
        <ConfirmDialog
          title="Delete Document"
          message="Are you sure you want to delete this document? This action cannot be undone."
          onConfirm={handleConfirmDelete}
          onCancel={() => setDeleteConfirm(null)}
        />
      )}

      <section className="dashboard-grid">
        <div className="panel">
          <div className="panel-header">
            <h2>Your documents</h2>
            <label className="button primary upload-button">
              {uploading ? (
                <>
                  <span className="spinner"></span>
                  Uploading...
                </>
              ) : (
                "Upload PDF"
              )}
              <input type="file" accept="application/pdf" onChange={handleUpload} hidden />
            </label>
          </div>

          <input
            type="text"
            className="search-input"
            placeholder="Search documents..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />

          {loadingDocs ? (
            <div className="document-list">
              {[1, 2, 3].map((i) => <LoadingSkeleton key={i} />)}
            </div>
          ) : null}

          {!loadingDocs && documents.length === 0 ? (
            <p className="empty-state">📄 No PDFs yet. Upload one to start asking questions.</p>
          ) : null}

          {!loadingDocs && searchQuery && filteredDocuments.length === 0 ? (
            <p className="empty-state">No documents match your search.</p>
          ) : null}

          <div className="document-list">
            {filteredDocuments.map((document) => (
              <article
                key={document.id}
                className={`document-card ${
                  selectedDocumentId === String(document.id) ? "selected" : ""
                } ${document.indexed ? "" : "processing"}`}
              >
                <button
                  className="document-select"
                  onClick={() => setSelectedDocumentId(String(document.id))}
                >
                  <strong>{document.originalFilename}</strong>
                  <span className={document.indexed ? "indexed" : "processing"}>
                    {document.indexed ? "✓ Indexed" : "⏳ Processing"}
                  </span>
                </button>
                <button
                  className="mini-action"
                  onClick={() => setDeleteConfirm(document.id)}
                  title="Delete this document"
                >
                  🗑️
                </button>
              </article>
            ))}
          </div>
        </div>

        <div className="panel">
          <div className="panel-header">
            <h2>Ask a question</h2>
          </div>
          <form className="question-form" onSubmit={handleAsk}>
            <label>
              Target document
              <select
                value={selectedDocumentId}
                onChange={(event) => setSelectedDocumentId(event.target.value)}
              >
                <option value="">All my indexed documents</option>
                {documents.map((document) => (
                  <option key={document.id} value={document.id}>
                    {document.originalFilename}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Question
              <textarea
                value={question}
                onChange={(event) => setQuestion(event.target.value)}
                placeholder="What does this document say about configuration or setup?"
                rows={6}
                required
              />
            </label>
            {error ? <p className="error-text">❌ {error}</p> : null}
            <button className="button primary wide" type="submit" disabled={asking || !question.trim()}>
              {asking ? (
                <>
                  <span className="spinner"></span>
                  Thinking...
                </>
              ) : (
                "Ask SmartDocs"
              )}
            </button>
          </form>

          <div className="answer-panel" ref={answerPanelRef}>
            <div className="answer-header">
              <h3>Answer</h3>
              {answer && (
                <button
                  className="copy-button"
                  onClick={copyToClipboard}
                  title="Copy to clipboard"
                >
                  {copied ? "✓ Copied!" : "📋 Copy"}
                </button>
              )}
            </div>
            <p className={answer ? "answer-text" : "empty-answer"}>
              {answer || "Your grounded answer will appear here."}
            </p>
          </div>
        </div>
      </section>
    </main>
  );
}

function resolveErrorMessage(error) {
  return error?.response?.data?.error || error?.message || "Something went wrong";
}
