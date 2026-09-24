import { useEffect, useState } from 'react';

export default function CopyButton({ text, compact = false }: { text: string; compact?: boolean }) {
  const [state, setState] = useState<'idle' | 'copied' | 'failed'>('idle');

  useEffect(() => {
    if (state === 'idle') return;
    const timer = window.setTimeout(() => setState('idle'), 1800);
    return () => window.clearTimeout(timer);
  }, [state]);

  async function copy() {
    try {
      await navigator.clipboard.writeText(text);
      setState('copied');
    } catch {
      setState('failed');
    }
  }

  const label = state === 'copied' ? 'Copied' : state === 'failed' ? 'Copy failed' : 'Copy';
  return (
    <button
      type="button"
      className={`btn btn-ghost btn-sm copy${state === 'copied' ? ' is-copied' : ''}`}
      onClick={copy}
      aria-label={compact ? `Copy ${text}` : undefined}
    >
      <span aria-live="polite">{label}</span>
    </button>
  );
}
