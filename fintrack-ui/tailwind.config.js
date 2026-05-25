/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.{html,ts}"],
  theme: {
    extend: {
      colors: {
        navy: {
          950: '#040D1A',
          900: '#071020',
          800: '#0A1628',
          700: '#0F1E35',
          600: '#162640',
          500: '#1D3050',
        },
        gold: {
          300: '#E8D090',
          400: '#D4B86A',
          500: '#C4A352',
          600: '#A8883A',
          700: '#8C6E28',
        },
        slate: {
          muted: '#5A7090',
          light: '#8A9BB5',
          text: '#E8E4D9',
        }
      },
      fontFamily: {
        display: ['Playfair Display', 'serif'],
        sans: ['DM Sans', 'sans-serif'],
      }
    },
  },
  plugins: [],
}
