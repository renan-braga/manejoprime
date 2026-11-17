import { Navigate } from "react-router-dom";
import { ReactNode } from "react";
import { useAuth } from "./AuthContext";

type ProtectedRouteProps = {
  children: ReactNode;
};

export function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { status } = useAuth();

  if (status === "loading") {
    return (
      <main className="min-h-screen bg-background flex items-center justify-center">
        <p className="text-sm text-muted-foreground">Restaurando sessão...</p>
      </main>
    );
  }

  if (status !== "authenticated") {
    return <Navigate replace to="/login" />;
  }

  return <>{children}</>;
}
