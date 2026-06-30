import { ArrowLeft, CheckCircle2, RotateCcw, XCircle } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { getLesson } from '../api/curriculum';
import { submitAttempts } from '../api/progress';
import type {
  AttemptResult,
  ExercisePublic,
  FillBlankPayload,
  LessonDetail,
  McPayload,
  SubmitAttemptsResponse,
  TranslationPayload,
} from '../api/types';

type Answer = Record<string, unknown>;

function isAnswered(ex: ExercisePublic, answer: Answer | undefined): boolean {
  if (!answer) return false;
  if (ex.type === 'multiple_choice') return typeof answer.selected_index === 'number';
  return typeof answer.text === 'string' && (answer.text as string).trim().length > 0;
}

function ExerciseItem({
  exercise,
  answer,
  onChange,
  result,
}: {
  exercise: ExercisePublic;
  answer: Answer | undefined;
  onChange: (a: Answer) => void;
  result?: AttemptResult;
}) {
  const locked = result !== undefined; // após submeter, trava a edição
  const border = !result
    ? 'border-[#3C4043]'
    : result.isCorrect
      ? 'border-green-500/40'
      : 'border-red-500/40';

  return (
    <div className={`rounded-2xl border bg-[#1E1E1F] p-6 ${border}`}>
      {exercise.type === 'multiple_choice' && (
        <McView
          payload={exercise.payload as unknown as McPayload}
          selected={answer?.selected_index as number | undefined}
          correctIndex={result?.correctAnswer?.selected_index as number | undefined}
          locked={locked}
          onSelect={(index) => onChange({ selected_index: index })}
        />
      )}

      {exercise.type === 'fill_blank' && (
        <TextView
          prompt={(exercise.payload as unknown as FillBlankPayload).sentence_template}
          hint={(exercise.payload as unknown as FillBlankPayload).hint}
          value={(answer?.text as string) ?? ''}
          locked={locked}
          onChange={(text) => onChange({ text })}
        />
      )}

      {exercise.type === 'translation' && (
        <TextView
          prompt={`Traduza: "${(exercise.payload as unknown as TranslationPayload).source_text}"`}
          value={(answer?.text as string) ?? ''}
          locked={locked}
          onChange={(text) => onChange({ text })}
        />
      )}

      {result && <ResultBanner result={result} />}
    </div>
  );
}

function McView({
  payload,
  selected,
  correctIndex,
  locked,
  onSelect,
}: {
  payload: McPayload;
  selected: number | undefined;
  correctIndex: number | undefined;
  locked: boolean;
  onSelect: (index: number) => void;
}) {
  return (
    <div>
      <p className="mb-4 font-medium">{payload.prompt}</p>
      <div className="space-y-2">
        {payload.options.map((opt) => {
          const isSelected = selected === opt.index;
          const isCorrect = correctIndex === opt.index;
          let cls = 'border-[#3C4043] bg-[#131314] hover:border-blue-500/40';
          if (locked && isCorrect) cls = 'border-green-500/50 bg-green-500/10';
          else if (locked && isSelected) cls = 'border-red-500/50 bg-red-500/10';
          else if (isSelected) cls = 'border-blue-500/60 bg-blue-500/10';
          return (
            <button
              key={opt.index}
              disabled={locked}
              onClick={() => onSelect(opt.index)}
              className={`block w-full rounded-xl border px-4 py-2.5 text-left text-sm transition ${cls}`}
            >
              {opt.text}
            </button>
          );
        })}
      </div>
    </div>
  );
}

function TextView({
  prompt,
  hint,
  value,
  locked,
  onChange,
}: {
  prompt: string;
  hint?: string;
  value: string;
  locked: boolean;
  onChange: (v: string) => void;
}) {
  return (
    <div>
      <p className="mb-3 font-medium">{prompt}</p>
      <input
        type="text"
        value={value}
        disabled={locked}
        onChange={(e) => onChange(e.target.value)}
        placeholder="Sua resposta…"
        className="w-full rounded-xl border border-[#3C4043] bg-[#131314] px-4 py-2.5 text-sm text-[#E8EAED] placeholder-[#9AA0A6] outline-none focus:border-blue-500 disabled:opacity-70"
      />
      {hint && <p className="mt-2 text-xs text-[#9AA0A6]">💡 {hint}</p>}
    </div>
  );
}

function ResultBanner({ result }: { result: AttemptResult }) {
  if (result.isCorrect) {
    return (
      <p className="mt-4 flex items-center gap-2 text-sm font-medium text-green-400">
        <CheckCircle2 className="h-4 w-4" /> Correto! +{result.xpEarned} XP
      </p>
    );
  }
  const correct = result.correctAnswer?.text as string | undefined;
  return (
    <div className="mt-4 space-y-1 text-sm">
      <p className="flex items-center gap-2 font-medium text-red-400">
        <XCircle className="h-4 w-4" /> Incorreto
      </p>
      {correct && (
        <p className="text-[#E8EAED]">
          Resposta certa: <span className="font-semibold text-green-300">{correct}</span>
        </p>
      )}
      {result.feedback && <p className="text-[#9AA0A6]">{result.feedback}</p>}
    </div>
  );
}

export function LessonScreen({ lessonId, onBack }: { lessonId: string; onBack: () => void }) {
  const [lesson, setLesson] = useState<LessonDetail | null>(null);
  const [answers, setAnswers] = useState<Record<string, Answer>>({});
  const [result, setResult] = useState<SubmitAttemptsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getLesson(lessonId)
      .then((r) => setLesson(r.data))
      .catch(() => setError('Não foi possível carregar a lição.'))
      .finally(() => setLoading(false));
  }, [lessonId]);

  const resultById = useMemo(
    () => new Map((result?.results ?? []).map((r) => [r.exerciseId, r])),
    [result],
  );

  const allAnswered = lesson?.exercises.every((ex) => isAnswered(ex, answers[ex.id])) ?? false;
  const theoryHtml = lesson?.content?.theory_html as string | undefined;

  const submit = async () => {
    if (!lesson) return;
    setSubmitting(true);
    setError(null);
    try {
      const res = await submitAttempts({
        lessonId: lesson.id,
        attempts: lesson.exercises.map((ex) => ({ exerciseId: ex.id, userAnswer: answers[ex.id] ?? {} })),
      });
      setResult(res.data);
    } catch {
      setError('Falha ao enviar as respostas.');
    } finally {
      setSubmitting(false);
    }
  };

  const retry = () => {
    setResult(null);
    setAnswers({});
  };

  return (
    <div className="min-h-screen bg-[#131314] text-[#E8EAED]">
      <header className="flex items-center gap-4 border-b border-[#3C4043] px-6 py-4">
        <button
          onClick={onBack}
          className="flex items-center gap-1.5 rounded-lg bg-[#28292A] px-3 py-1.5 text-sm text-[#9AA0A6] transition hover:text-[#E8EAED]"
        >
          <ArrowLeft className="h-4 w-4" /> Lições
        </button>
        <span className="truncate font-semibold">{lesson?.title ?? 'Lição'}</span>
      </header>

      <main className="mx-auto max-w-3xl p-6 md:p-10">
        {loading && <p className="text-[#9AA0A6]">Carregando…</p>}
        {error && (
          <p className="mb-4 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">
            {error}
          </p>
        )}

        {theoryHtml && (
          <div
            className="mb-8 rounded-2xl border border-blue-500/20 bg-[#1E1E1F] p-6 text-sm leading-relaxed text-[#cfd3d8]"
            dangerouslySetInnerHTML={{ __html: theoryHtml }}
          />
        )}

        {result && (
          <div className="mb-6 rounded-2xl border border-purple-500/30 bg-purple-500/10 p-5">
            <p className="text-lg font-bold">
              Pontuação: {result.lessonProgress.currentScore.toFixed(0)}%
            </p>
            <p className="text-sm text-[#cfb8f0]">
              Status: {result.lessonProgress.status} · acertos valem 10 XP (só no 1º acerto de cada).
            </p>
          </div>
        )}

        <div className="space-y-4">
          {lesson?.exercises.map((ex) => (
            <ExerciseItem
              key={ex.id}
              exercise={ex}
              answer={answers[ex.id]}
              result={resultById.get(ex.id)}
              onChange={(a) => setAnswers((prev) => ({ ...prev, [ex.id]: a }))}
            />
          ))}
        </div>

        {lesson && (
          <div className="mt-8 flex gap-3">
            {!result ? (
              <button
                onClick={submit}
                disabled={!allAnswered || submitting}
                className="rounded-xl bg-gradient-to-r from-blue-600 to-purple-600 px-6 py-3 text-sm font-semibold text-white transition hover:opacity-90 disabled:opacity-50"
              >
                {submitting ? 'Enviando…' : allAnswered ? 'Enviar respostas' : 'Responda tudo para enviar'}
              </button>
            ) : (
              <>
                <button
                  onClick={retry}
                  className="flex items-center gap-2 rounded-xl bg-[#28292A] px-5 py-3 text-sm font-semibold text-[#E8EAED] transition hover:bg-[#3C4043]"
                >
                  <RotateCcw className="h-4 w-4" /> Refazer
                </button>
                <button
                  onClick={onBack}
                  className="rounded-xl bg-gradient-to-r from-blue-600 to-purple-600 px-6 py-3 text-sm font-semibold text-white transition hover:opacity-90"
                >
                  Voltar às lições
                </button>
              </>
            )}
          </div>
        )}
      </main>
    </div>
  );
}
