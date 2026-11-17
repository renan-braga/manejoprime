import { SessionMe } from "./AuthContext";

export function hasAnyPermission(session: SessionMe | null, requiredPermissions: string[]) {
  if (requiredPermissions.length === 0) {
    return true;
  }

  const permissions = session?.permissions ?? [];
  return requiredPermissions.some((permission) => permissions.includes(permission));
}
