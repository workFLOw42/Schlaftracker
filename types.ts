
export interface User {
  id: string;
  name: string;
  email: string; // Simulating Google User identifier
  avatarUrl?: string;
  settings?: {
    defaultSleepLatency: number; // in minutes
  };
}

export interface WakeWindow {
  start: string; // ISO String
  end: string; // ISO String
}

export interface SleepEntry {
  id: string;
  userId: string;
  isNap?: boolean; // New flag for naps
  date: string; // ISO String for the "night of" or date of nap
  bedTime: string; // ISO String (Time got into bed)
  wakeTime: string; // ISO String (Time got out of bed)
  sleepLatency: number; // Minutes it took to fall asleep
  sleepDurationMinutes: number; // Actual sleep (Total - Latency - WakeWindows)
  wakeDurationMinutes: number; // Total time awake during night (WakeWindows)
  wakeWindows: WakeWindow[]; // Array of specific wake phases
  interruptionCount: number; // Number of times woke up
  quality: number; // 1-10 scale
  tags: string[]; // e.g., "Caffeine", "Exercise", "Stress"
  notes: string;
}

export interface GeminiAnalysis {
  summary: string;
  score: number; // calculated score 0-100
  positivePoints: string[];
  improvementAreas: string[];
  recommendation: string;
}

export enum AppView {
  DASHBOARD = 'DASHBOARD',
  LOG_SLEEP = 'LOG_SLEEP',
  STATISTICS = 'STATISTICS',
  PROFILE = 'PROFILE',
  AI_COACH = 'AI_COACH'
}
