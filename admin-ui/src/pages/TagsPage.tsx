import { useState } from 'react';
import { api } from '../api';
import { Alerts, Field, Loading, confirmed, useAction, useLoad } from '../components/ui';
import type { Tag } from '../types';

/** Tags appear in the site menu; a tag with a single article (e.g. "Agenda") shows that article directly. */
export default function TagsPage() {
  const tags = useLoad(() => api.get<Tag[]>('/api/admin/tags'), []);
  const [editing, setEditing] = useState<Tag>();
  const action = useAction();

  const save = async () => {
    if (!editing) return;
    const ok = await action.run(() => editing.id ? api.put(`/api/admin/tags/${editing.id}`, editing) :
      api.post('/api/admin/tags', editing), 'Tag saved');
    if (ok) {
      setEditing(undefined);
      tags.reload();
    }
  };

  const remove = async (tag: Tag) => {
    if (!confirmed(`Delete tag "${tag.name}"?`)) return;
    await action.run(() => api.del(`/api/admin/tags/${tag.id}`), 'Tag deleted');
    tags.reload();
  };

  return (
    <>
      <div className="toolbar">
        <h2>Tags</h2>
        <span className="spacer" />
        <button onClick={() => setEditing({ name: '' })}>Add tag</button>
      </div>
      <p className="muted">Tags are shown in the site menu (tags named after a year, e.g. "2019", are archives and are hidden).</p>
      <Alerts error={action.error} notice={action.notice} />
      {editing && (
        <div className="panel">
          <div className="form-grid">
            <Field label="Name" value={editing.name} onChange={(v) => setEditing({ ...editing, name: v })} required />
          </div>
          <div className="buttons">
            <button onClick={save} disabled={action.busy}>Save</button>
            <button className="secondary" onClick={() => setEditing(undefined)}>Cancel</button>
          </div>
        </div>
      )}
      <Loading loading={tags.loading} error={tags.error} />
      <table>
        <thead><tr><th>Name</th><th /></tr></thead>
        <tbody>
        {tags.data?.map((t) => (
          <tr key={t.id}>
            <td>{t.name}</td>
            <td className="actions">
              <button className="secondary" onClick={() => setEditing(t)}>Edit</button>
              <button className="danger" onClick={() => remove(t)}>Delete</button>
            </td>
          </tr>
        ))}
        </tbody>
      </table>
    </>
  );
}
