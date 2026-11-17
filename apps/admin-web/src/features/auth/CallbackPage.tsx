import { useEffect, useEffectEvent, useMemo, useRef, useState } from "react";
import { Navigate, useSearchParams } from "react-router-dom";
import { useAuth } from "./AuthContext";
import { consumeCodeVerifier, type OAuthProvider } from "./oauthPkce";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { describeApiError } from "@/lib/apiErrors";

const REDIRECT_URI = `${window.location.origin}/auth/callback`;

const VALID_PROVIDERS: OAuthProvider[] = ["google", "facebook", "microsoft"];

function isValidProvider(value: string | null): value is OAuthProvider {
  return VALID_PROVIDERS.includes(value as OAuthProvider);
}

type PagePhase =
  | { phase: "loading" }
  | { phase: "error"; message: string }
  | { phase: "onboarding"; onboardingToken: string };

export function CallbackPage() {
  const [searchParams] = useSearchParams();
  const { loginWithOAuth, registerFarm, status } = useAuth();
  const [pageState, setPageState] = useState<PagePhase>({ phase: "loading" });
  const consumedCodeRef = useRef<string | null>(null);

  const code = useMemo(() => searchParams.get("code"), [searchParams]);
  const state = useMemo(() => searchParams.get("state"), [searchParams]);
  const oauthError = useMemo(() => searchParams.get("error"), [searchParams]);

  const finishOAuth = useEffectEvent(async (provider: OAuthProvider, authCode: string) => {
    try {
      const codeVerifier = consumeCodeVerifier();
      if (codeVerifier == null) throw new Error("Missing PKCE code verifier.");
      const result = await loginWithOAuth(provider, authCode, codeVerifier, REDIRECT_URI);
      if (result.status === "onboarding_required") {
        setPageState({ phase: "onboarding", onboardingToken: result.onboardingToken });
      }
    } catch (error) {
      setPageState({ phase: "error", message: describeCallbackError(error) });
    }
  });

  useEffect(() => {
    if (oauthError != null) {
      setPageState({ phase: "error", message: "O login foi cancelado ou negado. Tente novamente." });
      return;
    }

    if (code == null || code.length === 0) {
      setPageState({ phase: "error", message: "Código de autorização ausente. Tente fazer login novamente." });
      return;
    }

    if (!isValidProvider(state)) {
      setPageState({ phase: "error", message: "Estado OAuth inválido. Tente fazer login novamente." });
      return;
    }

    if (consumedCodeRef.current === code) {
      return;
    }
    consumedCodeRef.current = code;

    void finishOAuth(state, code);
  }, [finishOAuth, code, state, oauthError]);

  if (status === "authenticated") {
    return <Navigate replace to="/app" />;
  }

  return (
    <main className="min-h-screen bg-background flex items-center justify-center p-4">
      <Card className="w-full max-w-md shadow-xl">
        <CardHeader>
          <span className="text-xs font-semibold tracking-widest uppercase text-muted-foreground mb-2">
            {pageState.phase === "onboarding" ? "Bem-vindo" : "Autenticação"}
          </span>
          <CardTitle className="text-xl">
            {pageState.phase === "loading" && "Finalizando seu login..."}
            {pageState.phase === "error" && "Não foi possível concluir o login."}
            {pageState.phase === "onboarding" && "Crie sua fazenda"}
          </CardTitle>
        </CardHeader>
        <CardContent>
          {pageState.phase === "loading" && (
            <p className="text-sm text-muted-foreground">
              Validando suas credenciais e carregando os dados da fazenda.
            </p>
          )}
          {pageState.phase === "error" && (
            <p className="text-sm text-muted-foreground">{pageState.message}</p>
          )}
          {pageState.phase === "onboarding" && (
            <FarmNameForm
              onboardingToken={pageState.onboardingToken}
              registerFarm={registerFarm}
            />
          )}
        </CardContent>
      </Card>
    </main>
  );
}

type FarmNameFormProps = {
  onboardingToken: string;
  registerFarm: (onboardingToken: string, farmName: string) => Promise<void>;
};

function FarmNameForm({ onboardingToken, registerFarm }: FarmNameFormProps) {
  const [farmName, setFarmName] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await registerFarm(onboardingToken, farmName);
    } catch (err) {
      setError(describeRegistrationError(err));
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4">
      <p className="text-sm text-muted-foreground">
        Para começar, escolha um nome para sua fazenda.
      </p>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="farmName">Nome da fazenda</Label>
        <Input
          id="farmName"
          value={farmName}
          onChange={(e) => setFarmName(e.target.value)}
          placeholder="Ex: Fazenda São João"
          disabled={submitting}
          autoFocus
        />
      </div>
      {error != null && (
        <p className="text-sm text-destructive">{error}</p>
      )}
      <Button type="submit" disabled={submitting || farmName.trim().length === 0}>
        {submitting ? "Criando fazenda..." : "Criar fazenda"}
      </Button>
    </form>
  );
}

function describeRegistrationError(error: unknown) {
  if (typeof error === "object" && error != null && "code" in error) {
    switch (error.code) {
      case "token_expired":
      case "authentication_failed":
        return "O tempo para criar sua fazenda expirou. Faça login novamente.";
      case "validation_failed":
        return describeApiError(error, "Nome da fazenda inválido.");
      default:
        break;
    }
  }
  return describeApiError(error, "Não foi possível criar a fazenda. Tente novamente.");
}

function describeCallbackError(error: unknown) {
  if (typeof error === "object" && error != null && "code" in error) {
    switch (error.code) {
      case "no_invitation":
        return "Sua conta não foi convidada para nenhuma fazenda. Peça ao administrador para enviar um convite.";
      case "access_revoked":
        return "Seu acesso foi revogado. Entre em contato com o administrador da fazenda.";
      case "inactive_user":
        return "Esta conta está inativa. Entre em contato com o administrador da fazenda.";
      case "oauth_verification_failed":
        return describeApiError(error, "Não foi possível validar sua conta no provedor de login. Tente novamente.");
      case "authentication_failed":
      case "token_expired":
        return "Sua sessão expirou durante o login. Tente fazer login novamente.";
      case "payment_required":
        return describeApiError(error, "Seu plano precisa ser regularizado para continuar.");
      default:
        break;
    }
  }

  return describeApiError(error, "Não foi possível concluir o fluxo de login. Tente novamente.");
}
