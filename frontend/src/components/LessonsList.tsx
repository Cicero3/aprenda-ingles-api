import { ArrowLeft, CheckCircle2, Circle, Lock, Trophy } from 'lucide-react';
import { useEffect, useState } from 'react';
import { listLessons } from '../api/curriculum';
import type { LessonStatus, LessonSummary } from '../api/types';

const STATUS_LABEL: Record<LessonStatus, string> = {
  not_started: 'Não iniciada',
  in_progress: 'Em andamento',
  completed: 'Concluída',
  mastered: 'Dominada',
};

function StatusIcon({ status, locked }: { status: LessonStatus; locked: boolean }) {
  if (locked) return <Lock className="h-5 w-5 text-[#5f6368]" />;
  if (status === 'mastered') return <Trophy className="h-5 w-5 text-yellow-400" />;
  if (status === 'completed') return <CheckCircle2 className="h-5 w-5 text-green-400" />;
  if (status === 'in_progress') return <Circle className="h-5 w-5 text-blue-400" />;
  return <Circle className="h-5 w-5 text-[#9AA0A6]" />;
}

export function LessonsList({
  moduleId,
  moduleTitle,
  onBack,
  onOpenLesson,
}: {
  moduleId: string;
  moduleTitle: string;
  onBack: () => void;
  onOpenLesson: (lessonId: string) => void;
}) {
  const [lessons, setLessons] = useState<LessonSummary[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    listLessons(moduleId)
      .then((r) => setLessons(r.data))
      .catch(() => setError('Não foi possível carregar as lições.'))
      .finally(() => setLoading(false));
  }, [moduleId]);

  return (
    <div className="min-h-screen bg-[#131314] text-[#E8EAED]">
      <header className="flex items-center gap-4 border-b border-[#3C4043] px-6 py-4">
        <button
          onClick={onBack}
          className="flex items-center gap-1.5 rounded-lg bg-[#28292A] px-3 py-1.5 text-sm text-[#9AA0A6] transition hover:text-[#E8EAED]"
        >
          <ArrowLeft className="h-4 w-4" /> Módulos
        </button>
        <span className="truncate font-semibold">{moduleTitle}</span>
      </header>

      <main className="mx-auto max-w-3xl p-6 md:p-10">
        <h1 className="mb-8 text-2xl font-bold">Lições</h1>

        {loading && <p className="text-[#9AA0A6]">Carregando…</p>}
        {error && (
          <p className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">
            {error}
          </p>
        )}

        <ol className="space-y-3">
          {lessons.map((l) => (
            <li key={l.id}>
              <button
                disabled={l.locked}
                onClick={() => onOpenLesson(l.id)}
                className={`flex w-full items-center gap-4 rounded-2xl border p-4 text-left transition ${
                  l.locked
                    ? 'cursor-not-allowed border-[#28292A] bg-[#1A1A1B] opacity-60'
                    : 'border-[#3C4043] bg-[#1E1E1F] hover:border-purple-500/40'
                }`}
              >
                <StatusIcon status={l.status} locked={l.locked} />
                <div className="min-w-0 flex-1">
                  <p className="truncate font-medium">
                    {l.orderIndex}. {l.title}
                  </p>
                  <p className="text-xs text-[#9AA0A6]">
                    {l.locked
                      ? 'Conclua a lição anterior para desbloquear'
                      : `${STATUS_LABEL[l.status]} · ${l.estimatedMinutes} min${
                          l.bestScore > 0 ? ` · melhor: ${l.bestScore.toFixed(0)}%` : ''
                        }`}
                  </p>
                </div>
              </button>
            </li>
          ))}
        </ol>
      </main>
    </div>
  );
}
