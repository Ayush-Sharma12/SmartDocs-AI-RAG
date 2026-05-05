import { useEffect } from "react";

export default function Toast({ message, type, onClose }) {
  useEffect(() => {
    const timer = setTimeout(onClose, 5000);
    return () => clearTimeout(timer);
  }, [onClose]);

  return (
    <div className={`toast toast-${type}`} role="status" aria-live={type === "error" ? "assertive" : "polite"}>
      <div className="toast-content">
        <span className="toast-icon" aria-hidden="true">
          {type === "success" ? "✓" : type === "error" ? "✕" : "ℹ"}
        </span>
        <p>{message}</p>
      </div>
      <button className="toast-close" onClick={onClose} aria-label="Close notification">×</button>
    </div>
  );
}
