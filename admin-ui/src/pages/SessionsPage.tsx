import { useState } from 'react';
import { api } from '../api';
import { Alerts, Field, Loading, confirmed, formatDateTime, fromInputDateTime, toInputDateTime, useAction, useLoad } from '../components/ui';
import type { Hall, Session, Submission } from '../types';

/** The agenda of the current branch. */
export default function SessionsPage() {
  const sessions = useLoad(() => api.get<Session[]>('/api/admin/sessions'), []);
  const halls = useLoad(() => api.get<Hall[]>('/api/admin/halls'), []);
  const unscheduled = useLoad(() => api.get<Submission[]>('/api/admin/sessions/unscheduled-submissions'), []);
  const [editing, setEditing] = useState<Session>();
  const action = useAction();

  const save = async () => {
    if (!editing) return;
    const body = {
      title: editing.submissionId ? null : editing.title,
      startTime: editing.startTime,
      endTime: editing.endTime,
      submissionId: editing.submissionId ?? null,
      hallId: editing.hallId ?? null,
    };
    const ok = await action.run(() => editing.id ? api.put(`/api/admin/sessions/${editing.id}`, body) :
      api.post('/api/admin/sessions', body), 'Session saved');
    if (ok) {
      setEditing(undefined);
      sessions.reload();
      unscheduled.reload();
    }
  };

  const remove = async (s: Session) => {
    if (!confirmed(`Delete the slot "${s.submissionTitle ?? s.title}"?`)) return;
    await action.run(() => api.del(`/api/admin/sessions/${s.id}`), 'Session deleted');
    sessions.reload();
    unscheduled.reload();
  };

  const set = (patch: Partial<Session>) => editing && setEditing({ ...editing, ...patch });

  // the submission currently scheduled in the edited slot must stay selectable
  const talkOptions = [
    ...(editing?.submissionId && editing.submissionTitle ? [{ id: editing.submissionId, title: editing.submissionTitle }] : []),
    ...(unscheduled.data ?? []).map((s) => ({
      id: s.id, title: `${s.title} (${s.speaker?.firstName ?? ''} ${s.speaker?.lastName ?? ''})`,
    })),
  ];

  return (
    <>
      <div className="toolbar">
        <h2>Sessions (agenda)</h2>
        <span className="spacer" />
        <button onClick={() => setEditing({})}>Add slot</button>
      </div>
      <p className="muted">A slot is either a confirmed talk in a hall, or a titled slot without hall (e.g. "Break",
        "Lunch") that spans all halls. "TBA" can be placed in a hall.</p>
      <Alerts error={action.error} notice={action.notice} />
      {editing && (
        <div className="panel">
          <div className="form-grid">
            <Field label="Start" type="datetime-local" value={toInputDateTime(editing.startTime)} required
                   onChange={(v) => set({ startTime: fromInputDateTime(v) })} />
            <Field label="End" type="datetime-local" value={toInputDateTime(editing.endTime)} required
                   error={action.fieldErrors.endTime} onChange={(v) => set({ endTime: fromInputDateTime(v) })} />
            <label className="field wide">
              <span>Talk</span>
              <select value={editing.submissionId ?? ''} onChange={(e) => {
                const id = e.target.value ? Number(e.target.value) : undefined;
                set({ submissionId: id, submissionTitle: talkOptions.find((t) => t.id === id)?.title });
              }}>
                <option value="">— no talk (break / TBA) —</option>
                {talkOptions.map((t) => <option key={t.id} value={t.id}>{t.title}</option>)}
              </select>
            </label>
            {!editing.submissionId && (
              <Field label="Title (Break, Lunch, TBA, ...)" value={editing.title} error={action.fieldErrors.title}
                     onChange={(v) => set({ title: v })} />
            )}
            <label className="field">
              <span>Hall</span>
              <select value={editing.hallId ?? ''} onChange={(e) => set({ hallId: e.target.value ? Number(e.target.value) : undefined })}>
                <option value="">— all halls —</option>
                {halls.data?.map((h) => <option key={h.id} value={h.id}>{h.name}</option>)}
              </select>
              {action.fieldErrors.hallId && <small className="error">{action.fieldErrors.hallId}</small>}
            </label>
          </div>
          <div className="buttons">
            <button onClick={save} disabled={action.busy}>Save</button>
            <button className="secondary" onClick={() => setEditing(undefined)}>Cancel</button>
          </div>
        </div>
      )}
      <Loading loading={sessions.loading} error={sessions.error} />
      <table>
        <thead><tr><th>Start</th><th>End</th><th>Hall</th><th>Talk / title</th><th>Speakers</th><th /></tr></thead>
        <tbody>
        {sessions.data?.map((s) => (
          <tr key={s.id}>
            <td>{formatDateTime(s.startTime)}</td>
            <td>{formatDateTime(s.endTime).substring(11)}</td>
            <td>{s.hallName ?? <span className="muted">all</span>}</td>
            <td>{s.submissionTitle ?? <em>{s.title}</em>}</td>
            <td>{s.speakers}</td>
            <td className="actions">
              <button className="secondary" onClick={() => setEditing(s)}>Edit</button>
              <button className="danger" onClick={() => remove(s)}>Delete</button>
            </td>
          </tr>
        ))}
        </tbody>
      </table>
    </>
  );
}
