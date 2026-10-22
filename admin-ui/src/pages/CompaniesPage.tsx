import { useState } from 'react';
import { api } from '../api';
import { Alerts, Checkbox, FileField, Field, Loading, Select, TextArea, confirmed, useAction, useLoad, useOptions } from '../components/ui';
import type { Partner, Sponsor } from '../types';

type Company = Sponsor & Partner;

interface Config {
  title: string;
  endpoint: string;
  packageField: 'sponsorPackage' | 'partnerPackage';
  packageOptions: string;
  withEmail: boolean;
}

function CompaniesPage({ config }: { config: Config }) {
  const list = useLoad(() => api.get<Company[]>(config.endpoint), [config.endpoint]);
  const [editing, setEditing] = useState<Company>();
  const [logo, setLogo] = useState<File>();
  const [resize, setResize] = useState(true);
  const [version, setVersion] = useState(0);
  const action = useAction();
  const packages = useOptions(config.packageOptions);

  const save = async () => {
    if (!editing) return;
    const form = new FormData();
    const fields: (keyof Company)[] = ['companyName', 'companyWebsite', 'email', 'description', config.packageField];
    fields.forEach((f) => {
      const value = editing[f];
      if (value !== undefined && value !== null) form.append(f, String(value));
    });
    form.append('active', String(editing.active));
    form.append('resizeImage', String(resize));
    if (logo) form.append('logo', logo);
    const ok = await action.run(() => editing.id ? api.put(`${config.endpoint}/${editing.id}`, form) :
      api.post(config.endpoint, form), 'Saved');
    if (ok) {
      setEditing(undefined);
      setLogo(undefined);
      setVersion(version + 1);
      list.reload();
    }
  };

  const remove = async (c: Company) => {
    if (!confirmed(`Delete "${c.companyName}"?`)) return;
    await action.run(() => api.del(`${config.endpoint}/${c.id}`), 'Deleted');
    list.reload();
  };

  return (
    <>
      <div className="toolbar">
        <h2>{config.title}</h2>
        <span className="spacer" />
        <button onClick={() => setEditing({ active: true })}>Add</button>
      </div>
      <Alerts error={action.error} notice={action.notice} />
      {editing && (
        <div className="panel">
          <div className="form-grid">
            <Field label="Company name" value={editing.companyName} required error={action.fieldErrors.companyName}
                   onChange={(v) => setEditing({ ...editing, companyName: v })} />
            <Field label="Website" value={editing.companyWebsite}
                   onChange={(v) => setEditing({ ...editing, companyWebsite: v })} />
            {config.withEmail && (
              <Field label="E-mail" type="email" value={editing.email} required error={action.fieldErrors.email}
                     onChange={(v) => setEditing({ ...editing, email: v })} />
            )}
            <Select label="Package" value={editing[config.packageField]} options={packages} empty="-"
                    onChange={(v) => setEditing({ ...editing, [config.packageField]: v || undefined })} />
            <Checkbox label="Active (shown on the site)" checked={editing.active}
                      onChange={(v) => setEditing({ ...editing, active: v })} />
            <div className="wide">
              <TextArea label="Description" value={editing.description}
                        onChange={(v) => setEditing({ ...editing, description: v })} />
            </div>
            <FileField label={editing.id ? 'Replace logo' : 'Logo'} onChange={setLogo} />
            <Checkbox label="Resize logo to 180×64" checked={resize} onChange={setResize} />
            {editing.id && <img className="thumb" src={`${config.endpoint}/${editing.id}/logo?v=${version}`} alt="" />}
          </div>
          <div className="buttons">
            <button onClick={save} disabled={action.busy}>Save</button>
            <button className="secondary" onClick={() => setEditing(undefined)}>Cancel</button>
          </div>
        </div>
      )}
      <Loading loading={list.loading} error={list.error} />
      <table>
        <thead><tr><th>Logo</th><th>Company</th><th>Package</th><th>Active</th><th /></tr></thead>
        <tbody>
        {list.data?.map((c) => (
          <tr key={c.id}>
            <td><img className="thumb" src={`${config.endpoint}/${c.id}/logo?v=${version}`} alt="" /></td>
            <td>{c.companyWebsite ? <a href={c.companyWebsite} target="_blank" rel="noreferrer">{c.companyName}</a> : c.companyName}</td>
            <td>{c[config.packageField]}</td>
            <td>{c.active ? 'yes' : 'no'}</td>
            <td className="actions">
              <button className="secondary" onClick={() => { setEditing(c); setLogo(undefined); }}>Edit</button>
              <button className="danger" onClick={() => remove(c)}>Delete</button>
            </td>
          </tr>
        ))}
        </tbody>
      </table>
    </>
  );
}

export function SponsorsPage() {
  return <CompaniesPage key="sponsors" config={{
    title: 'Sponsors', endpoint: '/api/admin/sponsors', packageField: 'sponsorPackage',
    packageOptions: 'sponsorPackage', withEmail: true,
  }} />;
}

export function PartnersPage() {
  return <CompaniesPage key="partners" config={{
    title: 'Partners', endpoint: '/api/admin/partners', packageField: 'partnerPackage',
    packageOptions: 'partnerPackage', withEmail: false,
  }} />;
}
