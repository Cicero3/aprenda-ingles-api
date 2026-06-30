import { useState } from 'react';
import { LoginScreen } from './components/LoginScreen';
import { ModulesDashboard } from './components/ModulesDashboard';
import { LessonsList } from './components/LessonsList';
import { LessonScreen } from './components/LessonScreen';
import { useAuth } from './auth/AuthContext';

type View =
  | { name: 'modules' }
  | { name: 'lessons'; moduleId: string; moduleTitle: string }
  | { name: 'lesson'; lessonId: string; moduleId: string; moduleTitle: string };

export default function App() {
  const { user, loading } = useAuth();
  const [view, setView] = useState<View>({ name: 'modules' });

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#131314] text-[#9AA0A6]">
        Carregando…
      </div>
    );
  }

  if (!user) return <LoginScreen />;

  switch (view.name) {
    case 'lessons':
      return (
        <LessonsList
          moduleId={view.moduleId}
          moduleTitle={view.moduleTitle}
          onBack={() => setView({ name: 'modules' })}
          onOpenLesson={(lessonId) =>
            setView({ name: 'lesson', lessonId, moduleId: view.moduleId, moduleTitle: view.moduleTitle })
          }
        />
      );
    case 'lesson':
      return (
        <LessonScreen
          lessonId={view.lessonId}
          onBack={() =>
            setView({ name: 'lessons', moduleId: view.moduleId, moduleTitle: view.moduleTitle })
          }
        />
      );
    default:
      return (
        <ModulesDashboard
          onOpenModule={(moduleId, moduleTitle) => setView({ name: 'lessons', moduleId, moduleTitle })}
        />
      );
  }
}
