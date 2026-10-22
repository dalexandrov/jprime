import { useState } from 'react';
import { Link } from 'react-router';
import { api, query } from '../api';
import {
  Alerts, Checkbox, Field, Loading, Pager, Select, YearSelect, confirmed, formatDateTime, useAction, useLoad,
  useLookups, useOptions,
} from '../components/ui';
import type { Invoice, Page, Registrant, SendResult, Visitor } from '../types';

function sendResultText(r: SendResult): string {
  return r.jobId ? 'Tickets are being sent by a background job - see "Background jobs"' :
    `${r.sent} ticket(s) sent${r.failed ? `, ${r.failed} failed` : ''}`;
}

function InvoicePanel({ registrant, onDone }: { registrant: Registrant; onDone: () => void }) {
  const invoice = useLoad(() => api.get<Invoice>(`/api/admin/registrants/${registrant.id}/invoice`), [registrant.id]);
  const [data, setData] = useState<Invoice>();
  const action = useAction();
  const current = data ?? invoice.data;
  if (!current) return <Loading loading={invoice.loading} error={invoice.error} />;
  const set = (patch: Partial<Invoice>) => setData({ ...current, ...patch });
  const body = {
    client: current.client, clientAddress: current.clientAddress, clientEIK: current.clientEIK,
    clientVAT: current.clientVAT, mol: current.mol, invoiceDate: current.invoiceDate,
    singlePriceWithVAT: current.singlePriceWithVAT, description: current.description,
  };
  const send = async () => {
    if (!confirmed(`Mark all ${current.passQty} visitor(s) as PAID and e-mail the original invoice to ${registrant.email}?`)) return;
    const result = await action.run(() => api.post<{ message: string }>(`/api/admin/registrants/${registrant.id}/invoice/send`, body),
      (r) => r.message);
    if (result) onDone();
  };
  return (
    <div className="panel">
      <h3>Original invoice</h3>
      <Alerts error={action.error} notice={action.notice} />
      <div className="form-grid">
        <Field label="Client" value={current.client} onChange={(v) => set({ client: v })} />
        <Field label="Address" value={current.clientAddress} onChange={(v) => set({ clientAddress: v })} />
        <Field label="EIK" value={current.clientEIK} onChange={(v) => set({ clientEIK: v })} />
        <Field label="VAT number" value={current.clientVAT} onChange={(v) => set({ clientVAT: v })} />
        <Field label="MOL" value={current.mol} onChange={(v) => set({ mol: v })} />
        <Field label="Invoice date (dd.MM.yyyy)" value={current.invoiceDate} onChange={(v) => set({ invoiceDate: v })} />
        <Field label="Single price with VAT" type="number" value={current.singlePriceWithVAT}
               onChange={(v) => set({ singlePriceWithVAT: Number(v) })} />
        <Field label="Description" value={current.description} onChange={(v) => set({ description: v })} />
      </div>
      <p>
        {current.passQty} pass(es) · total {current.totalPrice} + VAT {current.totalPriceVAT} = <strong>{current.totalPriceWithVAT}</strong>
        {current.invoiceNumber !== '0' && <> · invoice number {current.invoiceNumber}</>}
      </p>
      <div className="buttons">
        <button className="secondary" onClick={() => action.run(() => api.download(
          `/api/admin/registrants/${registrant.id}/invoice/preview`, 'invoice-preview.pdf', 'POST', body))}>
          Preview PDF
        </button>
        <button onClick={send} disabled={action.busy}>Mark paid &amp; send invoice</button>
      </div>
    </div>
  );
}

function RegistrantDetail({ id, onChanged }: { id: number; onChanged: () => void }) {
  const registrant = useLoad(() => api.get<Registrant>(`/api/admin/registrants/${id}`), [id]);
  const [form, setForm] = useState<Registrant>();
  const [visitor, setVisitor] = useState<Visitor>();
  const [showInvoice, setShowInvoice] = useState(false);
  const action = useAction();
  const paymentTypes = useOptions('paymentType');
  const statuses = useOptions('visitorStatus');
  const current = form ?? registrant.data;
  if (!current) return <Loading loading={registrant.loading} error={registrant.error} />;
  const set = (patch: Partial<Registrant>) => setForm({ ...current, ...patch });

  const save = async () => {
    const ok = await action.run(() => api.put(`/api/admin/registrants/${id}`, current), 'Registrant saved');
    if (ok) {
      setForm(undefined);
      registrant.reload();
      onChanged();
    }
  };
  const addVisitor = async () => {
    if (!visitor) return;
    const ok = await action.run(() => api.post(`/api/admin/registrants/${id}/visitors`, visitor), 'Visitor added');
    if (ok) {
      setVisitor(undefined);
      registrant.reload();
      onChanged();
    }
  };
  const sendTickets = () => action.run(() => api.post<SendResult>(`/api/admin/registrants/${id}/send-tickets`),
    sendResultText);

  return (
    <div className="panel">
      <h3>Registrant #{id}</h3>
      <Alerts error={action.error} notice={action.notice} />
      <div className="form-grid">
        <Field label="Name" value={current.name} onChange={(v) => set({ name: v })} required />
        <Field label="E-mail" value={current.email} onChange={(v) => set({ email: v })} />
        <Checkbox label="Company (VAT invoice)" checked={current.company} onChange={(v) => set({ company: v })} />
        <Checkbox label="Student" checked={current.student} onChange={(v) => set({ student: v })} />
        <Field label="Address" value={current.address} onChange={(v) => set({ address: v })} />
        <Field label="VAT number" value={current.vatNumber} onChange={(v) => set({ vatNumber: v })} />
        <Field label="EIK" value={current.eik} onChange={(v) => set({ eik: v })} />
        <Field label="MOL" value={current.mol} onChange={(v) => set({ mol: v })} />
        <Select label="Payment type" value={current.paymentType} options={paymentTypes} onChange={(v) => set({ paymentType: v })} />
        <label className="field"><span>Branch</span>
          <YearSelect value={current.branchYear} onChange={(y) => set({ branchYear: y })} />
        </label>
        <Field label="Real invoice number" type="number" value={current.realInvoiceNumber}
               onChange={(v) => set({ realInvoiceNumber: Number(v) })} />
        <Field label="Proforma invoice number" type="number" value={current.proformaInvoiceNumber}
               onChange={(v) => set({ proformaInvoiceNumber: Number(v) })} />
      </div>
      <div className="buttons">
        <button onClick={save} disabled={action.busy}>Save</button>
        <button className="secondary" onClick={sendTickets} disabled={action.busy}>Send tickets</button>
        {current.paymentType !== 'EPAY_ACCOUNT' && (
          <button className="secondary" onClick={() => setShowInvoice(!showInvoice)}>Invoice…</button>
        )}
      </div>
      {showInvoice && <InvoicePanel registrant={current} onDone={() => { registrant.reload(); onChanged(); }} />}

      <h4>Visitors</h4>
      <table>
        <thead><tr><th>Name</th><th>E-mail</th><th>Company</th><th>Status</th><th>Ticket</th></tr></thead>
        <tbody>
        {registrant.data?.visitors?.map((v) => (
          <tr key={v.id}><td>{v.name}</td><td>{v.email}</td><td>{v.company}</td><td>{v.status}</td><td>{v.ticket}</td></tr>
        ))}
        </tbody>
      </table>
      {visitor ? (
        <div className="form-grid">
          <Field label="Name" value={visitor.name} onChange={(v) => setVisitor({ ...visitor, name: v })} required />
          <Field label="E-mail" value={visitor.email} onChange={(v) => setVisitor({ ...visitor, email: v })} />
          <Field label="Company" value={visitor.company} onChange={(v) => setVisitor({ ...visitor, company: v })} />
          <Select label="Status" value={visitor.status} options={statuses}
                  onChange={(v) => setVisitor({ ...visitor, status: v as Visitor['status'] })} />
          <div className="buttons">
            <button onClick={addVisitor}>Add</button>
            <button className="secondary" onClick={() => setVisitor(undefined)}>Cancel</button>
          </div>
        </div>
      ) : (
        <p><button className="secondary" onClick={() => setVisitor({ name: '', status: 'REQUESTING' })}>Add visitor</button>
          {' '}<Link to="/visitors">Manage all visitors</Link></p>
      )}
    </div>
  );
}

export default function RegistrantsPage() {
  const { currentYear } = useLookups();
  const [year, setYear] = useState<number | undefined>(currentYear);
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<number>();
  const [creating, setCreating] = useState<Registrant>();
  const list = useLoad(() => api.get<Page<Registrant>>(`/api/admin/registrants${query({ year, page, size: 20 })}`), [year, page]);
  const action = useAction();

  const create = async () => {
    if (!creating) return;
    const created = await action.run(() => api.post<Registrant>('/api/admin/registrants', creating), 'Registrant created');
    if (created) {
      setCreating(undefined);
      setSelected(created.id);
      list.reload();
    }
  };

  const remove = async (r: Registrant) => {
    if (!confirmed(`Delete registrant "${r.name}" and all its ${r.visitorCount} visitor(s)?`)) return;
    await action.run(() => api.del(`/api/admin/registrants/${r.id}`), 'Registrant deleted');
    if (selected === r.id) setSelected(undefined);
    list.reload();
  };

  return (
    <>
      <div className="toolbar">
        <h2>Registrants</h2>
        <span>Year:</span>
        <YearSelect value={year} onChange={(y) => { setYear(y); setPage(0); }} />
        <span className="spacer" />
        <button onClick={() => setCreating({ name: '', email: '', company: false, student: false, paymentType: 'BANK_TRANSFER', branchYear: year })}>
          Add registrant
        </button>
      </div>
      <Alerts error={action.error} notice={action.notice} />
      {creating && (
        <div className="panel">
          <div className="form-grid">
            <Field label="Name" value={creating.name} onChange={(v) => setCreating({ ...creating, name: v })} required />
            <Field label="E-mail" value={creating.email} onChange={(v) => setCreating({ ...creating, email: v })} />
            <Checkbox label="Company" checked={creating.company} onChange={(v) => setCreating({ ...creating, company: v })} />
          </div>
          <div className="buttons">
            <button onClick={create}>Create</button>
            <button className="secondary" onClick={() => setCreating(undefined)}>Cancel</button>
          </div>
        </div>
      )}
      {selected && <RegistrantDetail key={selected} id={selected} onChanged={list.reload} />}
      <Loading loading={list.loading} error={list.error} />
      <table>
        <thead>
        <tr><th>Name</th><th>E-mail</th><th>Visitors</th><th>ePay #</th><th>Invoice #</th><th>Proforma #</th>
          <th>Payment</th><th>Student</th><th>Registered</th><th /></tr>
        </thead>
        <tbody>
        {list.data?.content.map((r) => (
          <tr key={r.id} className={selected === r.id ? 'selected' : ''}>
            <td>{r.name}{r.company && <> <span className="badge">company</span></>}</td>
            <td>{r.email}</td>
            <td>{r.visitorCount}</td>
            <td>{r.epayInvoiceNumber || ''}</td>
            <td>{r.realInvoiceNumber || ''}</td>
            <td>{r.proformaInvoiceNumber || ''}</td>
            <td>{r.paymentType}</td>
            <td>{r.student ? 'yes' : ''}</td>
            <td>{formatDateTime(r.createdDate)}</td>
            <td className="actions">
              <button className="secondary" onClick={() => setSelected(r.id)}>Open</button>
              <button className="danger" onClick={() => remove(r)}>Delete</button>
            </td>
          </tr>
        ))}
        </tbody>
      </table>
      {list.data && <Pager page={list.data.page} totalPages={list.data.totalPages} onPage={setPage} />}
    </>
  );
}
