import { Lock } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@/features/auth/AuthContext";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type { ComponentPropsWithoutRef } from "react";

type Props = ComponentPropsWithoutRef<typeof Button>;

export function BillingGatedButton({ onClick, children, className, ...props }: Props) {
  const { session } = useAuth();
  const navigate = useNavigate();
  const blocked = !!(session?.billing && !session.billing.billingExempt && !session.billing.canWrite);

  if (blocked) {
    return (
      <Button
        {...props}
        type="button"
        className={cn(className, "opacity-60")}
        onClick={() => navigate("/app/billing#checkout")}
        title="Regularize seu plano para habilitar esta ação"
      >
        <Lock className="h-3.5 w-3.5" />
        {children}
      </Button>
    );
  }

  return (
    <Button {...props} className={className} onClick={onClick}>
      {children}
    </Button>
  );
}
