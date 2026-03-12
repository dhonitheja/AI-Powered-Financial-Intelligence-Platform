import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        primary: {
          DEFAULT: "#0D0B1E", // Near black
          foreground: "#FFFFFF",
        },
        secondary: {
          DEFAULT: "#D4AF37", // Gold
          foreground: "#0D0B1E",
        },
        accent: {
          DEFAULT: "#1E1B4B", // Deep indigo
          foreground: "#FFFFFF",
        },
        gold: {
          light: "#F5D67B",
          DEFAULT: "#D4AF37",
          dark: "#B8962E",
        },
        success: "#10B981",
        warning: "#F59E0B",
        danger: "#E11D48",
        muted: {
          DEFAULT: "#64748B",
          foreground: "#F8FAFC",
        },
      },
      borderRadius: {
        lg: "var(--radius)",
        md: "calc(var(--radius) - 2px)",
        sm: "calc(var(--radius) - 4px)",
      },
      boxShadow: {
        soft: "0 2px 15px -3px rgba(0, 0, 0, 0.07), 0 10px 20px -2px rgba(0, 0, 0, 0.04)",
        gold: "0 4px 24px rgba(212,175,55,0.18)",
        "gold-lg": "0 8px 40px rgba(212,175,55,0.25)",
      }
    },
  },
  plugins: [require("tailwindcss-animate")],
};
export default config;
