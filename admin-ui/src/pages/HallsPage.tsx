import { useState } from 'react';
import { api } from '../api';
import { Alerts, Field, Loading, confirmed, useAction, useLoad } from '../components/ui';
import type { Hall } from '../types';

export default function HallsPage() {
  const halls = useLoad(() => api.get<Hall[]>('/api/admin/halls'), []);
  const [editing, setEditing] = useState<Hall>();
  const action = useAction();

  const save = async () => {
    if (!editing) return;
    const ok = await action.run(() => editing.id ? api.put(`/api/admin/halls/${editing.id}`, editing) :
      api.post('/api/admin/halls', editing), 'Hall saved');
    if (ok) {
      setEditing(undefined);
      halls.reload();
    }
  };

  const remove = async (hall: Hall) => {
    if (!confirmed(`Delete hall "${hall.name}"?`)) return;
    await action.run(() => api.del(`/api/admin/halls/${hall.id}`), 'Hall deleted');
    halls.reload();
  };

  return (
    <>
      <div className="toolbar">
        <h2>Halls</h2>
        <span className="spacer" />
        <button onClick={() => setEditing({ name: '' })}>Add hall</button>
      </div>
      <p className="muted">The agenda columns are the halls named in jprime.agenda.halls (site configuration), by default "Hall A", "Hall B" and "Workshops".</p>
      <Alerts error={action.error} notice={action.notice} />
      {editing && (
        <div className="panel">
          <div className="form-grid">
            <Field label="Name" value={editing.name} onChange={(v) => setEditing({ ...editing, name: v })} required />
            <Field label="Description" value={editing.description} onChange={(v) => setEditing({ ...editing, description: v })} />
          </div>
          <div className="buttons">
            <button onClick={save} disabled={action.busy}>Save</button>
            <button className="secondary" onClick={() => setEditing(undefined)}>Cancel</button>
          </div>
        </div>
      )}
      <Loading loading={halls.loading} error={halls.error} />
      <table>
        <thead><tr><th>Name</th><th>Description</th><th /></tr></thead>
        <tbody>
        {halls.data?.map((h) => (
          <tr key={h.id}>
            <td>{h.name}</td>
            <td>{h.description}</td>
            <td className="actions">
              <button className="secondary" onClick={() => setEditing(h)}>Edit</button>
              <button className="danger" onClick={() => remove(h)}>Delete</button>
            </td>
          </tr>
        ))}
        </tbody>
      </table>
    </>
  );
}
