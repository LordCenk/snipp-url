import { useCallback, useEffect, useState, type FormEvent } from 'react';
import * as api from '../api/client';
import type { Link } from '../api/client';
import Dialog from '../components/Dialog';
import CopyButton from '../components/CopyButton';
import { displayUrl, normalizeUrl } from '../lib/url';
import { formatCount, formatDateTime, isExpired, toApiInstant, toDateTimeInput, userTimeZone } from '../lib/format';

const PAGE_SIZE = 10;

export default function LinksPage() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<api.LinkPage | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [editing, setEditing] = useState<Link | null>(null);
  const [deleting, setDeleting] = useState<Link | null>(null);
  const [justCreated, setJustCreated] = useState<Link | null>(null);

  const load = useCallback(async (p: number) => {
    try {
      const result = await api.listLinks(p, PAGE_SIZE);
      // Deleting the last link on a page: step back a page
      if (result.items.length === 0 && p > 0) {
        setPage(p - 1);
        return;
      }
      setData(result);
      setLoadError(null);
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : 'Could not load your links.');
    }
  }, []);

  useEffect(() => {
    load(page);
  }, [page, load]);

  const pages = data ? Math.max(1, Math.ceil(data.total / PAGE_SIZE)) : 1;

  return (
    <div className="stack">
      <section className="card">
        <h1 className="page-title">Shorten a link</h1>
        <CreateLinkForm
          onCreated={(link) => {
            setJustCreated(link);
            if (page === 0) load(0);
            else setPage(0);
          }}
        />
        {justCreated && (
          <div className="created" role="status">
            <span className="muted">Your short link</span>
            <a href={api.shortUrl(justCreated.shortCode)} target="_blank" rel="noreferrer" className="short-link">
              {displayUrl(api.shortUrl(justCreated.shortCode))}
            </a>
            <CopyButton text={api.shortUrl(justCreated.shortCode)} />
          </div>
        )}
      </section>

      <section className="card">
        <div className="section-head">
          <h2>Your links</h2>
          {data && <span className="muted">{formatCount(data.total)} total</span>}
        </div>

        {loadError && <p className="alert alert-error" role="alert">{loadError}</p>}

        {data && data.items.length === 0 && !loadError && (
          <p className="empty">No links yet. Shorten your first one above.</p>
        )}

        {data && data.items.length > 0 && (
          <>
            <div className="table-wrap">
              <table className="table links-table">
                <thead>
                  <tr>
                    <th scope="col">Short link</th>
                    <th scope="col">Destination</th>
                    <th scope="col" className="num">Clicks</th>
                    <th scope="col">Status</th>
                    <th scope="col"><span className="sr-only">Actions</span></th>
                  </tr>
                </thead>
                <tbody>
                  {data.items.map((link) => (
                    <LinkRow
                      key={link.id}
                      link={link}
                      onEdit={() => setEditing(link)}
                      onDelete={() => setDeleting(link)}
                    />
                  ))}
                </tbody>
              </table>
            </div>
            {pages > 1 && (
              <nav className="pager" aria-label="Pagination">
                <button type="button" className="btn btn-ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>
                  Previous
                </button>
                <span className="muted">Page {page + 1} of {pages}</span>
                <button
                  type="button"
                  className="btn btn-ghost"
                  disabled={page + 1 >= pages}
                  onClick={() => setPage(page + 1)}
                >
                  Next
                </button>
              </nav>
            )}
          </>
        )}
      </section>

      {editing && (
        <EditLinkDialog
          link={editing}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null);
            load(page);
          }}
        />
      )}
      {deleting && (
        <DeleteLinkDialog
          link={deleting}
          onClose={() => setDeleting(null)}
          onDeleted={() => {
            setDeleting(null);
            if (justCreated?.id === deleting.id) setJustCreated(null);
            load(page);
          }}
        />
      )}
    </div>
  );
}

function LinkRow({ link, onEdit, onDelete }: { link: Link; onEdit: () => void; onDelete: () => void }) {
  const url = api.shortUrl(link.shortCode);
  const expired = isExpired(link.expiry);
  return (
    <tr>
      <td data-label="Short link">
        <div className="cell-link">
          <a href={url} target="_blank" rel="noreferrer" className="short-link">{link.shortCode}</a>
          <CopyButton text={url} compact />
        </div>
      </td>
      <td data-label="Destination" className="destination">
        <a href={link.longUrl} target="_blank" rel="noreferrer" title={link.longUrl}>
          {displayUrl(link.longUrl)}
        </a>
      </td>
      <td data-label="Clicks" className="num">{formatCount(link.clickCount)}</td>
      <td data-label="Status">
        {expired ? (
          <span className="badge badge-expired">Expired</span>
        ) : link.expiry ? (
          <span className="badge" title={formatDateTime(link.expiry)}>Expires {formatDateTime(link.expiry)}</span>
        ) : (
          <span className="badge badge-active">Active</span>
        )}
      </td>
      <td className="actions">
        <button type="button" className="btn btn-ghost btn-sm" onClick={onEdit} aria-label={`Edit ${link.shortCode}`}>
          Edit
        </button>
        <button
          type="button"
          className="btn btn-ghost btn-sm btn-danger"
          onClick={onDelete}
          aria-label={`Delete ${link.shortCode}`}
        >
          Delete
        </button>
      </td>
    </tr>
  );
}

function CreateLinkForm({ onCreated }: { onCreated: (link: Link) => void }) {
  const [url, setUrl] = useState('');
  const [expiry, setExpiry] = useState('');
  const [showExpiry, setShowExpiry] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    const normalized = normalizeUrl(url);
    if (!normalized.ok) {
      setError(normalized.error);
      return;
    }
    setBusy(true);
    try {
      const link = await api.createLink(normalized.url, showExpiry ? toApiInstant(expiry) : null);
      setUrl('');
      setExpiry('');
      setShowExpiry(false);
      onCreated(link);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not create the link.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="create-form" onSubmit={onSubmit} noValidate>
      <div className="create-row">
        <label className="field grow">
          <span className="sr-only">Long URL</span>
          <input
            type="text"
            inputMode="url"
            placeholder="Paste a long URL"
            value={url}
            onChange={(e) => setUrl(e.target.value)}
            aria-invalid={error ? true : undefined}
          />
        </label>
        <button type="submit" className="btn btn-primary" disabled={busy}>
          {busy ? 'Shortening…' : 'Shorten'}
        </button>
      </div>
      <div className="expiry-row">
        <label className="check">
          <input type="checkbox" checked={showExpiry} onChange={(e) => setShowExpiry(e.target.checked)} />
          <span>Expire this link</span>
        </label>
        {showExpiry && (
          <>
            <label className="field inline">
              <span className="sr-only">Expires at</span>
              <input type="datetime-local" value={expiry} onChange={(e) => setExpiry(e.target.value)} required />
            </label>
            <span className="muted tz-hint">{userTimeZone()}</span>
          </>
        )}
      </div>
      {error && <p className="alert alert-error" role="alert">{error}</p>}
    </form>
  );
}

function EditLinkDialog({ link, onClose, onSaved }: { link: Link; onClose: () => void; onSaved: () => void }) {
  const [url, setUrl] = useState(link.longUrl);
  const [expiry, setExpiry] = useState(toDateTimeInput(link.expiry));
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    const normalized = normalizeUrl(url);
    if (!normalized.ok) {
      setError(normalized.error);
      return;
    }
    setBusy(true);
    try {
      // The API leaves the expiry unchanged when it is omitted, so it can't be cleared here
      await api.updateLink(link.id, normalized.url, toApiInstant(expiry));
      onSaved();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not save the link.');
      setBusy(false);
    }
  }

  return (
    <Dialog title={`Edit ${link.shortCode}`} onClose={onClose}>
      <form onSubmit={onSubmit} noValidate className="dialog-form">
        <label className="field">
          <span>Destination URL</span>
          <input type="text" inputMode="url" value={url} onChange={(e) => setUrl(e.target.value)} autoFocus />
        </label>
        <label className="field">
          <span>Expires at <span className="muted">(optional, {userTimeZone()})</span></span>
          <input type="datetime-local" value={expiry} onChange={(e) => setExpiry(e.target.value)} />
        </label>
        {error && <p className="alert alert-error" role="alert">{error}</p>}
        <div className="dialog-actions">
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Save'}</button>
        </div>
      </form>
    </Dialog>
  );
}

function DeleteLinkDialog({ link, onClose, onDeleted }: { link: Link; onClose: () => void; onDeleted: () => void }) {
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onConfirm() {
    setBusy(true);
    try {
      await api.deleteLink(link.id);
      onDeleted();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not delete the link.');
      setBusy(false);
    }
  }

  return (
    <Dialog title="Delete this link?" onClose={onClose}>
      <p>
        <strong>{link.shortCode}</strong> will stop working and its click history will be deleted. This can't be undone.
      </p>
      {error && <p className="alert alert-error" role="alert">{error}</p>}
      <div className="dialog-actions">
        <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
        <button type="button" className="btn btn-danger-solid" onClick={onConfirm} disabled={busy}>
          {busy ? 'Deleting…' : 'Delete link'}
        </button>
      </div>
    </Dialog>
  );
}
