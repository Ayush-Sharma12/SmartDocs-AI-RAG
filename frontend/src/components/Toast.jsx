import { useEffect } from "react";

export default function Toast({ message, type, onClose }) {
  useEffect(() => {
    const timer = setTimeout(onClose, 4000);
    return () => clearTimeout(timer);
  }, [onClose]);

  return (
    <div className={`toast toast-${type}`}>
      <div className="toast-content">
        <span className="toast-icon">
          {type === "success" ? "✓" : type === "error" ? "✕" : "ℹ"}
        </span>
        <p>{message}</p>
      </div>
      <button className="toast-close" onClick={onClose}>×</button>
    </div>
  );
}
