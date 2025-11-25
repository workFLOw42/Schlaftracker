
import React, { useState, useEffect } from 'react';
import { SleepEntry, WakeWindow } from '../types';

interface SleepLoggerProps {
  userId: string;
  existingEntries: SleepEntry[]; // For validation
  initialData?: SleepEntry | null;
  defaultLatency: number;
  onSave: (entry: SleepEntry) => void;
  onCancel: () => void;
  onDelete?: (entryId: string) => void;
}

const SleepLogger: React.FC<SleepLoggerProps> = ({ userId, existingEntries, initialData, defaultLatency, onSave, onCancel, onDelete }) => {
  // Helper to format Date for input[datetime-local]
  const toLocalISO = (dateStr: string | Date) => {
    const date = new Date(dateStr);
    const pad = (num: number) => (num < 10 ? '0' : '') + num;
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
  };

  // Defaults
  const now = new Date();
  const yesterday = new Date(now);
  yesterday.setDate(yesterday.getDate() - 1);
  yesterday.setHours(23, 0, 0, 0);

  const todayWake = new Date(now);
  todayWake.setHours(7, 0, 0, 0);

  // Nap defaults (today 13:00 - 13:45)
  const todayNapStart = new Date(now);
  todayNapStart.setHours(13, 0, 0, 0);
  const todayNapEnd = new Date(now);
  todayNapEnd.setHours(13, 45, 0, 0);

  // State
  const [isNap, setIsNap] = useState(initialData?.isNap || false);
  const [bedTime, setBedTime] = useState(initialData ? toLocalISO(initialData.bedTime) : toLocalISO(yesterday));
  const [wakeTime, setWakeTime] = useState(initialData ? toLocalISO(initialData.wakeTime) : toLocalISO(todayWake));
  const [latency, setLatency] = useState(initialData ? initialData.sleepLatency : defaultLatency);
  const [quality, setQuality] = useState(initialData ? initialData.quality : 7);
  const [notes, setNotes] = useState(initialData ? initialData.notes : '');
  
  // Validation Error Message
  const [error, setError] = useState<string | null>(null);
  
  // Wake Windows State: Map to a local shape with IDs for UI handling
  const [wakeWindows, setWakeWindows] = useState<{id: string, start: string, end: string}[]>([]);
  const [isAddingWake, setIsAddingWake] = useState(false);
  
  // Temporary state for adding a new wake window
  const [newWakeStart, setNewWakeStart] = useState('');
  const [newWakeEnd, setNewWakeEnd] = useState('');

  // Switch times when toggling Nap mode (if not editing)
  useEffect(() => {
    if (!initialData) {
        if (isNap) {
            setBedTime(toLocalISO(todayNapStart));
            setWakeTime(toLocalISO(todayNapEnd));
            setLatency(5); // Naps usually happen faster
        } else {
            setBedTime(toLocalISO(yesterday));
            setWakeTime(toLocalISO(todayWake));
            setLatency(defaultLatency);
        }
    }
  }, [isNap]);

  // Initialize wake windows from props
  useEffect(() => {
    if (initialData && initialData.wakeWindows) {
      setWakeWindows(initialData.wakeWindows.map(w => ({
        id: crypto.randomUUID(),
        start: toLocalISO(w.start),
        end: toLocalISO(w.end)
      })));
    }
  }, [initialData]);

  // Auto-calculate stats based on windows
  const calculateStats = () => {
    let totalWakeMinutes = 0;
    wakeWindows.forEach(w => {
      const s = new Date(w.start).getTime();
      const e = new Date(w.end).getTime();
      totalWakeMinutes += Math.max(0, Math.floor((e - s) / 60000));
    });
    return { totalWakeMinutes, count: wakeWindows.length };
  };

  const { totalWakeMinutes, count } = calculateStats();

  const handleAddWakeStart = () => {
    // Default the new wake window to roughly the middle of the sleep period
    const startBed = new Date(bedTime).getTime();
    const endWake = new Date(wakeTime).getTime();
    const mid = new Date(startBed + (endWake - startBed) / 2);
    const midEnd = new Date(mid.getTime() + 15 * 60000); // 15 mins later

    setNewWakeStart(toLocalISO(mid));
    setNewWakeEnd(toLocalISO(midEnd));
    setIsAddingWake(true);
  };

  const confirmWakeWindow = () => {
    const start = new Date(newWakeStart);
    const end = new Date(newWakeEnd);

    if (end <= start) {
      alert("Das Ende der Wachphase muss nach dem Anfang liegen.");
      return;
    }
    
    // Validation: Check bounds
    if (start < new Date(bedTime) || end > new Date(wakeTime)) {
        alert("Die Wachphase muss innerhalb der Schlafzeit liegen.");
        return;
    }

    setWakeWindows([...wakeWindows, { id: crypto.randomUUID(), start: newWakeStart, end: newWakeEnd }]);
    setIsAddingWake(false);
  };

  const removeWakeWindow = (id: string) => {
    setWakeWindows(wakeWindows.filter(w => w.id !== id));
  };

  const validate = (): boolean => {
    setError(null);
    const start = new Date(bedTime);
    const end = new Date(wakeTime);
    
    // 1. Logic: End after Start
    if (end <= start) {
        setError("Die Aufwachzeit muss nach der Bettzeit liegen.");
        return false;
    }

    // 2. Logic: Duration
    const diffMs = end.getTime() - start.getTime();
    const timeInBedMinutes = Math.floor(diffMs / 60000);
    const actualSleepMinutes = timeInBedMinutes - totalWakeMinutes - latency;

    if (actualSleepMinutes < 0) {
      setError("Die berechnete Schlafzeit ist negativ. Bitte Wachphasen oder Einschlafzeit prüfen.");
      return false;
    }

    // 3. Logic: Overlap Check
    // We must exclude the current entry being edited from the check
    const currentId = initialData?.id;
    
    const hasOverlap = existingEntries.some(entry => {
        if (entry.id === currentId) return false; // Skip self

        const eStart = new Date(entry.bedTime);
        const eEnd = new Date(entry.wakeTime);

        // Check if ranges overlap: (StartA < EndB) and (EndA > StartB)
        return start < eEnd && end > eStart;
    });

    if (hasOverlap) {
        setError("Dieser Zeitraum überschneidet sich mit einem existierenden Eintrag.");
        return false;
    }

    return true;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!validate()) return;

    const start = new Date(bedTime);
    const end = new Date(wakeTime);
    const diffMs = end.getTime() - start.getTime();
    const timeInBedMinutes = Math.floor(diffMs / 60000);
    const actualSleepMinutes = timeInBedMinutes - totalWakeMinutes - latency;

    // Clean up wake windows for storage (remove temp IDs)
    const finalWakeWindows: WakeWindow[] = wakeWindows.map(w => ({
        start: new Date(w.start).toISOString(),
        end: new Date(w.end).toISOString()
    })).sort((a, b) => new Date(a.start).getTime() - new Date(b.start).getTime());

    const entryToSave: SleepEntry = {
      id: initialData ? initialData.id : crypto.randomUUID(),
      userId,
      isNap,
      date: start.toISOString(),
      bedTime: start.toISOString(),
      wakeTime: end.toISOString(),
      sleepLatency: latency,
      sleepDurationMinutes: actualSleepMinutes,
      wakeDurationMinutes: totalWakeMinutes,
      wakeWindows: finalWakeWindows,
      interruptionCount: count,
      quality,
      tags: initialData ? initialData.tags : [],
      notes
    };

    onSave(entryToSave);
  };

  return (
    <div className="bg-night-800 rounded-2xl p-6 shadow-xl border border-night-700 animate-fade-in relative">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-white flex items-center gap-2">
            <i className={`fas ${initialData ? 'fa-edit' : 'fa-plus-circle'} text-dream-500`}></i>
            {initialData ? 'Eintrag bearbeiten' : 'Schlaf erfassen'}
        </h2>
        <div className="flex items-center gap-4">
            {initialData && onDelete && (
                <button 
                    type="button" 
                    onClick={() => {
                        if(window.confirm('Eintrag wirklich löschen?')) onDelete(initialData.id);
                    }}
                    className="text-red-400 hover:text-red-300 text-sm flex items-center gap-1"
                >
                    <i className="fas fa-trash"></i> <span className="hidden sm:inline">Löschen</span>
                </button>
            )}
            <button 
                type="button" 
                onClick={onCancel} 
                className="text-gray-400 hover:text-white transition-colors text-xl"
                aria-label="Schließen"
            >
                <i className="fas fa-times"></i>
            </button>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-8">
        
        {/* Type Toggle */}
        <div className="flex bg-night-900 p-1 rounded-lg border border-night-600">
            <button
                type="button"
                className={`flex-1 py-2 rounded-md text-sm font-bold transition-all ${!isNap ? 'bg-dream-600 text-white shadow' : 'text-gray-400 hover:text-gray-200'}`}
                onClick={() => setIsNap(false)}
            >
                <i className="fas fa-moon mr-2"></i> Nachtschlaf
            </button>
            <button
                type="button"
                className={`flex-1 py-2 rounded-md text-sm font-bold transition-all ${isNap ? 'bg-orange-500 text-white shadow' : 'text-gray-400 hover:text-gray-200'}`}
                onClick={() => setIsNap(true)}
            >
                <i className="fas fa-sun mr-2"></i> Mittagsschlaf
            </button>
        </div>

        {/* Error Display */}
        {error && (
            <div className="bg-red-500/20 border border-red-500/50 text-red-200 p-3 rounded-lg flex items-center gap-2 text-sm">
                <i className="fas fa-exclamation-triangle"></i>
                {error}
            </div>
        )}

        {/* Main Times */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-gray-400 text-sm mb-1">Ging ins Bett</label>
            <input
              type="datetime-local"
              value={bedTime}
              onChange={(e) => setBedTime(e.target.value)}
              className="w-full bg-night-900 border border-night-600 rounded p-3 text-white focus:border-dream-500 outline-none"
              required
            />
          </div>
          <div>
            <label className="block text-gray-400 text-sm mb-1">Aufgewacht</label>
            <input
              type="datetime-local"
              value={wakeTime}
              onChange={(e) => setWakeTime(e.target.value)}
              className="w-full bg-night-900 border border-night-600 rounded p-3 text-white focus:border-dream-500 outline-none"
              required
            />
          </div>
        </div>

        {/* Latency Input */}
        <div>
            <label className="block text-gray-400 text-sm mb-1">
                <i className="fas fa-stopwatch mr-1 text-gray-500"></i> Dauer bis zum Einschlafen (Min)
            </label>
            <div className="flex items-center gap-4">
                <input
                    type="range"
                    min="0"
                    max="60"
                    step="1"
                    value={latency}
                    onChange={(e) => setLatency(parseInt(e.target.value))}
                    className="flex-1 h-2 bg-night-600 rounded-lg appearance-none cursor-pointer accent-gray-400"
                />
                <div className="w-20 bg-night-900 border border-night-600 rounded px-3 py-2 text-center text-white font-mono">
                    {latency}m
                </div>
            </div>
        </div>

        {/* Wake Phases Section */}
        <div className="bg-night-900/50 p-4 rounded-xl border border-night-700">
            <div className="flex justify-between items-center mb-4">
                <label className="text-gray-300 font-medium flex items-center gap-2">
                    <i className="fas fa-eye text-orange-400"></i> Wachphasen
                </label>
                <div className="text-xs text-gray-500">
                    {count} Phasen • {totalWakeMinutes} Min. gesamt
                </div>
            </div>

            <div className="space-y-3 mb-4">
                {wakeWindows.map((win) => (
                    <div key={win.id} className="flex items-center gap-2 bg-night-800 p-2 rounded border border-night-600">
                        <span className="text-orange-300 text-sm flex-1">
                            {new Date(win.start).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})} - {new Date(win.end).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}
                        </span>
                        <span className="text-gray-500 text-xs">
                             {Math.floor((new Date(win.end).getTime() - new Date(win.start).getTime())/60000)} min
                        </span>
                        <button type="button" onClick={() => removeWakeWindow(win.id)} className="text-red-400 hover:text-red-300 px-2">
                            <i className="fas fa-times"></i>
                        </button>
                    </div>
                ))}
                {wakeWindows.length === 0 && !isAddingWake && (
                    <p className="text-xs text-gray-600 italic">Keine Wachphasen erfasst.</p>
                )}
            </div>

            {!isAddingWake ? (
                <button
                    type="button"
                    onClick={handleAddWakeStart}
                    className="w-full py-2 border border-dashed border-gray-600 text-gray-400 rounded hover:border-dream-500 hover:text-dream-400 transition-colors text-sm"
                >
                    + Wachphase hinzufügen
                </button>
            ) : (
                <div className="bg-night-800 p-3 rounded border border-dream-500/50 animate-fade-in">
                    <div className="grid grid-cols-2 gap-2 mb-3">
                        <div>
                            <label className="text-xs text-gray-500 block mb-1">Von</label>
                            <input
                                type="datetime-local"
                                value={newWakeStart}
                                onChange={(e) => setNewWakeStart(e.target.value)}
                                className="w-full bg-night-900 border border-night-600 rounded p-1 text-sm text-white"
                            />
                        </div>
                        <div>
                            <label className="text-xs text-gray-500 block mb-1">Bis</label>
                            <input
                                type="datetime-local"
                                value={newWakeEnd}
                                onChange={(e) => setNewWakeEnd(e.target.value)}
                                className="w-full bg-night-900 border border-night-600 rounded p-1 text-sm text-white"
                            />
                        </div>
                    </div>
                    <div className="flex gap-2">
                        <button type="button" onClick={() => setIsAddingWake(false)} className="flex-1 py-1 text-xs text-gray-400 hover:bg-night-700 rounded">Abbrechen</button>
                        <button type="button" onClick={confirmWakeWindow} className="flex-1 py-1 text-xs bg-dream-600 text-white rounded hover:bg-dream-500">Hinzufügen</button>
                    </div>
                </div>
            )}
        </div>

        {/* Quality */}
        <div>
          <label className="block text-gray-400 text-sm mb-2 flex justify-between">
             <span>Schlafqualität (1-10)</span>
             <span className={`font-bold ${quality >= 8 ? 'text-emerald-400' : quality >= 5 ? 'text-indigo-400' : 'text-rose-400'}`}>{quality}/10</span>
          </label>
          <input
            type="range"
            min="1"
            max="10"
            value={quality}
            onChange={(e) => setQuality(parseInt(e.target.value))}
            className="w-full h-2 bg-night-600 rounded-lg appearance-none cursor-pointer accent-dream-500"
          />
          <div className="flex justify-between text-xs text-gray-500 mt-1">
            <span>Schlecht</span>
            <span>Exzellent</span>
          </div>
        </div>

        {/* Notes */}
        <div>
           <label className="block text-gray-400 text-sm mb-1">Notizen (Optional)</label>
           <textarea
             value={notes}
             onChange={(e) => setNotes(e.target.value)}
             className="w-full bg-night-900 border border-night-600 rounded p-3 text-white focus:border-dream-500 outline-none h-24"
             placeholder="Traumfetzen, Stress, spätes Essen..."
           />
        </div>

        <div className="flex gap-4 pt-4 border-t border-night-700">
           <button
             type="button"
             onClick={onCancel}
             className="flex-1 py-3 bg-night-700 hover:bg-night-600 text-white rounded-lg transition-colors"
           >
             Abbrechen
           </button>
           <button
             type="submit"
             className="flex-1 py-3 bg-dream-500 hover:bg-dream-400 text-white font-bold rounded-lg shadow-lg shadow-dream-500/30 transition-all transform hover:scale-[1.02]"
           >
             Speichern
           </button>
        </div>

      </form>
    </div>
  );
};

export default SleepLogger;
