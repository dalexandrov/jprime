import { useState } from 'react';
import { api, query } from '../api';
import { Alerts, Field, Loading, Pager, confirmed, useAction, useLoad } from '../components/ui';
import type { Page, User } from '../types';

interface NewUser {
  firstName?: string;
  lastName?: string;
  email: string;
  phone?: string;
  password?: string;
}

/** All rows of the User table: site users, speakers and sponsors. */
export default function UsersPage() {
  const [page, setPage] = useState(0);
  const users = useLoad(() => api.get<Page<User>>(`/api/admin/users${query({ page, size: 50 })}`), [page]);
  const [creating, setCreating] = useState<NewUser>();
  const action = useAction();

  const create = async () => {
    if (!creating) return;
    if (await action.run(() => api.post('/api/admin/users', creating), 'User created')) {
      setCreating(undefined);
      users.reload();
    }
  };

  const remove = async (u: User) => {
    if (!confirmed(`Delete ${u.type.toLowerCase()} ${u.email}?`)) return;
    await action.run(() => api.del(`/api/admin/users/${u.id}`), 'User deleted');
    users.reload();
  };

  return (
    <>
      <div className="toolbar">
        <h2>Users</h2>
        <span className="spacer" />
        <button onClick={() => setCreating({ email: '' })}>Add user</button>
      </div>
      <Alerts error={action.error} notice={action.notice} />
      {creating && (
        <div className="panel">
          <div className="form-grid">
            <Field label="First name" value={creating.firstName} onChange={(v) => setCreating({ ...creating, firstName: v })} />
            <Field label="Last name" value={creating.lastName} onChange={(v) => setCreating({ ...creating, lastName: v })} />
            <Field label="E-mail" type="email" value={creating.email} required error={action.fieldErrors.email}
                   onChange={(v) => setCreating({ ...creating, email: v })} />
            <Field label="Phone" value={creating.phone} onChange={(v) => setCreating({ ...creating, phone: v })} />
            <Field label="Password (optional)" type="password" value={creating.password}
                   onChange={(v) => setCreating({ ...creating, password: v })} />
          </div>
          <div className="buttons">
            <button onClick={create} disabled={action.busy}>Create</button>
            <button className="secondary" onClick={() => setCreating(undefined)}>Cancel</button>
          </div>
        </div>
      )}
      <Loading loading={users.loading} error={users.error} />
      <table>
        <thead><tr><th>Name</th><th>E-mail</th><th>Phone</th><th>Type</th><th /></tr></thead>
        <tbody>
        {users.data?.content.map((u) => (
          <tr key={u.id}>
            <td>{u.firstName} {u.lastName}</td>
            <td>{u.email}</td>
            <td>{u.phone}</td>
            <td>{u.type}</td>
            <td className="actions"><button className="danger" onClick={() => remove(u)}>Delete</button></td>
          </tr>
        ))}
        </tbody>
      </table>
      {users.data && <Pager page={users.data.page} totalPages={users.data.totalPages} onPage={setPage} />}
    </>
  );
}
