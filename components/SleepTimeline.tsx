
import React from 'react';
import { SleepEntry } from '../types';

interface SleepTimelineProps {
  entry: SleepEntry;
  height?: string;
  showLabels?: boolean;
  alignToAxis?: boolean; // If true, align to a standard 18:00 - 18:00 (24h) axis to include Naps
}

const SleepTimeline: React.FC<SleepTimelineProps> = ({ entry, height = "h-4", showLabels = true, alignToAxis = false }) => {
  const bedTime = new Date(entry.bedTime).getTime();
  const wakeTime = new Date(entry.wakeTime).getTime();
  
  // Standard Axis definition (for alignment): 18:00 (prev day) to 18:00 (next day) = 24 hours
  // This allows showing night sleep AND naps (usually afternoon) on one consistent line.
  const AXIS_START_HOUR = 18;
  const AXIS_DURATION_HOURS = 24;
  const AXIS_DURATION_MS = AXIS_DURATION_HOURS * 60 * 60 * 1000;

  // Helper to determine bar color based on quality
  const getQualityColor = (quality: number) => {
    if (quality >= 8) return 'bg-emerald-500/80';
    if (quality >= 5) return 'bg-indigo-500/80';
    return 'bg-rose-500/80';
  };

  const barColor = getQualityColor(entry.quality);

  // Helper to normalize a date to a reference day for comparison
  const getNormalizedTime = (dateStr: string) => {
    const d = new Date(dateStr);
    const h = d.getHours();
    
    // Calculate offset from 18:00 previous day
    let hoursFromStart = 0;
    
    // If time is 18:00 or later, it's the start of the window (0 to 6h)
    if (h >= AXIS_START_HOUR) {
        hoursFromStart = (h - AXIS_START_HOUR) + (d.getMinutes() / 60);
    } else {
        // If time is before 18:00, it's the next day (6h to 24h)
        hoursFromStart = (h + (24 - AXIS_START_HOUR)) + (d.getMinutes() / 60);
    }
    return (hoursFromStart / AXIS_DURATION_HOURS) * 100;
  };

  let leftPercent = 0;
  let widthPercent = 100;
  
  // Latency calculation in percent
  let latencyPercent = 0;

  if (alignToAxis) {
      const startPct = getNormalizedTime(entry.bedTime);
      const endPct = getNormalizedTime(entry.wakeTime);
      
      let diff = endPct - startPct;
      // Handle wrapping across the 24h boundary if simple math fails (though our normalization handles simple day roll)
      if (diff < 0) diff += 100; 

      leftPercent = Math.max(0, startPct);
      widthPercent = diff;
      
      // Calculate latency as a percentage of the 24h fixed axis
      latencyPercent = ((entry.sleepLatency || 0) / 60 / AXIS_DURATION_HOURS) * 100;
      
      if (leftPercent + widthPercent > 100) {
          widthPercent = 100 - leftPercent;
      }
  } else {
      // Relative mode (0-100% of that specific night)
      const totalDurationMinutes = (wakeTime - bedTime) / 60000;
      if (totalDurationMinutes > 0) {
          latencyPercent = ((entry.sleepLatency || 0) / totalDurationMinutes) * 100;
      }
  }
  
  const totalDuration = wakeTime - bedTime;
  if (totalDuration <= 0) return null;

  return (
    <div className="w-full">
      {showLabels && !alignToAxis && (
        <div className="flex justify-between text-xs text-gray-500 mb-1">
          <span>{new Date(entry.bedTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
          <span>{new Date(entry.wakeTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
        </div>
      )}
      
      <div className={`relative w-full ${height} bg-night-900/50 rounded-full overflow-hidden border border-night-700`}>
        {/* Main Sleep Bar (Starts after latency) */}
        <div 
            className={`absolute top-0 h-full ${barColor} rounded-sm transition-all`}
            style={{
                left: `${leftPercent + latencyPercent}%`,
                width: `${Math.max(0, widthPercent - latencyPercent)}%`
            }}
            title={`${entry.isNap ? 'Mittagsschlaf' : 'Schlaf'} - Qualität: ${entry.quality}/10`}
        >
            {entry.isNap && (
                <div className="w-full h-full flex items-center justify-center opacity-50">
                    <i className="fas fa-sun text-[10px] text-white"></i>
                </div>
            )}
        </div>

        {/* Latency Bar (At the start of bed time) */}
        <div 
            className="absolute top-0 h-full bg-gray-600/50 border-r border-night-900/30"
            title={`Einschlafen: ${entry.sleepLatency} min`}
            style={{
                left: `${leftPercent}%`,
                width: `${latencyPercent}%`
            }}
        ></div>

        {/* Wake Windows */}
        {entry.wakeWindows.map((window, index) => {
          let wLeft = 0;
          let wWidth = 0;

          if (alignToAxis) {
              const startPct = getNormalizedTime(window.start);
              const endPct = getNormalizedTime(window.end);
              wLeft = startPct;
              wWidth = endPct - startPct;
          } else {
              // Relative to specific night duration
              const start = new Date(window.start).getTime();
              const end = new Date(window.end).getTime();
              wLeft = ((start - bedTime) / totalDuration) * 100;
              wWidth = ((end - start) / totalDuration) * 100;
          }

          return (
            <div
              key={index}
              className="absolute top-0 h-full bg-orange-400 border-x border-night-900/50 z-10"
              style={{
                left: `${Math.max(0, wLeft)}%`,
                width: `${Math.min(100, wWidth)}%`,
              }}
              title={`Wach: ${new Date(window.start).toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'})}`}
            ></div>
          );
        })}
      </div>
      
      {showLabels && !alignToAxis && (
        <div className="flex gap-4 mt-1 justify-center">
            <div className="flex items-center gap-1">
                <div className={`w-2 h-2 rounded-full ${barColor}`}></div>
                <span className="text-[10px] text-gray-400">Schlaf</span>
            </div>
            <div className="flex items-center gap-1">
                <div className="w-2 h-2 rounded-full bg-gray-600"></div>
                <span className="text-[10px] text-gray-400">Einschlafen</span>
            </div>
            {entry.wakeWindows.length > 0 && (
                <div className="flex items-center gap-1">
                    <div className="w-2 h-2 rounded-full bg-orange-400"></div>
                    <span className="text-[10px] text-gray-400">Wach</span>
                </div>
            )}
        </div>
      )}
    </div>
  );
};

export default SleepTimeline;
