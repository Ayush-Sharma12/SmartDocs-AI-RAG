import { useEffect } from "react";

export default function Toast({ message, type, onClose }) {
  useEffect(() => {
    const timer = setTimeout(onClose, 4000);
    return () => clearTimeout(timer);
  }, [onClose]);

  return (
    <div
      className={`toast toast-${type}`}
      role={type === "error" ? "alert" : "status"}
      aria-live={type === "error" ? "assertive" : "polite"}
      tabIndex={0}
      onKeyDown={(e) => (e.key === "Escape" || e.key === "Enter") && onClose()}
    >
      <div className="toast-content">
        <span className="toast-icon" aria-hidden="true">
          {type === "success" ? "✓" : type === "error" ? "✕" : "ℹ"}
        </span>
        <p>{message}</p>
      </div>
      <button type="button" className="toast-close" onClick={onClose} aria-label="Close notification">×</button>
    </div>
  );
}
