import { useEffect, useRef, type ReactNode } from 'react';

/** Modal built on the native <dialog>: focus trapping, Esc to close and a backdrop for free. */
export default function Dialog({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = ref.current;
    if (dialog && !dialog.open) dialog.showModal();
  }, []);

  return (
    <dialog
      ref={ref}
      className="dialog"
      aria-labelledby="dialog-title"
      onCancel={(e) => {
        e.preventDefault();
        onClose();
      }}
      onClick={(e) => {
        // Clicking the backdrop (the dialog element itself, outside its content) closes it
        if (e.target === ref.current) onClose();
      }}
    >
      <div className="dialog-body">
        <h2 id="dialog-title">{title}</h2>
        {children}
      </div>
    </dialog>
  );
}
