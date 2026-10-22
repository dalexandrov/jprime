import { useCallback, useEffect, useState } from 'react';
import { Route, Routes } from 'react-router';
import { api, setUnauthorizedHandler } from './api';
import Layout from './components/Layout';
import { LookupsContext } from './components/ui';
import type { Lookups } from './types';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import BranchesPage from './pages/BranchesPage';
import TagsPage from './pages/TagsPage';
import ArticlesPage from './pages/ArticlesPage';
import { PartnersPage, SponsorsPage } from './pages/CompaniesPage';
import SpeakersPage from './pages/SpeakersPage';
import SubmissionsPage from './pages/SubmissionsPage';
import HallsPage from './pages/HallsPage';
import SessionsPage from './pages/SessionsPage';
import RegistrantsPage from './pages/RegistrantsPage';
import VisitorsPage from './pages/VisitorsPage';
import UsersPage from './pages/UsersPage';
import JobsPage from './pages/JobsPage';
import RafflePage from './pages/RafflePage';

type AuthState = { status: 'checking' } | { status: 'anonymous' } | { status: 'authenticated'; username: string };

export default function App() {
  const [auth, setAuth] = useState<AuthState>({ status: 'checking' });
  const [lookups, setLookups] = useState<Lookups | undefined>();

  const checkSession = useCallback(async () => {
    const response = await fetch('/api/auth/me', { credentials: 'same-origin' });
    if (response.ok) {
      const me = await response.json();
      setAuth({ status: 'authenticated', username: me.username });
    } else {
      setAuth({ status: 'anonymous' });
    }
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(() => setAuth({ status: 'anonymous' }));
    checkSession().catch(() => setAuth({ status: 'anonymous' }));
  }, [checkSession]);

  useEffect(() => {
    if (auth.status === 'authenticated') {
      api.get<Lookups>('/api/admin/lookups').then(setLookups).catch(() => setLookups(undefined));
    }
  }, [auth.status]);

  if (auth.status === 'checking') {
    return <p className="center muted">Loading…</p>;
  }
  if (auth.status === 'anonymous') {
    return <LoginPage onLoggedIn={checkSession} />;
  }
  if (!lookups) {
    return <p className="center muted">Loading…</p>;
  }

  const logout = async () => {
    await api.logout().catch(() => undefined);
    setAuth({ status: 'anonymous' });
  };

  const refreshLookups = () => api.get<Lookups>('/api/admin/lookups').then(setLookups);

  return (
    <LookupsContext.Provider value={lookups}>
      <Routes>
        <Route element={<Layout username={auth.username} onLogout={logout} />}>
          <Route index element={<DashboardPage />} />
          <Route path="branches" element={<BranchesPage onChanged={refreshLookups} />} />
          <Route path="tags" element={<TagsPage />} />
          <Route path="articles" element={<ArticlesPage />} />
          <Route path="sponsors" element={<SponsorsPage />} />
          <Route path="partners" element={<PartnersPage />} />
          <Route path="speakers" element={<SpeakersPage />} />
          <Route path="submissions" element={<SubmissionsPage />} />
          <Route path="halls" element={<HallsPage />} />
          <Route path="sessions" element={<SessionsPage />} />
          <Route path="registrants" element={<RegistrantsPage />} />
          <Route path="visitors" element={<VisitorsPage />} />
          <Route path="users" element={<UsersPage />} />
          <Route path="jobs" element={<JobsPage />} />
          <Route path="raffle" element={<RafflePage />} />
          <Route path="*" element={<p>Page not found.</p>} />
        </Route>
      </Routes>
    </LookupsContext.Provider>
  );
}
