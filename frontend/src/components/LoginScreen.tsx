import { Sparkles } from 'lucide-react';
import { useState, type FormEvent } from 'react';
import { ApiError } from '../api/http';
import { useAuth } from '../auth/AuthContext';

export function LoginScreen() {
  const { login, register } = useAuth();
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      if (mode === 'login') await login(email, password);
      else await register(email, password);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Falha ao autenticar');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-[#131314] p-6">
      <div className="w-full max-w-sm rounded-3xl border border-[#3C4043] bg-[#1E1E1F] p-8 shadow-2xl">
        <div className="mb-8 flex flex-col items-center">
          <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-blue-500 to-purple-600 shadow-[0_0_25px_rgba(197,138,249,0.25)]">
            <Sparkles className="h-8 w-8 text-white" />
          </div>
          <h1 className="bg-gradient-to-r from-blue-400 to-purple-400 bg-clip-text text-xl font-bold text-transparent">
            Aprenda Inglês
          </h1>
          <p className="mt-1 text-sm text-[#9AA0A6]">
            {mode === 'login' ? 'Entre para continuar' : 'Crie sua conta'}
          </p>
        </div>

        <form onSubmit={submit} className="space-y-4">
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="seu@email.com"
            className="w-full rounded-xl border border-[#3C4043] bg-[#131314] px-4 py-3 text-sm text-[#E8EAED] placeholder-[#9AA0A6] outline-none focus:border-blue-500"
          />
          <input
            type="password"
            required
            minLength={12}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Senha (mín. 12 caracteres)"
            className="w-full rounded-xl border border-[#3C4043] bg-[#131314] px-4 py-3 text-sm text-[#E8EAED] placeholder-[#9AA0A6] outline-none focus:border-blue-500"
          />

          {error && (
            <p className="rounded-lg border border-red-500/30 bg-red-500/10 px-3 py-2 text-xs text-red-300">
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={busy}
            className="w-full rounded-xl bg-gradient-to-r from-blue-600 to-purple-600 py-3 text-sm font-semibold text-white transition hover:opacity-90 disabled:opacity-50"
          >
            {busy ? '...' : mode === 'login' ? 'Entrar' : 'Criar conta'}
          </button>
        </form>

        <button
          onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setError(null); }}
          className="mt-6 w-full text-center text-xs text-[#9AA0A6] transition hover:text-[#E8EAED]"
        >
          {mode === 'login' ? 'Não tem conta? Cadastre-se' : 'Já tem conta? Entrar'}
        </button>
      </div>
    </div>
  );
}
