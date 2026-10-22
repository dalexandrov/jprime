import { useEffect, useRef, useState } from 'react';
import { api } from '../api';
import { Loading, useLoad } from '../components/ui';
import type { RaffleEntry } from '../types';

/**
 * The raffle shown on the big screen: shuffles the names of the present visitors (they registered with the
 * "I am here!" form on the site) and stops on a winner. A winner is not drawn again.
 */
export default function RafflePage() {
  const [kind, setKind] = useState<'jprime' | 'jpro'>('jprime');
  const candidates = useLoad(() => api.get<RaffleEntry[]>(`/api/admin/raffle/${kind}`), [kind]);
  const [current, setCurrent] = useState<RaffleEntry>();
  const [running, setRunning] = useState(false);
  const [winners, setWinners] = useState<RaffleEntry[]>([]);
  const pool = useRef<RaffleEntry[]>([]);
  const index = useRef(-1);

  useEffect(() => {
    pool.current = [...(candidates.data ?? [])];
    setWinners([]);
    setCurrent(undefined);
  }, [candidates.data]);

  useEffect(() => {
    if (!running) return undefined;
    const timer = setInterval(() => {
      if (pool.current.length === 0) return;
      index.current = Math.floor(Math.random() * pool.current.length);
      setCurrent(pool.current[index.current]);
    }, 100);
    return () => clearInterval(timer);
  }, [running]);

  const toggle = () => {
    if (running && index.current >= 0) {
      const [winner] = pool.current.splice(index.current, 1);
      setCurrent(winner);
      setWinners((w) => [...w, winner]);
      index.current = -1;
    }
    setRunning(!running);
  };

  return (
    <>
      <div className="toolbar">
        <h2>Raffle</h2>
        <select value={kind} onChange={(e) => setKind(e.target.value as 'jprime' | 'jpro')}>
          <option value="jprime">jPrime</option>
          <option value="jpro">jProfessionals</option>
        </select>
        <span className="muted">{pool.current.length} candidate(s)</span>
        <span className="spacer" />
        <button className="secondary" onClick={candidates.reload}>Reload candidates</button>
      </div>
      <Loading loading={candidates.loading} error={candidates.error} />
      <div className="panel" style={{ textAlign: 'center' }}>
        <div className="raffle-result">{current ? `${current.name}${current.company ? ` (${current.company})` : ''}` : '—'}</div>
        <button onClick={toggle} disabled={!running && pool.current.length === 0}>{running ? 'Stop' : 'Start'}</button>
      </div>
      {winners.length > 0 && (
        <div className="panel">
          <h3>Winners</h3>
          <ol>{winners.map((w, i) => <li key={i}>{w.name}{w.company ? ` (${w.company})` : ''}</li>)}</ol>
        </div>
      )}
    </>
  );
}
