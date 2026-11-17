import { RouteObject, Navigate } from "react-router-dom";
import { OpsLoginPage } from "../features/ops/OpsLoginPage";
import { OpsPage } from "../features/ops/OpsPage";
import { OpsFarmDetailPage } from "../features/ops/OpsFarmDetailPage";
import { LandingPage } from "../features/landing/LandingPage";
import { PrivacyPage } from "../features/landing/PrivacyPage";
import { TermsPage } from "../features/landing/TermsPage";
import { DashboardPage } from "../features/dashboard/DashboardPage";
import { AnimalsPage } from "../features/animals/AnimalsPage";
import { AnimalDetailPage } from "../features/animals/AnimalDetailPage";
import { LotsPage } from "../features/animals/LotsPage";
import { CallbackPage } from "../features/auth/CallbackPage";
import { FeedingPage } from "../features/feeding/FeedingPage";
import { LoginPage } from "../features/auth/LoginPage";
import { PermissionRoute } from "../features/auth/PermissionRoute";
import { ProtectedRoute } from "../features/auth/ProtectedRoute";
import { HealthPage } from "../features/health/HealthPage";
import { MedicationsPage } from "../features/health/MedicationsPage";
import { OccurrencesPage } from "../features/health/OccurrencesPage";
import { VaccinesPage } from "../features/health/VaccinesPage";
import { ReportsPage } from "../features/reports/ReportsPage";
import { AdminShellLayout } from "../features/shell/AdminShellLayout";
import { BillingPage } from "../features/billing/BillingPage";
import { OffersPage } from "../features/offers/OffersPage";
import { SettingsPage } from "../features/settings/SettingsPage";
import { UsersPage } from "../features/users/UsersPage";
import { WeighingsPage } from "../features/weighings/WeighingsPage";

export const appRoutes: RouteObject[] = [
  {
    path: "/",
    element: <LandingPage />
  },
  {
    path: "/app",
    element: (
      <ProtectedRoute>
        <AdminShellLayout />
      </ProtectedRoute>
    ),
    children: [
      {
        index: true,
        element: (
          <PermissionRoute requiredPermissions={["dashboard.read"]}>
            <DashboardPage />
          </PermissionRoute>
        )
      },
      {
        path: "animals",
        element: (
          <PermissionRoute requiredPermissions={["animals.read"]}>
            <AnimalsPage />
          </PermissionRoute>
        )
      },
      {
        path: "lots",
        element: (
          <PermissionRoute requiredPermissions={["lots.read"]}>
            <LotsPage />
          </PermissionRoute>
        )
      },
      {
        path: "animals/feedings",
        element: (
          <PermissionRoute requiredPermissions={["animals.read"]}>
            <FeedingPage />
          </PermissionRoute>
        )
      },
      {
        path: "animals/weighings",
        element: (
          <PermissionRoute requiredPermissions={["animals.read"]}>
            <WeighingsPage />
          </PermissionRoute>
        )
      },
      {
        path: "animals/:animalId",
        element: (
          <PermissionRoute requiredPermissions={["animals.read"]}>
            <AnimalDetailPage />
          </PermissionRoute>
        )
      },
      {
        path: "health",
        element: (
          <PermissionRoute requiredPermissions={["occurrences.write", "vaccines.write", "medications.write", "reports.health.read"]}>
            <HealthPage />
          </PermissionRoute>
        )
      },
      {
        path: "health/occurrences",
        element: (
          <PermissionRoute requiredPermissions={["occurrences.write"]}>
            <OccurrencesPage />
          </PermissionRoute>
        )
      },
      {
        path: "health/vaccines",
        element: (
          <PermissionRoute requiredPermissions={["vaccines.write"]}>
            <VaccinesPage />
          </PermissionRoute>
        )
      },
      {
        path: "health/medications",
        element: (
          <PermissionRoute requiredPermissions={["medications.write"]}>
            <MedicationsPage />
          </PermissionRoute>
        )
      },
      {
        path: "offers",
        element: (
          <PermissionRoute requiredPermissions={["offers.read"]}>
            <OffersPage />
          </PermissionRoute>
        )
      },
      {
        path: "reports",
        element: (
          <PermissionRoute requiredPermissions={["reports.read", "reports.health.read"]}>
            <ReportsPage />
          </PermissionRoute>
        )
      },
      {
        path: "users",
        element: (
          <PermissionRoute requiredPermissions={["users.read"]}>
            <UsersPage />
          </PermissionRoute>
        )
      },
      {
        path: "billing",
        element: (
          <PermissionRoute requiredPermissions={["settings.read"]}>
            <BillingPage />
          </PermissionRoute>
        )
      },
      {
        path: "settings",
        element: (
          <PermissionRoute requiredPermissions={["settings.read"]}>
            <SettingsPage />
          </PermissionRoute>
        )
      }
    ]
  },
  {
    path: "/login",
    element: <LoginPage />
  },
  {
    path: "/auth/callback",
    element: <CallbackPage />
  },
  {
    path: "/ops/login",
    element: <OpsLoginPage />
  },
  {
    path: "/ops/farms/:farmId",
    element: <OpsFarmDetailPage />
  },
  {
    path: "/ops",
    element: <OpsPage />
  },
  {
    path: "/privacy",
    element: <PrivacyPage />
  },
  {
    path: "/terms",
    element: <TermsPage />
  },
  {
    path: "*",
    element: <Navigate replace to="/app" />
  }
];
