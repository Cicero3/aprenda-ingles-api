import { BookOpen, Flame, LogOut, Star } from 'lucide-react';
import { useEffect, useState } from 'react';
import { listModules } from '../api/curriculum';
import { myProgress } from '../api/progress';
import type { ModuleSummary, ProgressDashboard } from '../api/types';
import { useAuth } from '../auth/AuthContext';

export function ModulesDashboard() {
  const { user, logout } = useAuth();
  const [modules, setModules] = useState<ModuleSummary[]>([]);
  const [dashboard, setDashboard] = useState<ProgressDashboard | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([listModules(), myProgress()])
      .then(([mods, prog]) => {
        setModules(mods.data);
        setDashboard(prog.data);
      })
      .catch(() => setError('Não foi possível carregar o conteúdo. O backend está no ar?'))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="min-h-screen bg-[#131314] text-[#E8EAED]">
      <header className="flex items-center justify-between border-b border-[#3C4043] px-6 py-4">
        <div className="flex items-center gap-3">
          <BookOpen className="h-5 w-5 text-purple-400" />
          <span className="font-semibold">Aprenda Inglês</span>
        </div>
        <div className="flex items-center gap-4 text-sm">
          {dashboard && (
            <>
              <span className="flex items-center gap-1.5 text-purple-300" title="XP total">
                <Star className="h-4 w-4" /> {dashboard.totalXp} XP
              </span>
              <span className="flex items-center gap-1.5 text-orange-300" title="Sequência">
                <Flame className="h-4 w-4" /> {dashboard.streakDays}
              </span>
            </>
          )}
          <span className="hidden text-[#9AA0A6] sm:inline">{user?.email}</span>
          <button
            onClick={() => logout()}
            className="flex items-center gap-1.5 rounded-lg bg-[#28292A] px-3 py-1.5 text-[#9AA0A6] transition hover:text-[#E8EAED]"
          >
            <LogOut className="h-4 w-4" /> Sair
          </button>
        </div>
      </header>

      <main className="mx-auto max-w-4xl p-6 md:p-10">
        <h1 className="mb-8 text-2xl font-bold">Seus módulos</h1>

        {loading && <p className="text-[#9AA0A6]">Carregando…</p>}
        {error && (
          <p className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">
            {error}
          </p>
        )}

        <div className="grid gap-4 md:grid-cols-2">
          {modules.map((m) => (
            <article
              key={m.id}
              className="rounded-2xl border border-[#3C4043] bg-[#1E1E1F] p-6 transition hover:border-purple-500/40"
            >
              <div className="mb-3 flex items-center justify-between">
                <span className="rounded-md bg-blue-500/10 px-2 py-0.5 text-xs font-bold text-blue-300">
                  {m.level}
                </span>
                <span className="text-xs text-[#9AA0A6]">
                  {m.completedLessonCount}/{m.lessonCount} lições
                </span>
              </div>
              <h2 className="mb-2 font-bold">{m.title}</h2>
              {m.description && (
                <p className="mb-4 line-clamp-2 text-sm text-[#9AA0A6]">{m.description}</p>
              )}
              <div className="h-2 overflow-hidden rounded-full bg-[#28292A]">
                <div
                  className="h-full rounded-full bg-gradient-to-r from-blue-500 to-purple-500 transition-all"
                  style={{ width: `${m.progressPercent}%` }}
                />
              </div>
              <p className="mt-1 text-right text-xs text-[#9AA0A6]">{m.progressPercent}%</p>
            </article>
          ))}
        </div>
      </main>
    </div>
  );
}
