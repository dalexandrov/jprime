import { NavLink, Outlet } from 'react-router';
import { useLookups } from './ui';

const MENU: [string, string][] = [
  ['/', 'Dashboard'],
  ['/branches', 'Branches'],
  ['/tags', 'Tags'],
  ['/articles', 'Articles'],
  ['/sponsors', 'Sponsors'],
  ['/partners', 'Partners'],
  ['/speakers', 'Speakers'],
  ['/submissions', 'Submissions'],
  ['/halls', 'Halls'],
  ['/sessions', 'Sessions (agenda)'],
  ['/registrants', 'Registrants'],
  ['/visitors', 'Visitors'],
  ['/users', 'Users'],
  ['/jobs', 'Background jobs'],
  ['/raffle', 'Raffle'],
];

export default function Layout({ username, onLogout }: { username: string; onLogout: () => void }) {
  const { currentYear } = useLookups();
  return (
    <div className="layout">
      <aside className="sidebar">
        <h1>jPrime admin</h1>
        <p className="muted">Current branch: {currentYear}</p>
        <nav>
          {MENU.map(([to, label]) => (
            <NavLink key={to} to={to} end={to === '/'}>{label}</NavLink>
          ))}
        </nav>
        <p className="muted">
          {username} · <button className="link" onClick={onLogout}>Logout</button>
        </p>
      </aside>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
