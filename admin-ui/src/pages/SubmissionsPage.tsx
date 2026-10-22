import { Fragment, useState } from 'react';
import { useSearchParams } from 'react-router';
import { api, query } from '../api';
import {
  Alerts, Checkbox, Field, Loading, Pager, Select, TextArea, YearSelect, confirmed, formatDateTime, useAction,
  useLoad, useLookups, useOptions,
} from '../components/ui';
import type { Message, Page, SpeakerFields, Submission, SubmissionInput, SubmissionStatus } from '../types';

interface Editing extends SubmissionInput {
  id?: number;
  withCoSpeaker: boolean;
}

function SpeakerForm({ title, value, onChange, errors, prefix }: {
  title: string; value: SpeakerFields; onChange: (v: SpeakerFields) => void; errors: Record<string, string>;
  prefix: string;
}) {
  const set = (patch: Partial<SpeakerFields>) => onChange({ ...value, ...patch });
  return (
    <>
      <h4 className="wide">{title}</h4>
      <Field label="E-mail" type="email" value={value.email} onChange={(v) => set({ email: v })} required
             error={errors[`${prefix}.email`]}
             hint="An existing speaker is found by e-mail; only the fields you fill in are updated." />
      <Field label="First name" value={value.firstName} onChange={(v) => set({ firstName: v })}
             error={errors[`${prefix}.firstName`]} />
      <Field label="Last name" value={value.lastName} onChange={(v) => set({ lastName: v })}
             error={errors[`${prefix}.lastName`]} />
      <Field label="Headline" value={value.headline} onChange={(v) => set({ headline: v })} />
      <Field label="X handle" value={value.twitter} onChange={(v) => set({ twitter: v })} />
      <Field label="Bluesky" value={value.bsky} onChange={(v) => set({ bsky: v })} />
      <div className="wide">
        <TextArea label="Bio" value={value.bio} onChange={(v) => set({ bio: v })} error={errors[`${prefix}.bio`]} />
      </div>
    </>
  );
}

export default function SubmissionsPage() {
  const { currentYear } = useLookups();
  const [params, setParams] = useSearchParams();
  const singleId = params.get('id');
  const [year, setYear] = useState<number | undefined>(currentYear);
  const [status, setStatus] = useState<SubmissionStatus>();
  const [all, setAll] = useState(false);
  const [page, setPage] = useState(0);
  const [expanded, setExpanded] = useState<number>();
  const [editing, setEditing] = useState<Editing>();
  const action = useAction();
  const levels = useOptions('sessionLevel');
  const types = useOptions('sessionType');
  const statuses = useOptions('submissionStatus');

  const list = useLoad(async (): Promise<Page<Submission>> => {
    if (singleId) {
      const one = await api.get<Submission>(`/api/admin/submissions/${singleId}`);
      return { content: [one], page: 0, size: 1, totalElements: 1, totalPages: 1 };
    }
    return api.get<Page<Submission>>(`/api/admin/submissions${query({ year, status, all, page, size: 20 })}`);
  }, [singleId, year, status, all, page]);
  const stats = useLoad(() => api.get<Partial<Record<SubmissionStatus, number>>>(
    `/api/admin/submissions/stats${query({ year })}`), [year]);

  const reload = () => {
    list.reload();
    stats.reload();
  };

  const act = async (s: Submission, verb: string) => {
    if (verb === 'delete') {
      if (!confirmed(`Delete "${s.title}"? Only SUBMITTED proposals can be deleted.`)) return;
      await action.run(() => api.del(`/api/admin/submissions/${s.id}`), 'Submission deleted');
    } else {
      await action.run(() => api.post<Message | Submission>(`/api/admin/submissions/${s.id}/${verb}`),
        (result) => ('message' in result ? result.message : `Status: ${(result as Submission).status}`));
    }
    reload();
  };

  const edit = (s?: Submission) => {
    if (!s) {
      setEditing({ title: '', description: '', level: 'BEGINNER', type: 'CONFERENCE_SESSION', branchYear: currentYear,
        featured: false, speaker: {}, coSpeaker: null, withCoSpeaker: false });
      return;
    }
    setEditing({
      id: s.id, title: s.title, description: s.description, level: s.level, type: s.type, branchYear: s.branchYear,
      featured: s.featured,
      speaker: { email: s.speaker?.email, firstName: s.speaker?.firstName, lastName: s.speaker?.lastName },
      coSpeaker: s.coSpeaker ? { email: s.coSpeaker.email, firstName: s.coSpeaker.firstName, lastName: s.coSpeaker.lastName } : null,
      withCoSpeaker: !!s.coSpeaker,
    });
  };

  const save = async () => {
    if (!editing) return;
    const { id, withCoSpeaker, ...body } = editing;
    const payload = { ...body, coSpeaker: withCoSpeaker ? body.coSpeaker : null };
    const ok = await action.run(() => id ? api.put(`/api/admin/submissions/${id}`, payload) :
      api.post('/api/admin/submissions', payload), 'Submission saved');
    if (ok) {
      setEditing(undefined);
      reload();
    }
  };

  const set = (patch: Partial<Editing>) => editing && setEditing({ ...editing, ...patch });

  return (
    <>
      <div className="toolbar">
        <h2>Submissions</h2>
        {singleId ? (
          <button className="secondary" onClick={() => setParams({})}>Show all</button>
        ) : (
          <>
            <span>Year:</span>
            <YearSelect value={year} onChange={(y) => { setYear(y); setPage(0); }} />
            <select value={status ?? ''} onChange={(e) => { setStatus((e.target.value || undefined) as SubmissionStatus); setPage(0); }}>
              <option value="">All statuses</option>
              {statuses.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
            <Checkbox label="All years" checked={all} onChange={(v) => { setAll(v); setPage(0); }} />
          </>
        )}
        <span className="spacer" />
        <button className="secondary" onClick={() => api.download('/api/admin/submissions/export', 'submissions.csv')}>
          Export SUBMITTED to CSV
        </button>
        <button onClick={() => edit()}>Add submission</button>
      </div>
      <div className="stats">
        {stats.data && Object.entries(stats.data).map(([s, count]) => (
          <button key={s} className="secondary" onClick={() => { setStatus(s as SubmissionStatus); setPage(0); }}>
            {s}: {count}
          </button>
        ))}
      </div>
      <Alerts error={action.error} notice={action.notice} />

      {editing && (
        <div className="panel">
          <h3>{editing.id ? 'Edit submission' : 'New submission'}</h3>
          <div className="form-grid">
            <div className="wide">
              <Field label="Title" value={editing.title} onChange={(v) => set({ title: v })} required
                     error={action.fieldErrors.title} />
            </div>
            <div className="wide">
              <TextArea label="Abstract" rows={8} value={editing.description} onChange={(v) => set({ description: v })}
                        error={action.fieldErrors.description} />
            </div>
            <Select label="Level" value={editing.level} options={levels} onChange={(v) => set({ level: v })} />
            <Select label="Type" value={editing.type} options={types} onChange={(v) => set({ type: v })} />
            <label className="field">
              <span>Branch</span>
              <YearSelect value={editing.branchYear} onChange={(y) => set({ branchYear: y })} />
            </label>
            <Checkbox label="Featured (shown on the home page)" checked={!!editing.featured}
                      onChange={(v) => set({ featured: v })} />
            <SpeakerForm title="Speaker" prefix="speaker" value={editing.speaker} errors={action.fieldErrors}
                         onChange={(v) => set({ speaker: v })} />
            <div className="wide">
              <Checkbox label="With co-speaker" checked={editing.withCoSpeaker}
                        onChange={(v) => set({ withCoSpeaker: v, coSpeaker: v ? editing.coSpeaker ?? {} : null })} />
            </div>
            {editing.withCoSpeaker && (
              <SpeakerForm title="Co-speaker" prefix="coSpeaker" value={editing.coSpeaker ?? {}}
                           errors={action.fieldErrors} onChange={(v) => set({ coSpeaker: v })} />
            )}
          </div>
          <div className="buttons">
            <button onClick={save} disabled={action.busy}>Save</button>
            <button className="secondary" onClick={() => setEditing(undefined)}>Cancel</button>
          </div>
        </div>
      )}

      <Loading loading={list.loading} error={list.error} />
      <table>
        <thead>
        <tr><th>Title</th><th>Level</th><th>Type</th><th>Speaker</th><th>Co-speaker</th><th>Year</th><th>Status</th><th>Submitted</th><th /></tr>
        </thead>
        <tbody>
        {list.data?.content.map((s) => (
          <Fragment key={s.id}>
            <tr>
              <td>
                <button className="link" onClick={() => setExpanded(expanded === s.id ? undefined : s.id)}>{s.title}</button>
                {s.featured && <> <span className="badge">featured</span></>}
              </td>
              <td>{s.level}</td>
              <td>{s.type}</td>
              <td>{s.speaker?.firstName} {s.speaker?.lastName}</td>
              <td>{s.coSpeaker && `${s.coSpeaker.firstName ?? ''} ${s.coSpeaker.lastName ?? ''}`}</td>
              <td>{s.branchYear}</td>
              <td><span className="badge">{s.status}</span></td>
              <td>{formatDateTime(s.createdDate)}</td>
              <td className="actions">
                <button className="secondary" onClick={() => act(s, 'accept')} title="Accept and e-mail the speakers">Accept</button>
                <button className="secondary" onClick={() => act(s, 'confirm')}>Confirm</button>
                <button className="secondary" onClick={() => act(s, 'reject')} title="Reject and e-mail the speakers">Reject</button>
                <button className="secondary" onClick={() => act(s, 'cancel')}>Cancel</button>
                <button className="secondary" onClick={() => act(s, 'notify')} title="Re-send the 'thank you' e-mails">Notify</button>
                <button className="secondary" onClick={() => edit(s)}>Edit</button>
                {s.status === 'SUBMITTED' && <button className="danger" onClick={() => act(s, 'delete')}>Delete</button>}
              </td>
            </tr>
            {expanded === s.id && (
              <tr><td colSpan={9} style={{ whiteSpace: 'pre-wrap' }}>{s.description}</td></tr>
            )}
          </Fragment>
        ))}
        </tbody>
      </table>
      {list.data && <Pager page={list.data.page} totalPages={list.data.totalPages} onPage={setPage} />}
    </>
  );
}
