import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render } from "@testing-library/react";
import { ReactElement, StrictMode, cloneElement } from "react";
import { MemoryRouter, RouteObject, useRoutes } from "react-router-dom";
import { afterEach, vi } from "vitest";
import { appRoutes } from "../../app/appRoutes";
import { AuthProvider, AuthStatus, SessionMe } from "../../features/auth/AuthContext";
import { ThemeProvider } from "../../features/theme/ThemeContext";

const activeClients: QueryClient[] = [];
afterEach(() => {
  activeClients.forEach(qc => qc.clear());
  activeClients.length = 0;
});

type TestAuthTokens = {
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresAt?: string;
  refreshTokenExpiresAt?: string;
};

type RenderAuthOptions = {
  status?: AuthStatus;
  session?: Partial<SessionMe> | SessionMe | null;
  tokens?: Partial<TestAuthTokens> | TestAuthTokens | null;
  strictMode?: boolean;
};

type RenderRouteOptions = RenderAuthOptions & {
  initialEntries?: string[];
  routes?: RouteObject[];
  restoreFromStorage?: boolean;
};

const AUTH_STORAGE_KEY = "curralzap.admin.auth";

const DEFAULT_SESSION: SessionMe = {
  userId: "user-1",
  name: "Admin Web Test User",
  email: "test@example.com",
  phone: "5511999990001",
  role: "admin",
  farmId: "farm-1",
  farmName: "Fazenda Teste",
  permissions: ["dashboard.read"]
};

const DEFAULT_TOKENS: TestAuthTokens = {
  accessToken: "test-access-token",
  refreshToken: "test-refresh-token"
};

export function buildSession(overrides: Partial<SessionMe> = {}): SessionMe {
  return {
    ...DEFAULT_SESSION,
    ...overrides
  };
}

export function buildTokens(overrides: Partial<TestAuthTokens> = {}): TestAuthTokens {
  return {
    ...DEFAULT_TOKENS,
    ...overrides
  };
}

export function storeAuthTokens(overrides: Partial<TestAuthTokens> = {}) {
  const tokens = buildTokens(overrides);
  window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(tokens));
  return tokens;
}

export function renderWithAuth(ui: ReactElement, options: RenderAuthOptions = {}) {
  const status = options.status ?? "authenticated";
  const session =
    options.session === null
      ? null
      : buildSession((options.session ?? {}) as Partial<SessionMe>);
  const tokens =
    options.tokens === null
      ? null
      : buildTokens((options.tokens ?? {}) as Partial<TestAuthTokens>);

  return render(
    withProviders(
      ui,
      <AuthProvider
        initialStatus={status}
        initialSession={session}
        initialTokens={status === "authenticated" ? tokens : null}
      />,
      options.strictMode
    )
  );
}

export function renderWithBootstrap(ui: ReactElement, options: Pick<RenderAuthOptions, "strictMode"> = {}) {
  return render(withProviders(ui, <AuthProvider />, options.strictMode));
}

export function renderAppRoutes(options: RenderRouteOptions = {}) {
  const ui = (
    <MemoryRouter
      future={{ v7_relativeSplatPath: true }}
      initialEntries={options.initialEntries ?? ["/app"]}
    >
      <TestRouteRenderer routes={options.routes ?? appRoutes} />
    </MemoryRouter>
  );

  if (options.restoreFromStorage) {
    return renderWithBootstrap(ui, { strictMode: options.strictMode });
  }

  return renderWithAuth(ui, options);
}

export function jsonResponse(body: unknown, init: ResponseInit = { status: 200 }) {
  return new Response(JSON.stringify(body), init);
}

// Onboarding status that tells the shell: wizard not needed (farm already has data).
const ONBOARDING_NOOP_RESPONSE = jsonResponse({
  adminHasPhone: true,
  hasLots: true,
  hasAnimals: true,
  hasVaccines: true,
  animalCount: 0,
  animalPlanLimit: null
});

function isOnboardingStatusUrl(input: RequestInfo | URL): boolean {
  const url = typeof input === "string" ? input : (input as Request).url ?? input.toString();
  return url.includes("/api/v1/onboarding/status");
}

export function setMockFetch(...responses: Array<Response | Promise<Response>>) {
  const queue = [...responses];
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const mock = vi.fn<any>().mockImplementation(async (...args: unknown[]) => {
    if (isOnboardingStatusUrl(args[0] as RequestInfo | URL)) return ONBOARDING_NOOP_RESPONSE.clone();
    return queue.shift() ?? new Response(null, { status: 500 });
  });
  vi.stubGlobal("fetch", mock);
  return mock;
}

export function setPendingFetch() {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const mock = vi.fn<any>().mockImplementation((...args: unknown[]) => {
    if (isOnboardingStatusUrl(args[0] as RequestInfo | URL)) {
      return Promise.resolve(ONBOARDING_NOOP_RESPONSE.clone());
    }
    return new Promise<Response>(() => undefined);
  });
  vi.stubGlobal("fetch", mock);
  return mock;
}

function withProviders(ui: ReactElement, authProvider: ReactElement, strictMode = false) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: 0
      },
      mutations: {
        retry: false
      }
    }
  });
  activeClients.push(queryClient);

  const providerTree = (
    <ThemeProvider switchable>
      <QueryClientProvider client={queryClient}>
        {cloneElement(authProvider, undefined, ui)}
      </QueryClientProvider>
    </ThemeProvider>
  );

  return strictMode ? <StrictMode>{providerTree}</StrictMode> : providerTree;
}

function TestRouteRenderer({ routes }: { routes: RouteObject[] }) {
  return useRoutes(routes);
}
