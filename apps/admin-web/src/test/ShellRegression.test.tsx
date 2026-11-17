import { act, fireEvent, renderHook, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";
import { ThemeProvider, useTheme } from "../features/theme/ThemeContext";
import { buildSession, renderAppRoutes, setPendingFetch } from "./support/adminWebTestSupport";

const ADMIN_PERMISSIONS = [
  "dashboard.read",
  "animals.read",
  "occurrences.write",
  "vaccines.write",
  "medications.write",
  "offers.read",
  "reports.read",
  "users.read",
  "settings.read"
];

const WORKER_PERMISSIONS = ["dashboard.read", "animals.read", "occurrences.write"];

function setViewportWidth(width: number) {
  Object.defineProperty(window, "innerWidth", {
    configurable: true,
    writable: true,
    value: width
  });
}

afterEach(() => {
  setViewportWidth(1024);
});

describe("admin shell layout", () => {
  it("renders all nav items for admin role", async () => {
    const session = buildSession({ role: "admin", permissions: ADMIN_PERMISSIONS });
    setPendingFetch();
    renderAppRoutes({ session });

    await waitFor(() => {
      expect(screen.getByRole("link", { name: "Painel" })).toBeInTheDocument();
    });
    expect(screen.getByRole("link", { name: "Animais" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Ocorrências" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Ofertas" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Relatórios" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Usuários" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Configurações" })).toBeInTheDocument();
  });

  it("hides Reports nav item for worker role", async () => {
    const session = buildSession({ role: "worker", permissions: WORKER_PERMISSIONS });
    setPendingFetch();
    renderAppRoutes({ session });

    await waitFor(() => {
      expect(screen.getByRole("link", { name: "Painel" })).toBeInTheDocument();
    });
    expect(screen.queryByRole("link", { name: "Relatórios" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Usuários" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Configurações" })).not.toBeInTheDocument();
  });

  it("collapse toggle sets sidebar to collapsed state", async () => {
    const session = buildSession({ permissions: ["dashboard.read"] });
    setPendingFetch();
    renderAppRoutes({ session });

    await waitFor(() => {
      expect(screen.getByRole("link", { name: "Painel" })).toBeInTheDocument();
    });

    const sidebar = screen.getByRole("complementary");
    expect(sidebar).toHaveAttribute("data-collapsed", "false");

    fireEvent.click(screen.getByRole("button", { name: /recolher menu/i }));
    expect(sidebar).toHaveAttribute("data-collapsed", "true");
  });

  it("starts with sidebar collapsed on mobile viewport", async () => {
    setViewportWidth(390);
    const session = buildSession({ permissions: ["dashboard.read"] });
    setPendingFetch();
    renderAppRoutes({ session });

    await waitFor(() => {
      expect(screen.getByRole("complementary")).toHaveAttribute("data-collapsed", "true");
    });
    expect(screen.getByRole("button", { name: /expandir menu/i })).toBeInTheDocument();
  });

  it("keeps top header content constrained for narrow screens", async () => {
    const longFarmName = "Fazenda Santa Maria do Vale Comprido com Nome Operacional Extenso";
    const session = buildSession({
      farmName: longFarmName,
      role: "veterinarian",
      permissions: ["dashboard.read"]
    });
    setPendingFetch();
    renderAppRoutes({ session });

    await waitFor(() => {
      expect(screen.getByText(longFarmName)).toBeInTheDocument();
    });
    expect(screen.getByText(longFarmName)).toHaveClass("min-w-0", "truncate");
    expect(screen.getByText("Veterinário")).toHaveClass("hidden", "sm:inline");
  });

  it("dark-mode toggle adds dark class to document root", async () => {
    const session = buildSession({ permissions: ["dashboard.read"] });
    setPendingFetch();
    renderAppRoutes({ session });

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /alternar modo escuro/i })).toBeInTheDocument();
    });

    expect(document.documentElement.classList.contains("dark")).toBe(false);
    fireEvent.click(screen.getByRole("button", { name: /alternar modo escuro/i }));
    expect(document.documentElement.classList.contains("dark")).toBe(true);
  });
});

describe("admin shell routes", () => {
  it("shows only the navigation groups allowed by the current permissions", async () => {
    const session = buildSession({
      userId: "user-1",
      name: "Restricted Admin",
      phone: "5511999990001",
      role: "worker",
      farmId: "farm-1",
      farmName: "Fazenda Restrita",
      permissions: ["dashboard.read", "animals.read"]
    });

    setPendingFetch();

    renderAppRoutes({ session });

    await waitFor(() => {
      expect(screen.getByRole("link", { name: "Painel" })).toBeInTheDocument();
    });
    expect(screen.getByRole("link", { name: "Animais" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Saúde" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Usuários" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Configurações" })).not.toBeInTheDocument();
  });

  it("redirects forbidden routes back to the dashboard", async () => {
    const session = buildSession({
      userId: "user-2",
      name: "Vet User",
      phone: "5511999990002",
      role: "veterinarian",
      farmId: "farm-1",
      farmName: "Fazenda Vet",
      permissions: ["dashboard.read", "animals.read", "reports.health.read", "occurrences.write"]
    });

    setPendingFetch();

    renderAppRoutes({ initialEntries: ["/app/users"], session });

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: "Dashboard" })).toBeInTheDocument();
    });
    expect(screen.queryByText("Users route group is reserved.")).not.toBeInTheDocument();
  });
});

describe("ThemeContext", () => {
  it("defaults to light theme and does not apply dark class", () => {
    const { result } = renderHook(() => useTheme(), {
      wrapper: ({ children }) => <ThemeProvider>{children}</ThemeProvider>
    });

    expect(result.current.theme).toBe("light");
    expect(document.documentElement.classList.contains("dark")).toBe(false);
  });

  it("toggles to dark theme and applies the dark class to the document root", () => {
    const { result } = renderHook(() => useTheme(), {
      wrapper: ({ children }) => <ThemeProvider switchable>{children}</ThemeProvider>
    });

    act(() => {
      result.current.toggleTheme?.();
    });

    expect(result.current.theme).toBe("dark");
    expect(document.documentElement.classList.contains("dark")).toBe(true);
  });

  it("toggles back to light theme and removes the dark class", () => {
    const { result } = renderHook(() => useTheme(), {
      wrapper: ({ children }) => <ThemeProvider switchable>{children}</ThemeProvider>
    });

    act(() => {
      result.current.toggleTheme?.();
    });
    act(() => {
      result.current.toggleTheme?.();
    });

    expect(result.current.theme).toBe("light");
    expect(document.documentElement.classList.contains("dark")).toBe(false);
  });

  it("persists theme preference in localStorage when switchable", () => {
    const { result } = renderHook(() => useTheme(), {
      wrapper: ({ children }) => <ThemeProvider switchable>{children}</ThemeProvider>
    });

    act(() => {
      result.current.toggleTheme?.();
    });

    expect(window.localStorage.getItem("theme")).toBe("dark");
  });

  it("does not expose toggleTheme and reports not switchable when switchable is false", () => {
    const { result } = renderHook(() => useTheme(), {
      wrapper: ({ children }) => <ThemeProvider>{children}</ThemeProvider>
    });

    expect(result.current.toggleTheme).toBeUndefined();
    expect(result.current.switchable).toBe(false);
  });
});
