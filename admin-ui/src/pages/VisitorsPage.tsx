import { useMemo, useState } from 'react';
import { api, query } from '../api';
import {
  Alerts, Checkbox, Field, FileField, Loading, Select, TextArea, YearSelect, confirmed, useAction, useLoad,
  useLookups, useOptions,
} from '../components/ui';
import type { SendResult, Visitor, VisitorList } from '../types';

function sendResultText(r: SendResult): string {
  return r.jobId ? 'Started a background job - see "Background jobs"' :
    `${r.sent} ticket(s) sent${r.failed ? `, ${r.failed} failed` : ''}`;
}

type Panel = 'edit' | 'upload' | 'mail' | undefined;

export default function VisitorsPage() {
  const { currentYear } = useLookups();
  const [year, setYear] = useState<number | undefined>(currentYear);
  const [filter, setFilter] = useState('');
  const [panel, setPanel] = useState<Panel>();
  const [editing, setEditing] = useState<Visitor>({ name: '' });
  const [upload, setUpload] = useState<{ file?: File; visitorType: string; visitorStatus: string; emptyBeforeUpload: boolean }>(
    { visitorType: 'JPRIME', visitorStatus: 'PAYED', emptyBeforeUpload: false });
  const [mail, setMail] = useState({ subject: '', content: '' });
  const list = useLoad(() => api.get<VisitorList>(`/api/admin/visitors${query({ year })}`), [year]);
  const action = useAction();
  const statuses = useOptions('visitorStatus');
  const visitorTypes = useOptions('visitorType');

  const visible = useMemo(() => {
    const text = filter.trim().toLowerCase();
    const all = list.data?.visitors ?? [];
    if (!text) return all;
    return all.filter((v) => [v.name, v.email, v.company, v.registrantName, v.ticket]
      .some((value) => value?.toLowerCase().includes(text)));
  }, [list.data, filter]);

  const save = async () => {
    const body = { name: editing.name, email: editing.email, company: editing.company, status: editing.status,
      registrantId: editing.registrantId };
    const ok = await action.run(() => editing.id ? api.put(`/api/admin/visitors/${editing.id}`, body) :
      api.post('/api/admin/visitors', body), 'Visitor saved');
    if (ok) {
      setPanel(undefined);
      list.reload();
    }
  };

  const remove = async (v: Visitor) => {
    if (!confirmed(`Delete visitor ${v.name}?`)) return;
    await action.run(() => api.del(`/api/admin/visitors/${v.id}`), 'Visitor deleted');
    list.reload();
  };

  const sendTicket = async (v: Visitor) => {
    await action.run(() => api.post<SendResult>(`/api/admin/visitors/${v.id}/send-ticket`), sendResultText);
    list.reload();
  };

  const sendAll = async () => {
    if (!confirmed('Send tickets to ALL paid / sponsored visitors of the current branch that have no ticket yet?')) return;
    await action.run(() => api.post<SendResult>('/api/admin/visitors/send-tickets'), sendResultText);
    list.reload();
  };

  const doUpload = async () => {
    if (!upload.file) return;
    const form = new FormData();
    form.append('file', upload.file);
    form.append('visitorType', upload.visitorType);
    form.append('visitorStatus', upload.visitorStatus);
    form.append('emptyBeforeUpload', String(upload.emptyBeforeUpload));
    const result = await action.run(() => api.post<{ imported: number; errors: string[] }>('/api/admin/visitors/upload', form),
      (r) => `${r.imported} visitor(s) imported${r.errors.length ? `. Errors: ${r.errors.join('; ')}` : ''}`);
    if (result) list.reload();
  };

  const sendMail = async () => {
    if (!confirmed('Send this e-mail to ALL visitors of the current branch?')) return;
    const ok = await action.run(() => api.post('/api/admin/visitors/mail', mail), 'The e-mails are being sent by a background job');
    if (ok) setPanel(undefined);
  };

  const stats = list.data?.stats;

  return (
    <>
      <div className="toolbar">
        <h2>Visitors</h2>
        <span>Year:</span>
        <YearSelect value={year} onChange={setYear} />
        <input placeholder="Filter…" value={filter} onChange={(e) => setFilter(e.target.value)} />
        <span className="spacer" />
        <button onClick={() => { setEditing({ name: '', status: 'REQUESTING' }); setPanel('edit'); }}>Add</button>
        <button className="secondary" onClick={() => action.run(() => api.download(`/api/admin/visitors/export${query({ year })}`, 'visitors.csv'))}>Export</button>
        <button className="secondary" onClick={() => setPanel('upload')}>Upload CSV</button>
        <button className="secondary" onClick={() => setPanel('mail')}>E-mail all</button>
        <button className="secondary" onClick={sendAll}>Send tickets</button>
      </div>
      {stats && (
        <div className="stats">
          <span className="badge">Total: {stats.total}</span>
          <span className="badge">Requesting: {stats.requesting}</span>
          <span className="badge">Payed: {stats.payed}</span>
          <span className="badge">Sponsored: {stats.sponsored}</span>
          <span className="badge">Shown: {visible.length}</span>
        </div>
      )}
      <Alerts error={action.error} notice={action.notice} />

      {panel === 'edit' && (
        <div className="panel">
          <h3>{editing.id ? 'Edit visitor' : 'New visitor'}</h3>
          <p className="muted">A new visitor without registrant gets its own (individual) registrant.</p>
          <div className="form-grid">
            <Field label="Name" value={editing.name} onChange={(v) => setEditing({ ...editing, name: v })} required />
            <Field label="E-mail" value={editing.email} onChange={(v) => setEditing({ ...editing, email: v })} />
            <Field label="Company" value={editing.company} onChange={(v) => setEditing({ ...editing, company: v })} />
            <Select label="Status" value={editing.status} options={statuses}
                    onChange={(v) => setEditing({ ...editing, status: v as Visitor['status'] })} />
          </div>
          <div className="buttons">
            <button onClick={save} disabled={action.busy}>Save</button>
            <button className="secondary" onClick={() => setPanel(undefined)}>Cancel</button>
          </div>
        </div>
      )}

      {panel === 'upload' && (
        <div className="panel">
          <h3>Upload visitors from CSV</h3>
          <p className="muted">jPrime columns: Names, Email, Company, Is Company, Registrant, Address, VAT Number,
            Material Responsible Person, Company E-Mail. jProfessionals: the Eventbrite attendee export (First Name,
            Last Name, Email Address). The first line must be the header.</p>
          <div className="form-grid">
            <FileField label="CSV file" accept=".csv,text/csv" onChange={(f) => setUpload({ ...upload, file: f })} />
            <Select label="Type" value={upload.visitorType} options={visitorTypes}
                    onChange={(v) => setUpload({ ...upload, visitorType: v })} />
            <Select label="Status of the imported visitors" value={upload.visitorStatus} options={statuses}
                    onChange={(v) => setUpload({ ...upload, visitorStatus: v })} />
            <Checkbox label="Delete all jProfessionals visitors first" checked={upload.emptyBeforeUpload}
                      onChange={(v) => setUpload({ ...upload, emptyBeforeUpload: v })} />
          </div>
          <div className="buttons">
            <button onClick={doUpload} disabled={action.busy || !upload.file}>Upload</button>
            <button className="secondary" onClick={() => setPanel(undefined)}>Close</button>
          </div>
        </div>
      )}

      {panel === 'mail' && (
        <div className="panel">
          <h3>E-mail to all visitors of the current branch</h3>
          <div className="form-grid">
            <div className="wide"><Field label="Subject" value={mail.subject} onChange={(v) => setMail({ ...mail, subject: v })} /></div>
            <div className="wide"><TextArea label="Content (HTML)" rows={10} value={mail.content}
                                            onChange={(v) => setMail({ ...mail, content: v })} /></div>
          </div>
          <div className="buttons">
            <button onClick={sendMail} disabled={action.busy}>Send</button>
            <button className="secondary" onClick={() => setPanel(undefined)}>Cancel</button>
          </div>
        </div>
      )}

      <Loading loading={list.loading} error={list.error} />
      <table>
        <thead>
        <tr><th>ID</th><th>Name</th><th>E-mail</th><th>Company</th><th>Status</th><th>Registrant</th><th>Invoice #</th>
          <th>Ticket</th><th>Present</th><th>Checked in</th><th /></tr>
        </thead>
        <tbody>
        {visible.map((v) => (
          <tr key={v.id}>
            <td>{v.id}</td>
            <td>{v.name}</td>
            <td>{v.email}</td>
            <td>{v.company}</td>
            <td>{v.status}</td>
            <td>{v.registrantName}</td>
            <td>{v.registrantInvoiceNumber || ''}</td>
            <td><small>{v.ticket}</small></td>
            <td>{v.present ? 'yes' : ''}</td>
            <td>{v.registered ? 'yes' : ''}</td>
            <td className="actions">
              <button className="secondary" onClick={() => { setEditing(v); setPanel('edit'); }}>Edit</button>
              {v.status !== 'REQUESTING' && <button className="secondary" onClick={() => sendTicket(v)}>Ticket</button>}
              <button className="danger" onClick={() => remove(v)}>Delete</button>
            </td>
          </tr>
        ))}
        </tbody>
      </table>
    </>
  );
}
