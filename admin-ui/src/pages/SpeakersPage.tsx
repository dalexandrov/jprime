import { Fragment, useState } from 'react';
import { Link } from 'react-router';
import { api, query } from '../api';
import {
  Alerts, Checkbox, Field, FileField, Loading, Pager, TextArea, YearSelect, confirmed, useAction, useLoad,
} from '../components/ui';
import type { Page, Speaker } from '../types';

export default function SpeakersPage() {
  const [year, setYear] = useState<number>();
  const [page, setPage] = useState(0);
  const [version, setVersion] = useState(0);
  const speakers = useLoad(() => api.get<Page<Speaker>>(`/api/admin/speakers${query({ year, page, size: 20 })}`),
    [year, page]);
  const [editing, setEditing] = useState<Speaker>();
  const [picture, setPicture] = useState<File>();
  const [resize, setResize] = useState(true);
  const [expanded, setExpanded] = useState<number>();
  const action = useAction();

  const save = async () => {
    if (!editing) return;
    const form = new FormData();
    (['firstName', 'lastName', 'email', 'phone', 'headline', 'twitter', 'bsky', 'bio'] as const).forEach((f) => {
      const value = editing[f];
      if (value !== undefined && value !== null) form.append(f, value);
    });
    form.append('resizeImage', String(resize));
    if (picture) form.append('picture', picture);
    const ok = await action.run(() => editing.id ? api.put(`/api/admin/speakers/${editing.id}`, form) :
      api.post('/api/admin/speakers', form), 'Speaker saved');
    if (ok) {
      setEditing(undefined);
      setPicture(undefined);
      setVersion(version + 1);
      speakers.reload();
    }
  };

  const remove = async (s: Speaker) => {
    if (!confirmed(`Delete ${s.firstName} ${s.lastName} AND all his/her submissions?`)) return;
    await action.run(() => api.del(`/api/admin/speakers/${s.id}`), 'Speaker deleted');
    speakers.reload();
  };

  const set = (patch: Partial<Speaker>) => editing && setEditing({ ...editing, ...patch });

  return (
    <>
      <div className="toolbar">
        <h2>Speakers</h2>
        <span>Year:</span>
        <YearSelect value={year} allowAll="All" onChange={(y) => { setYear(y); setPage(0); }} />
        <span className="spacer" />
        <button onClick={() => { setEditing({}); setPicture(undefined); }}>Add speaker</button>
      </div>
      <Alerts error={action.error} notice={action.notice} />
      {editing && (
        <div className="panel">
          <div className="form-grid">
            <Field label="First name" value={editing.firstName} onChange={(v) => set({ firstName: v })} />
            <Field label="Last name" value={editing.lastName} onChange={(v) => set({ lastName: v })} />
            <Field label="E-mail" type="email" value={editing.email} onChange={(v) => set({ email: v })} required
                   error={action.fieldErrors.email} />
            <Field label="Phone" value={editing.phone} onChange={(v) => set({ phone: v })} />
            <Field label="Headline" value={editing.headline} onChange={(v) => set({ headline: v })} />
            <Field label="X handle" value={editing.twitter} onChange={(v) => set({ twitter: v })} />
            <Field label="Bluesky profile URL" value={editing.bsky} onChange={(v) => set({ bsky: v })} />
            <div className="wide"><TextArea label="Bio" value={editing.bio} onChange={(v) => set({ bio: v })} /></div>
            <FileField label={editing.id ? 'Replace picture' : 'Picture'} onChange={setPicture} />
            <Checkbox label="Resize to 280×326" checked={resize} onChange={setResize} />
            {editing.id && <img className="thumb" src={`/api/admin/speakers/${editing.id}/picture?v=${version}`} alt="" />}
          </div>
          <div className="buttons">
            <button onClick={save} disabled={action.busy}>Save</button>
            <button className="secondary" onClick={() => setEditing(undefined)}>Cancel</button>
          </div>
        </div>
      )}
      <Loading loading={speakers.loading} error={speakers.error} />
      <table>
        <thead>
        <tr>
          <th>Picture</th><th>Name</th><th>E-mail</th><th>X / Bluesky</th><th>Type</th>
          <th>Submissions{year ? ` ${year}` : ''}</th><th>Featured</th><th>Status</th><th>Last year</th><th />
        </tr>
        </thead>
        <tbody>
        {speakers.data?.content.map((s) => (
          <Fragment key={s.id}>
            <tr>
              <td><img className="thumb" src={`/api/admin/speakers/${s.id}/picture?v=${version}`} alt="" /></td>
              <td><button className="link" onClick={() => setExpanded(expanded === s.id ? undefined : s.id)}>
                {s.firstName} {s.lastName}</button></td>
              <td>{s.email}</td>
              <td>{s.twitter}{s.bsky && <> · <a href={s.bsky} target="_blank" rel="noreferrer">bsky</a></>}</td>
              <td>{s.speakerType}</td>
              <td>{s.numberOfSubmissions}</td>
              <td>{s.featured ? 'yes' : ''}</td>
              <td>{s.status}</td>
              <td>{s.lastBranchYear}</td>
              <td className="actions">
                <button className="secondary" onClick={() => { setEditing(s); setPicture(undefined); }}>Edit</button>
                <button className="danger" onClick={() => remove(s)}>Delete</button>
              </td>
            </tr>
            {expanded === s.id && (
              <tr>
                <td colSpan={10}>
                  <p>{s.bio}</p>
                  <ul>
                    {s.submissions?.map((sub) => (
                      <li key={sub.id}>
                        {sub.branchYear}: <Link to={`/submissions?id=${sub.id}`}>{sub.title}</Link> ({sub.status}
                        {sub.featured ? ', featured' : ''})
                      </li>
                    ))}
                  </ul>
                </td>
              </tr>
            )}
          </Fragment>
        ))}
        </tbody>
      </table>
      {speakers.data && <Pager page={speakers.data.page} totalPages={speakers.data.totalPages} onPage={setPage} />}
    </>
  );
}
