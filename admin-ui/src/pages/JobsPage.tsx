import { useEffect } from 'react';
import { api } from '../api';
import { Loading, formatDateTime, useLoad } from '../components/ui';
import type { Job } from '../types';

/** Pending jobs and jobs completed in the last 8 hours. Refreshes every 5 seconds while a job is running. */
export default function JobsPage() {
  const jobs = useLoad(() => api.get<Job[]>('/api/admin/jobs'), []);
  const running = jobs.data?.some((j) => !j.completed) ?? false;

  useEffect(() => {
    if (!running) return undefined;
    const timer = setInterval(jobs.reload, 5000);
    return () => clearInterval(timer);
  }, [running, jobs.reload]);

  return (
    <>
      <div className="toolbar">
        <h2>Background jobs</h2>
        <span className="spacer" />
        <button className="secondary" onClick={jobs.reload}>Refresh</button>
      </div>
      <Loading loading={jobs.loading && !jobs.data} error={jobs.error} />
      {jobs.data?.length === 0 && <p className="muted">No recent jobs.</p>}
      <table>
        <thead><tr><th>Description</th><th>Status</th><th>Completed</th><th>Log</th></tr></thead>
        <tbody>
        {jobs.data?.map((j) => (
          <tr key={j.jobId}>
            <td>{j.description}</td>
            <td>{j.status}</td>
            <td>{j.completed ? formatDateTime(j.completed) : <span className="badge">running</span>}</td>
            <td>{j.log && <pre className="log">{j.log}</pre>}</td>
          </tr>
        ))}
        </tbody>
      </table>
    </>
  );
}
