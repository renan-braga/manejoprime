import React, { useState } from "react";
import { useAuth } from "./AuthContext";
import { buildOAuthUrl, type OAuthProvider } from "./oauthPkce";
import { readGoogleClientId, readFacebookAppId, readMicrosoftClientId } from "../../config/runtimeConfig";
import { Button } from "@/components/ui/button";
import { BrandLogo } from "@/components/BrandLogo";

const REDIRECT_URI = `${window.location.origin}/auth/callback`;

function GoogleIcon() {
  return (
    <svg viewBox="0 0 24 24" className="w-5 h-5 shrink-0" aria-hidden="true">
      <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
      <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
      <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z" />
      <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
    </svg>
  );
}

function MicrosoftIcon() {
  return (
    <svg viewBox="0 0 24 24" className="w-5 h-5 shrink-0" aria-hidden="true">
      <path fill="#F25022" d="M0 0H11.4V11.4H0z" />
      <path fill="#7FBA00" d="M12.6 0H24V11.4H12.6z" />
      <path fill="#00A4EF" d="M0 12.6H11.4V24H0z" />
      <path fill="#FFB900" d="M12.6 12.6H24V24H12.6z" />
    </svg>
  );
}

function FacebookIcon() {
  return (
    <svg viewBox="0 0 24 24" className="w-5 h-5 shrink-0" aria-hidden="true">
      <path fill="#1877F2" d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z" />
    </svg>
  );
}

type OAuthButtonConfig = {
  provider: OAuthProvider;
  label: string;
  clientId: string;
  icon: React.ReactNode;
};

const OAUTH_BUTTONS: OAuthButtonConfig[] = [
  { provider: "google", label: "Google", clientId: readGoogleClientId(), icon: <GoogleIcon /> },
  { provider: "microsoft", label: "Microsoft", clientId: readMicrosoftClientId(), icon: <MicrosoftIcon /> },
  { provider: "facebook", label: "Facebook", clientId: readFacebookAppId(), icon: <FacebookIcon /> },
];

export function LoginPage() {
  const { authFailureReason, clearAuthFailureReason } = useAuth();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [pending, setPending] = useState<OAuthProvider | null>(null);

  async function handleOAuth(provider: OAuthProvider, clientId: string) {
    if (!clientId) {
      setErrorMessage(`Login com ${provider} não configurado.`);
      return;
    }
    clearAuthFailureReason();
    setErrorMessage(null);
    setPending(provider);
    try {
      const url = await buildOAuthUrl(provider, clientId, REDIRECT_URI);
      window.location.href = url;
    } catch {
      setPending(null);
      setErrorMessage("Não foi possível iniciar o login. Tente novamente.");
    }
  }

  return (
    <div className="min-h-screen bg-background flex">
      {/* Left — form */}
      <div className="flex-1 flex items-center justify-center p-8">
        <div className="w-full max-w-sm">
          <a
            href="/"
            aria-label="Ir para a landing page"
            className="mb-8 inline-flex items-center gap-3 rounded-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
          >
            <BrandLogo className="w-14 h-14" />
            <span className="font-heading font-bold text-3xl tracking-tight">ManejoPrime</span>
          </a>

          <div className="space-y-6">
            <div>
              <h2 className="font-heading text-xl font-bold">Acesse sua conta</h2>
              <p className="text-sm text-muted-foreground mt-1">
                Use Google, Microsoft ou Facebook. Para acessar uma fazenda, escolha a conta com o e-mail cadastrado.
              </p>
            </div>

            <div className="space-y-3">
              {OAUTH_BUTTONS.map(({ provider, label, clientId, icon }) => (
                <Button
                  key={provider}
                  variant="outline"
                  className="w-full gap-3 py-5 text-sm font-medium justify-start px-4"
                  disabled={pending != null}
                  onClick={() => void handleOAuth(provider, clientId)}
                  aria-label={label}
                >
                  {icon}
                  {pending === provider ? "Redirecionando..." : label}
                </Button>
              ))}
            </div>

            {errorMessage != null && <p className="text-sm text-destructive">{errorMessage}</p>}
            {authFailureReason != null && <p className="text-sm text-destructive">{authFailureReason}</p>}

            <p className="text-xs text-muted-foreground text-center leading-relaxed">
              Sem vínculo com a fazenda? Peça ao administrador para cadastrar seu e-mail.
            </p>
          </div>
        </div>
      </div>

      {/* Right — visual panel */}
      <div className="hidden lg:flex w-[480px] bg-gradient-to-br from-emerald-800 to-emerald-950 items-center justify-center p-12 relative overflow-hidden">
        <div
          className="absolute inset-0 opacity-10"
          style={{
            backgroundImage:
              "url(\"data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.4'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C%2Fg%3E%3C/svg%3E\")",
          }}
        />
        <div className="relative text-center text-white space-y-6">
          <BrandLogo className="w-16 h-16 mx-auto drop-shadow-xl text-white" />
          <h1 className="font-heading text-3xl font-bold">Acesso seguro à sua fazenda</h1>
          <p className="text-emerald-200 text-sm leading-relaxed max-w-xs mx-auto">
            Você pode entrar com uma conta comum. Para ver os dados da fazenda, ela precisa usar o mesmo e-mail cadastrado no convite.
          </p>
          <ul className="mx-auto max-w-xs space-y-3 pt-2 text-left text-sm text-emerald-100">
            <li className="rounded-md border border-white/10 bg-white/5 px-4 py-3">Login com Google, Microsoft ou Facebook</li>
            <li className="rounded-md border border-white/10 bg-white/5 px-4 py-3">Vínculo com a fazenda pelo e-mail cadastrado</li>
            <li className="rounded-md border border-white/10 bg-white/5 px-4 py-3">Permissões liberadas conforme sua função</li>
            <li className="rounded-md border border-white/10 bg-white/5 px-4 py-3">Painel, rebanho e registros após o vínculo</li>
          </ul>
        </div>
      </div>
    </div>
  );
}
