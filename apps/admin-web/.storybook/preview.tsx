import type { Preview } from "@storybook/react";
import "../src/styles.css";

const preview: Preview = {
  parameters: {
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
    backgrounds: {
      default: "light",
      values: [
        { name: "light", value: "#fafaf9" },
        { name: "dark", value: "#1c1917" },
      ],
    },
  },
  globalTypes: {
    theme: {
      description: "Tema global",
      toolbar: {
        title: "Tema",
        icon: "circlehollow",
        items: ["light", "dark"],
        dynamicTitle: true,
      },
    },
  },
  decorators: [
    (Story, context) => {
      const theme = context.globals["theme"] ?? "light";
      return (
        <div
          className={theme === "dark" ? "dark" : ""}
          style={{
            minHeight: "100vh",
            padding: "1.5rem",
            background: "var(--background)",
            color: "var(--foreground)",
          }}
        >
          <Story />
        </div>
      );
    },
  ],
};

export default preview;
