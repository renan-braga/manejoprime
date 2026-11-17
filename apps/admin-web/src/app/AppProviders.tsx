import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactNode, useState } from "react";
import { AuthProvider } from "../features/auth/AuthContext";
import { ThemeProvider } from "../features/theme/ThemeContext";
import { TooltipProvider } from "../components/ui/tooltip";

type AppProvidersProps = {
  children: ReactNode;
};

export function AppProviders({ children }: AppProvidersProps) {
  const [queryClient] = useState(() => new QueryClient());

  return (
    <ThemeProvider switchable>
      <TooltipProvider delayDuration={300}>
        <QueryClientProvider client={queryClient}>
          <AuthProvider>{children}</AuthProvider>
        </QueryClientProvider>
      </TooltipProvider>
    </ThemeProvider>
  );
}
