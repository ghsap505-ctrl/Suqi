/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      fontFamily: {
        display: ['"Cairo"', "system-ui", "sans-serif"],
        sans: ['"IBM Plex Sans Arabic"', '"Cairo"', "system-ui", "sans-serif"],
      },
      colors: {
        souqi: {
          teal: "#0F4C5C",
          tealLight: "#1F6E82",
          amber: "#E09F3E",
          terracotta: "#C8553D",
          emerald: "#1B7F5A",
          sand: "#FAF7F2",
          sandCard: "#FFFFFF",
          whatsapp: "#128C7E",
        },
      },
    },
  },
  plugins: [],
};
