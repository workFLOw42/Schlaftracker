
import { SleepEntry, User, WakeWindow } from '../types';

const STORAGE_KEYS = {
  USERS: 'schlafgut_users',
  ACTIVE_USER: 'schlafgut_active_user',
  ENTRIES: 'schlafgut_entries',
};

// --- User Management ---

export const getUsers = (): User[] => {
  const usersJson = localStorage.getItem(STORAGE_KEYS.USERS);
  return usersJson ? JSON.parse(usersJson) : [];
};

export const saveUser = (user: User): void => {
  const users = getUsers();
  const existingIndex = users.findIndex(u => u.id === user.id);
  if (existingIndex >= 0) {
    users[existingIndex] = user;
  } else {
    users.push(user);
  }
  localStorage.setItem(STORAGE_KEYS.USERS, JSON.stringify(users));
  setActiveUser(user);
};

export const getActiveUser = (): User | null => {
  const userJson = localStorage.getItem(STORAGE_KEYS.ACTIVE_USER);
  return userJson ? JSON.parse(userJson) : null;
};

export const setActiveUser = (user: User | null): void => {
  if (user) {
    localStorage.setItem(STORAGE_KEYS.ACTIVE_USER, JSON.stringify(user));
  } else {
    localStorage.removeItem(STORAGE_KEYS.ACTIVE_USER);
  }
};

// --- Sleep Entries Management ---

export const getEntries = (userId: string): SleepEntry[] => {
  const allEntriesJson = localStorage.getItem(STORAGE_KEYS.ENTRIES);
  const allEntries: SleepEntry[] = allEntriesJson ? JSON.parse(allEntriesJson) : [];
  // Filter by user and sort by date descending
  return allEntries
    .filter(entry => entry.userId === userId)
    .sort((a, b) => new Date(b.bedTime).getTime() - new Date(a.bedTime).getTime());
};

export const saveEntry = (entry: SleepEntry): void => {
  const allEntriesJson = localStorage.getItem(STORAGE_KEYS.ENTRIES);
  const allEntries: SleepEntry[] = allEntriesJson ? JSON.parse(allEntriesJson) : [];
  
  const existingIndex = allEntries.findIndex(e => e.id === entry.id);
  if (existingIndex >= 0) {
    allEntries[existingIndex] = entry;
  } else {
    allEntries.push(entry);
  }
  
  localStorage.setItem(STORAGE_KEYS.ENTRIES, JSON.stringify(allEntries));
};

export const updateEntry = (entry: SleepEntry): void => {
  saveEntry(entry); // In this implementation, saveEntry handles both insert and update logic
};

export const deleteEntry = (entryId: string): void => {
  const allEntriesJson = localStorage.getItem(STORAGE_KEYS.ENTRIES);
  let allEntries: SleepEntry[] = allEntriesJson ? JSON.parse(allEntriesJson) : [];
  allEntries = allEntries.filter(e => e.id !== entryId);
  localStorage.setItem(STORAGE_KEYS.ENTRIES, JSON.stringify(allEntries));
};

// Helper to seed data for demo purposes
export const seedDemoData = (userId: string) => {
  const existing = getEntries(userId);
  if (existing.length > 0) return;

  const demoEntries: SleepEntry[] = [];
  const now = new Date();
  
  for (let i = 0; i < 7; i++) {
    const date = new Date(now);
    date.setDate(date.getDate() - i);
    
    // 1. Regular Night Sleep
    // Bedtime: 22:00 - 01:00
    const bedTime = new Date(date);
    bedTime.setHours(22 + Math.floor(Math.random() * 3), Math.floor(Math.random() * 60));
    
    // Wake time: 06:00 - 09:00 next day
    const wakeTime = new Date(date);
    wakeTime.setDate(wakeTime.getDate() + 1);
    wakeTime.setHours(6 + Math.floor(Math.random() * 3), Math.floor(Math.random() * 60));

    const totalDurationMs = wakeTime.getTime() - bedTime.getTime();
    
    // Random Latency (10-45 mins)
    const latency = 10 + Math.floor(Math.random() * 35);

    // Generate Wake Windows
    const wakeWindows: WakeWindow[] = [];
    const interruptionCount = Math.floor(Math.random() * 4); // 0 to 3 interruptions
    let accumulatedWakeMinutes = 0;

    for (let j = 0; j < interruptionCount; j++) {
      // Create a wake window somewhere in the middle
      const windowStart = new Date(bedTime.getTime() + (latency * 60000) + (totalDurationMs * (0.2 + Math.random() * 0.5)));
      const windowDurationMin = 5 + Math.floor(Math.random() * 25); 
      const windowEnd = new Date(windowStart.getTime() + windowDurationMin * 60000);
      
      if (windowEnd.getTime() < wakeTime.getTime()) {
         wakeWindows.push({
           start: windowStart.toISOString(),
           end: windowEnd.toISOString()
         });
         accumulatedWakeMinutes += windowDurationMin;
      }
    }

    wakeWindows.sort((a, b) => new Date(a.start).getTime() - new Date(b.start).getTime());
    const totalMinutes = Math.floor(totalDurationMs / 60000);
    const quality = Math.floor(Math.random() * 4) + 6;

    demoEntries.push({
      id: crypto.randomUUID(),
      userId,
      isNap: false,
      date: date.toISOString(),
      bedTime: bedTime.toISOString(),
      wakeTime: wakeTime.toISOString(),
      sleepLatency: latency,
      sleepDurationMinutes: totalMinutes - accumulatedWakeMinutes - latency,
      wakeDurationMinutes: accumulatedWakeMinutes,
      wakeWindows: wakeWindows,
      interruptionCount: wakeWindows.length,
      quality: quality,
      tags: i % 2 === 0 ? ['Kaffee'] : ['Sport'],
      notes: 'Automatischer Demo-Eintrag'
    });

    // 2. Occasional Nap (50% chance)
    if (Math.random() > 0.5) {
        const napStart = new Date(wakeTime); // Day after "night"
        napStart.setHours(13 + Math.floor(Math.random() * 2), Math.floor(Math.random() * 30)); // 13:00 - 15:00
        const napDuration = 20 + Math.floor(Math.random() * 40); // 20-60 min
        const napEnd = new Date(napStart.getTime() + napDuration * 60000);
        
        demoEntries.push({
            id: crypto.randomUUID(),
            userId,
            isNap: true,
            date: napStart.toISOString(), // Date of nap
            bedTime: napStart.toISOString(),
            wakeTime: napEnd.toISOString(),
            sleepLatency: 5,
            sleepDurationMinutes: napDuration - 5,
            wakeDurationMinutes: 0,
            wakeWindows: [],
            interruptionCount: 0,
            quality: 8,
            tags: ['Powernap'],
            notes: 'Mittagsschlaf'
        });
    }
  }
  
  demoEntries.forEach(saveEntry);
};
