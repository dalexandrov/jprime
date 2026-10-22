import { Link } from 'react-router';
import { api } from '../api';
import { Loading, useLoad, useLookups } from '../components/ui';
import type { Branch, Job, SubmissionStatus, VisitorList } from '../types';

export default function DashboardPage() {
  const { currentYear } = useLookups();
  const branch = useLoad(() => api.get<Branch>(`/api/admin/branches/${currentYear}`), [currentYear]);
  const stats = useLoad(() => api.get<Partial<Record<SubmissionStatus, number>>>('/api/admin/submissions/stats'), []);
  const visitors = useLoad(() => api.get<VisitorList>('/api/admin/visitors'), []);
  const jobs = useLoad(() => api.get<Job[]>('/api/admin/jobs'), []);

  return (
    <>
      <h2>jPrime {currentYear}</h2>
      <Loading loading={branch.loading} error={branch.error} />
      {branch.data && (
        <div className="panel">
          <p>
            Starts <strong>{branch.data.startDate?.substring(0, 10)}</strong> ({branch.data.durationText}) ·
            CFP {branch.data.cfpOpenDate?.substring(0, 10)} – {branch.data.cfpCloseDate?.substring(0, 10)} ·
            {branch.data.soldOut ? ' tickets SOLD OUT' : ' tickets on sale'} ·
            agenda {branch.data.agendaPublished ? 'published' : 'not published'}
          </p>
          <Link to="/branches">Edit branch</Link>
        </div>
      )}
      <div className="panel">
        <h3>Submissions</h3>
        <div className="stats">
          {stats.data && Object.entries(stats.data).map(([status, count]) => (
            <span key={status} className="badge">{status}: {count}</span>
          ))}
          {stats.data && Object.keys(stats.data).length === 0 && <span className="muted">No submissions yet</span>}
        </div>
        <Link to="/submissions">Review submissions</Link>
      </div>
      <div className="panel">
        <h3>Visitors</h3>
        {visitors.data && (
          <div className="stats">
            <span className="badge">Total: {visitors.data.stats.total}</span>
            <span className="badge">Requesting: {visitors.data.stats.requesting}</span>
            <span className="badge">Payed: {visitors.data.stats.payed}</span>
            <span className="badge">Sponsored: {visitors.data.stats.sponsored}</span>
          </div>
        )}
        <Link to="/visitors">Manage visitors</Link>
      </div>
      {jobs.data && jobs.data.length > 0 && (
        <div className="panel">
          <h3>Background jobs</h3>
          <ul>
            {jobs.data.map((j) => <li key={j.jobId}>{j.description}: {j.status}</li>)}
          </ul>
        </div>
      )}
    </>
  );
}
