import { Navigate } from "react-router-dom";
import { ReactNode } from "react";
import { useAuth } from "./AuthContext";

type PermissionRouteProps = {
  children: ReactNode;
  requiredPermissions: string[];
};

export function PermissionRoute({ children, requiredPermissions }: PermissionRouteProps) {
  const { session } = useAuth();
  const permissions = session?.permissions ?? [];
  const allowed = requiredPermissions.some((permission) => permissions.includes(permission));

  if (!allowed) {
    return <Navigate replace to="/" />;
  }

  return <>{children}</>;
}
