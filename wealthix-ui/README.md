# Antigravity Frontend 🚀

A production-grade, AI-powered financial intelligence dashboard. Built with Next.js 14, TypeScript, and Tailwind CSS.

## ✨ Features
- **AI-Risk Analysis**: Real-time fraud detection visualization powered by Gemini.
- **Microservice Architecture**: Connected to Java Spring Boot (Core) and Python FastAPI (AI).
- **Responsive Dashboard**: Recharts-powered analytics with premium SaaS aesthetics.
- **Glassmorphism UI**: Modern design with soft shadows and smooth animations.

## 🛠 Tech Stack
- **Framework**: Next.js 14 (App Router)
- **Styling**: Tailwind CSS
- **State**: Zustand
- **Charts**: Recharts
- **Icons**: Lucide React
- **Notifications**: Sonner

## 🚀 Getting Started

### 1. Prerequisites
- Node.js 18+
- Backend services (Java & Python) running.

### 2. Installation
```bash
cd antigravity-ui
npm install
```

### 3. Environment Setup
Create a `.env.local` file:
```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

### 4. Development
```bash
npm run dev
```
Open [http://localhost:3000](http://localhost:3000) to see the result.

## 📁 Architecture
- `/src/app`: Routes and Page components.
- `/src/components`: UI Atomic components and Layout Shell.
- `/src/services`: API abstraction using Axios.
- `/src/styles`: Theme configuration and Global CSS.

## 📝 Production Build
```bash
npm run build
npm start
```

## 📄 License
MIT
