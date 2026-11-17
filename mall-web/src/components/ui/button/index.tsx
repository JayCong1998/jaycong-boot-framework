import type { ButtonHTMLAttributes } from "react";
import { cn } from "@/lib/utils";
import { buttonVariants, type ButtonVariants } from "./variants";
export function Button({
  className,
  variant,
  size,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & ButtonVariants) {
  return (
    <button
      data-slot="button"
      data-variant={variant ?? undefined}
      data-size={size ?? undefined}
      className={cn(buttonVariants({ variant, size }), className)}
      {...props}
    />
  );
}
