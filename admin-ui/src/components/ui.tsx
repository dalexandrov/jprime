import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { ApiError } from '../api';
import type { Lookups, Option } from '../types';

// ---------------------------------------------------------------- data loading

export function useLoad<T>(loader: () => Promise<T>, deps: unknown[]) {
  const [data, setData] = useState<T | undefined>(undefined);
  const [error, setError] = useState<string | undefined>(undefined);
  const [loading, setLoading] = useState(true);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const load = useCallback(loader, deps);

  const reload = useCallback(() => {
    setLoading(true);
    setError(undefined);
    load()
      .then(setData)
      .catch((e: unknown) => setError(errorMessage(e)))
      .finally(() => setLoading(false));
  }, [load]);

  useEffect(reload, [reload]);
  return { data, error, loading, reload, setData };
}

export function errorMessage(e: unknown): string {
  if (e instanceof ApiError) return e.message;
  if (e instanceof Error) return e.message;
  return String(e);
}

/** Runs an action, shows its error or success message. */
export function useAction() {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | undefined>();
  const [notice, setNotice] = useState<string | undefined>();
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const run = useCallback(async <T,>(action: () => Promise<T>, success?: string | ((result: T) => string)) => {
    setBusy(true);
    setError(undefined);
    setNotice(undefined);
    setFieldErrors({});
    try {
      const result = await action();
      if (success) setNotice(typeof success === 'function' ? success(result) : success);
      return result;
    } catch (e) {
      setError(errorMessage(e));
      if (e instanceof ApiError) setFieldErrors(e.fieldErrors);
      return undefined;
    } finally {
      setBusy(false);
    }
  }, []);

  return { run, busy, error, notice, fieldErrors, setNotice, setError };
}

// ---------------------------------------------------------------- lookups (enums, years)

export const LookupsContext = createContext<Lookups>({ currentYear: new Date().getFullYear(), years: [], enums: {} });

export function useLookups() {
  return useContext(LookupsContext);
}

export function useOptions(name: string): Option[] {
  return useLookups().enums[name] ?? [];
}

// ---------------------------------------------------------------- feedback

export function Alerts({ error, notice }: { error?: string; notice?: string }) {
  return (
    <>
      {error && <div className="alert error">{error}</div>}
      {notice && <div className="alert success">{notice}</div>}
    </>
  );
}

export function Loading({ loading, error }: { loading: boolean; error?: string }) {
  if (error) return <div className="alert error">{error}</div>;
  if (loading) return <p className="muted">Loading…</p>;
  return null;
}

// ---------------------------------------------------------------- form fields

interface FieldProps {
  label: string;
  value: string | number | undefined | null;
  onChange: (value: string) => void;
  type?: string;
  error?: string;
  required?: boolean;
  placeholder?: string;
  hint?: ReactNode;
}

export function Field({ label, value, onChange, type = 'text', error, required, placeholder, hint }: FieldProps) {
  return (
    <label className="field">
      <span>{label}{required && ' *'}</span>
      <input
        type={type}
        value={value ?? ''}
        required={required}
        placeholder={placeholder}
        onChange={(e) => onChange(e.target.value)}
      />
      {hint && <small className="muted">{hint}</small>}
      {error && <small className="error">{error}</small>}
    </label>
  );
}

export function TextArea({ label, value, onChange, rows = 5, error }: {
  label: string; value?: string | null; onChange: (v: string) => void; rows?: number; error?: string;
}) {
  return (
    <label className="field">
      <span>{label}</span>
      <textarea rows={rows} value={value ?? ''} onChange={(e) => onChange(e.target.value)} />
      {error && <small className="error">{error}</small>}
    </label>
  );
}

export function Select({ label, value, onChange, options, empty }: {
  label: string; value?: string | number | null; onChange: (v: string) => void; options: Option[]; empty?: string;
}) {
  return (
    <label className="field">
      <span>{label}</span>
      <select value={value ?? ''} onChange={(e) => onChange(e.target.value)}>
        {empty !== undefined && <option value="">{empty}</option>}
        {options.map((o) => (
          <option key={o.value} value={o.value}>{o.label}</option>
        ))}
      </select>
    </label>
  );
}

export function Checkbox({ label, checked, onChange }: { label: string; checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <label className="checkbox">
      <input type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} /> {label}
    </label>
  );
}

export function FileField({ label, onChange, accept = 'image/*' }: {
  label: string; onChange: (file: File | undefined) => void; accept?: string;
}) {
  return (
    <label className="field">
      <span>{label}</span>
      <input type="file" accept={accept} onChange={(e) => onChange(e.target.files?.[0])} />
    </label>
  );
}

export function YearSelect({ value, onChange, allowAll }: {
  value?: number; onChange: (year: number | undefined) => void; allowAll?: string;
}) {
  const { years } = useLookups();
  return (
    <select value={value ?? ''} onChange={(e) => onChange(e.target.value ? Number(e.target.value) : undefined)}>
      {allowAll !== undefined && <option value="">{allowAll}</option>}
      {years.map((y) => (
        <option key={y} value={y}>{y}</option>
      ))}
    </select>
  );
}

export function Pager({ page, totalPages, onPage }: { page: number; totalPages: number; onPage: (page: number) => void }) {
  if (totalPages <= 1) return null;
  return (
    <div className="pager">
      <button className="secondary" disabled={page <= 0} onClick={() => onPage(page - 1)}>« previous</button>
      <span>page {page + 1} of {totalPages}</span>
      <button className="secondary" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>next »</button>
    </div>
  );
}

// ---------------------------------------------------------------- dates (the API uses ISO LocalDateTime)

export function formatDateTime(value?: string): string {
  if (!value) return '';
  return value.replace('T', ' ').substring(0, 16);
}

export function formatDate(value?: string): string {
  return value ? value.substring(0, 10) : '';
}

/** "2026-06-03T09:00:00" -> "2026-06-03T09:00" for datetime-local inputs */
export function toInputDateTime(value?: string): string {
  return value ? value.substring(0, 16) : '';
}

/** datetime-local / date input value -> ISO LocalDateTime */
export function fromInputDateTime(value: string): string | undefined {
  if (!value) return undefined;
  return value.length === 10 ? `${value}T00:00:00` : value.length === 16 ? `${value}:00` : value;
}

export function confirmed(text: string): boolean {
  return window.confirm(text);
}
