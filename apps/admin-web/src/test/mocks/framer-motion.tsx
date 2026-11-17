import React, { forwardRef } from "react";

const FRAMER_PROPS = new Set([
  "animate", "initial", "exit", "transition", "variants",
  "whileHover", "whileTap", "whileFocus", "whileInView", "whileDrag",
  "layout", "layoutId", "layoutScroll", "layoutRoot",
  "drag", "dragConstraints", "dragElastic", "dragMomentum",
  "dragSnapToOrigin", "dragTransition", "dragDirectionLock",
  "onDragStart", "onDragEnd", "onDrag", "onDragOver",
  "onAnimationStart", "onAnimationComplete",
  "onUpdate", "onTap", "onTapStart", "onTapCancel",
  "onHoverStart", "onHoverEnd",
  "transformTemplate", "custom", "inherit",
  "onViewportEnter", "onViewportLeave", "viewport",
]);

function createMotionElement(tag: string) {
  const Component = forwardRef<HTMLElement, Record<string, unknown>>(
    (rawProps, ref) => {
      const { children, ...rest } = rawProps;
      const filtered: Record<string, unknown> = { ref };
      for (const key in rest) {
        if (!FRAMER_PROPS.has(key)) {
          filtered[key] = rest[key];
        }
      }
      return React.createElement(tag, filtered, children as React.ReactNode);
    }
  );
  Component.displayName = `motion.${tag}`;
  return Component;
}

export const motion = new Proxy(
  {} as Record<string, ReturnType<typeof createMotionElement>>,
  {
    get(target, prop: string) {
      if (!(prop in target)) {
        target[prop] = createMotionElement(prop);
      }
      return target[prop];
    },
  }
);

export const AnimatePresence = ({ children }: { children?: React.ReactNode }) => (
  <>{children}</>
);

export const MotionConfig = ({ children }: { children?: React.ReactNode }) => (
  <>{children}</>
);

export const LayoutGroup = ({ children }: { children?: React.ReactNode }) => (
  <>{children}</>
);

export const useAnimation = () => ({
  start: () => Promise.resolve(),
  stop: () => {},
  set: () => {},
});

export const useMotionValue = (initial: number) => ({
  get: () => initial,
  set: () => {},
  onChange: () => () => {},
});

export const useTransform = () => ({ get: () => 0 });

export const useSpring = (value: number) => ({
  get: () => value,
  set: () => {},
});

export const useReducedMotion = () => false;
