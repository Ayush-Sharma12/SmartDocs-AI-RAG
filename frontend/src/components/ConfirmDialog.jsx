export default function ConfirmDialog({ title, message, onConfirm, onCancel }) {
  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <h2>{title}</h2>
        <p>{message}</p>
        <div className="modal-actions">
          <button className="button secondary" onClick={onCancel}>
            Cancel
          </button>
          <button className="button primary" onClick={onConfirm}>
            Confirm
          </button>
        </div>
      </div>
    </div>
  );
}
