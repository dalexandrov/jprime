import { useState } from 'react';
import { api } from '../api';
import {
  Alerts, Checkbox, Field, Loading, confirmed, formatDate, fromInputDateTime, toInputDateTime, useAction, useLoad,
  useOptions,
} from '../components/ui';
import type { Branch, TicketType } from '../types';

const DURATIONS: [number, string][] = [[4, '4 hours'], [24, '1 day'], [48, '2 days'], [72, '3 days'], [96, '4 days'], [120, '5 days']];
const TICKETS: [TicketType, string][] = [['EARLY_BIRD', 'Early bird'], ['REGULAR', 'Regular'], ['STUDENT', 'Student']];

function emptyBranch(year: number): Branch {
  return {
    year, label: `YEAR_${year}`, durationHours: 48, durationText: '', soldOut: false, agendaPublished: false,
    currentBranch: false, soldOutPackages: [], ticketPrices: {},
  };
}

export default function BranchesPage({ onChanged }: { onChanged: () => void }) {
  const branches = useLoad(() => api.get<Branch[]>('/api/admin/branches'), []);
  const [editing, setEditing] = useState<Branch>();
  const [isNew, setIsNew] = useState(false);
  const action = useAction();
  const packages = useOptions('sponsorPackage');

  const save = async () => {
    if (!editing) return;
    const body = {
      startDate: fromInputDateTime(toInputDateTime(editing.startDate)),
      durationHours: editing.durationHours,
      cfpOpenDate: editing.cfpOpenDate,
      cfpCloseDate: editing.cfpCloseDate,
      soldOut: editing.soldOut,
      agendaPublished: editing.agendaPublished,
      soldOutPackages: editing.soldOutPackages,
      ticketPrices: editing.ticketPrices,
    };
    const saved = await action.run(() => api.put<Branch>(`/api/admin/branches/${editing.year}`, body), 'Branch saved');
    if (saved) {
      setEditing(undefined);
      branches.reload();
      onChanged();
    }
  };

  const makeCurrent = async (year: number) => {
    if (await action.run(() => api.post(`/api/admin/branches/${year}/current`), `${year} is now the current branch`)) {
      branches.reload();
      onChanged();
    }
  };

  const remove = async (year: number) => {
    if (!confirmed(`Delete branch ${year} and its ticket prices?`)) return;
    await action.run(() => api.del(`/api/admin/branches/${year}`), 'Branch deleted');
    branches.reload();
    onChanged();
  };

  const set = (patch: Partial<Branch>) => editing && setEditing({ ...editing, ...patch });

  return (
    <>
      <div className="toolbar">
        <h2>Branches</h2>
        <span className="spacer" />
        <button onClick={() => {
          const next = Math.max(new Date().getFullYear(), ...(branches.data ?? []).map((b) => b.year + 1));
          setEditing(emptyBranch(next));
          setIsNew(true);
        }}>Add branch</button>
      </div>
      <Alerts error={action.error} notice={action.notice} />

      {editing && (
        <div className="panel">
          <h3>{isNew ? 'New branch' : `Edit ${editing.year}`}</h3>
          <div className="form-grid">
            {isNew && <Field label="Year" type="number" value={editing.year} onChange={(v) => set({ year: Number(v) })} />}
            <Field label="Start date" type="datetime-local" value={toInputDateTime(editing.startDate)}
                   onChange={(v) => set({ startDate: fromInputDateTime(v) })} required />
            <label className="field">
              <span>Duration</span>
              <select value={editing.durationHours} onChange={(e) => set({ durationHours: Number(e.target.value) })}>
                {DURATIONS.map(([h, label]) => <option key={h} value={h}>{label}</option>)}
              </select>
            </label>
            <Field label="CFP opens" type="date" value={formatDate(editing.cfpOpenDate)}
                   onChange={(v) => set({ cfpOpenDate: fromInputDateTime(v) })} />
            <Field label="CFP closes (end of day)" type="date" value={formatDate(editing.cfpCloseDate)}
                   onChange={(v) => set({ cfpCloseDate: fromInputDateTime(v) })} />
            {TICKETS.map(([type, label]) => (
              <Field key={type} label={`${label} price (EUR)`} type="number" value={editing.ticketPrices[type] ?? ''}
                     onChange={(v) => set({ ticketPrices: { ...editing.ticketPrices, [type]: v === '' ? undefined : Number(v) } })} />
            ))}
            <Checkbox label="Tickets sold out" checked={editing.soldOut} onChange={(v) => set({ soldOut: v })} />
            <Checkbox label="Agenda published" checked={editing.agendaPublished}
                      onChange={(v) => set({ agendaPublished: v })} />
          </div>
          <h4>Sold out sponsor packages</h4>
          <div className="toolbar">
            {packages.map((p) => (
              <Checkbox key={p.value} label={p.label} checked={editing.soldOutPackages.includes(p.value)}
                        onChange={(on) => set({
                          soldOutPackages: on ? [...editing.soldOutPackages, p.value] :
                            editing.soldOutPackages.filter((x) => x !== p.value),
                        })} />
            ))}
          </div>
          <div className="buttons">
            <button onClick={save} disabled={action.busy}>Save</button>
            <button className="secondary" onClick={() => setEditing(undefined)}>Cancel</button>
          </div>
        </div>
      )}

      <Loading loading={branches.loading} error={branches.error} />
      <table>
        <thead>
        <tr><th>Year</th><th>Start</th><th>Duration</th><th>CFP</th><th>Prices (EB / R / S)</th><th>Flags</th><th /></tr>
        </thead>
        <tbody>
        {branches.data?.map((b) => (
          <tr key={b.year} className={b.currentBranch ? 'selected' : ''}>
            <td>{b.year}{b.currentBranch && <> <span className="badge">current</span></>}</td>
            <td>{b.startDate?.replace('T', ' ').substring(0, 16)}</td>
            <td>{b.durationText}</td>
            <td>{formatDate(b.cfpOpenDate)} – {formatDate(b.cfpCloseDate)}</td>
            <td>{b.ticketPrices.EARLY_BIRD ?? '-'} / {b.ticketPrices.REGULAR ?? '-'} / {b.ticketPrices.STUDENT ?? '-'}</td>
            <td>{b.soldOut && <span className="badge">sold out</span>} {b.agendaPublished && <span className="badge">agenda</span>}</td>
            <td className="actions">
              <button className="secondary" onClick={() => { setEditing(b); setIsNew(false); }}>Edit</button>
              {!b.currentBranch && <button className="secondary" onClick={() => makeCurrent(b.year)}>Make current</button>}
              {!b.currentBranch && <button className="danger" onClick={() => remove(b.year)}>Delete</button>}
            </td>
          </tr>
        ))}
        </tbody>
      </table>
    </>
  );
}
