import { useState, type FormEvent } from 'react';
import { api } from '../api';

export default function LoginPage({ onLoggedIn }: { onLoggedIn: () => void }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string>();
  const [busy, setBusy] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(undefined);
    try {
      if (await api.login(username, password)) {
        onLoggedIn();
      } else {
        setError('Invalid username or password');
      }
    } catch {
      setError('The server is not reachable');
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="login panel" onSubmit={submit}>
      <h2>jPrime admin</h2>
      {error && <div className="alert error">{error}</div>}
      <label className="field">
        <span>Username</span>
        <input autoFocus value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" />
      </label>
      <label className="field">
        <span>Password</span>
        <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
               autoComplete="current-password" />
      </label>
      <div className="buttons">
        <button type="submit" disabled={busy}>Login</button>
      </div>
    </form>
  );
}
