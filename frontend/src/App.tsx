import { LoginScreen } from './components/LoginScreen';
import { ModulesDashboard } from './components/ModulesDashboard';
import { useAuth } from './auth/AuthContext';

export default function App() {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#131314] text-[#9AA0A6]">
        Carregando…
      </div>
    );
  }

  return user ? <ModulesDashboard /> : <LoginScreen />;
}
