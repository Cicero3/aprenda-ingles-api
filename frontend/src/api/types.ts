// Tipos espelhando o contrato do backend (docs/content-contract.md + DTOs).
// INTERINO: quando o backend estiver no ar, gere o client tipado pelo OpenAPI
// (npm run api:spec && npm run api:gen) e migre para src/api/generated.

export interface ApiResponse<T> {
  data: T;
  meta?: PageMeta;
}

export interface PageMeta {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ApiErrorBody {
  error: { code: string; message: string; path?: string };
}

// ---- Auth ----
export interface AuthResponse {
  userId: string;
  email: string;
  accessToken: string;
  expiresIn: number;
}

export interface CurrentUser {
  userId: string;
  email: string;
}

// ---- Curriculum ----
export type LessonStatus = 'not_started' | 'in_progress' | 'completed' | 'mastered';

export interface ModuleSummary {
  id: string;
  title: string;
  description: string | null;
  level: string;
  orderIndex: number;
  lessonCount: number;
  completedLessonCount: number;
  progressPercent: number;
}

export interface LessonSummary {
  id: string;
  title: string;
  orderIndex: number;
  estimatedMinutes: number;
  status: LessonStatus;
  bestScore: number;
  locked: boolean;
}

export type ExerciseType = 'multiple_choice' | 'fill_blank' | 'translation';

export interface ExercisePublic {
  id: string;
  orderIndex: number;
  type: ExerciseType;
  // payload varia por tipo (ver docs/content-contract.md); cru aqui.
  payload: Record<string, unknown>;
}

// Shapes do payload por tipo (ver docs/content-contract.md). Usados na renderização.
export interface McOption {
  index: number;
  text: string;
}
export interface McPayload {
  prompt: string;
  audio_url?: string;
  options: McOption[];
}
export interface FillBlankPayload {
  sentence_template: string;
  hint?: string;
  audio_url?: string;
}
export interface TranslationPayload {
  source_text: string;
}

export interface LessonDetail {
  id: string;
  moduleId: string;
  title: string;
  content: Record<string, unknown>;
  exercises: ExercisePublic[];
}

// ---- Progress ----
export interface AttemptItem {
  exerciseId: string;
  userAnswer: Record<string, unknown>;
  timeSpentMs?: number;
}

export interface SubmitAttemptsRequest {
  lessonId: string;
  attempts: AttemptItem[];
}

export interface AttemptResult {
  exerciseId: string;
  isCorrect: boolean;
  xpEarned: number;
  correctAnswer?: Record<string, unknown>;
  feedback?: string;
}

export interface LessonProgressSummary {
  status: LessonStatus;
  currentScore: number;
  exercisesRemaining: number;
}

export interface SubmitAttemptsResponse {
  results: AttemptResult[];
  lessonProgress: LessonProgressSummary;
}

export interface ModuleProgress {
  moduleId: string;
  title: string;
  progressPercent: number;
  nextLessonId: string | null;
}

export interface ProgressDashboard {
  userId: string;
  totalXp: number;
  streakDays: number;
  currentLevel: string;
  modules: ModuleProgress[];
  recentErrors: unknown[];
}
