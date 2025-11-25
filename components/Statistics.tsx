
import React, { useState, useMemo } from 'react';
import { SleepEntry } from '../types';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, BarChart, Bar, Cell } from 'recharts';
import SleepTimeline from './SleepTimeline';
import { jsPDF } from 'jspdf';

interface StatisticsProps {
  entries: SleepEntry[];
}

const Statistics: React.FC<StatisticsProps> = ({ entries }) => {
  // Date Range State
  const today = new Date();
  const sevenDaysAgo = new Date();
  sevenDaysAgo.setDate(today.getDate() - 7);

  const formatDateInput = (d: Date) => d.toISOString().split('T')[0];

  const [startDate, setStartDate] = useState(formatDateInput(sevenDaysAgo));
  const [endDate, setEndDate] = useState(formatDateInput(today));

  // Filter entries based on range
  const filteredEntries = useMemo(() => {
     const start = new Date(startDate);
     start.setHours(0,0,0,0);
     const end = new Date(endDate);
     end.setHours(23,59,59,999);

     return entries.filter(e => {
        const entryDate = new Date(e.date);
        return entryDate >= start && entryDate <= end;
     });
  }, [entries, startDate, endDate]);

  const sortedForCharts = [...filteredEntries]
    .filter(e => !e.isNap) // Only chart night sleep for trends initially
    .reverse()
    .map(e => ({
      date: new Date(e.date).toLocaleDateString(undefined, { weekday: 'short', day: 'numeric' }),
      duration: parseFloat((e.sleepDurationMinutes / 60).toFixed(1)),
      quality: e.quality,
      interruptions: e.interruptionCount,
      wakeTime: e.wakeDurationMinutes
  }));

  const CustomTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
      return (
        <div className="bg-night-800 border border-night-600 p-3 rounded shadow-xl">
          <p className="text-gray-300 font-bold mb-1">{label}</p>
          {payload.map((entry: any, index: number) => (
            <p key={index} style={{ color: entry.color }} className="text-sm">
              {entry.name}: {entry.value}
            </p>
          ))}
        </div>
      );
    }
    return null;
  };

  // --- MEDIAN CALCULATION HELPERS ---

  // Constants for Timeline
  const AXIS_START_HOUR = 18;
  const AXIS_DURATION_HOURS = 24;

  const getMinutesFromAxisStart = (dateStr: string) => {
    const d = new Date(dateStr);
    let h = d.getHours();
    const m = d.getMinutes();
    
    // Normalize logic: 18:00 is 0. 19:00 is 60. 00:00 is 360. 06:00 is 720.
    if (h < AXIS_START_HOUR) {
        h += 24; // Handle next day
    }
    return ((h - AXIS_START_HOUR) * 60) + m;
  };

  const calculateMedianTimeStr = (times: string[]): { display: string, minutesFromStart: number } | null => {
      if (times.length === 0) return null;
      
      const minutes = times.map(t => getMinutesFromAxisStart(t));
      minutes.sort((a, b) => a - b);
      
      const mid = Math.floor(minutes.length / 2);
      const medianMin = minutes.length % 2 !== 0 
        ? minutes[mid] 
        : (minutes[mid - 1] + minutes[mid]) / 2;

      // Convert back to time string
      let totalMinutes = medianMin;
      let h = Math.floor(totalMinutes / 60) + AXIS_START_HOUR;
      const m = Math.floor(totalMinutes % 60);
      
      if (h >= 24) h -= 24;

      const pad = (n: number) => n < 10 ? '0' + n : n;
      return { 
          display: `${pad(h)}:${pad(m)}`, 
          minutesFromStart: medianMin 
      };
  };

  const nightSleepEntries = filteredEntries.filter(e => !e.isNap);
  const medianBedTime = calculateMedianTimeStr(nightSleepEntries.map(e => e.bedTime));
  const medianWakeTime = calculateMedianTimeStr(nightSleepEntries.map(e => e.wakeTime));

  const medianBedPercent = medianBedTime ? (medianBedTime.minutesFromStart / (AXIS_DURATION_HOURS * 60)) * 100 : 0;
  const medianWakePercent = medianWakeTime ? (medianWakeTime.minutesFromStart / (AXIS_DURATION_HOURS * 60)) * 100 : 0;
  
  // Averages for Export
  const avgDuration = nightSleepEntries.length > 0 
    ? (nightSleepEntries.reduce((acc, c) => acc + c.sleepDurationMinutes, 0) / nightSleepEntries.length / 60).toFixed(2) + ' Std' 
    : '-';
  const avgQuality = nightSleepEntries.length > 0
    ? (nightSleepEntries.reduce((acc, c) => acc + c.quality, 0) / nightSleepEntries.length).toFixed(1)
    : '-';

  // --- EXPORT LOGIC ---

  const handleExportCSV = () => {
    // Header
    const headers = ['Datum', 'Typ', 'Bettzeit', 'Aufwachzeit', 'Schlafdauer (min)', 'Wachdauer (min)', 'Einschlafzeit (min)', 'Qualität (1-10)', 'Notizen'];
    
    // Rows
    const rows = filteredEntries.map(e => [
        new Date(e.date).toLocaleDateString(),
        e.isNap ? 'Mittagsschlaf' : 'Nachtschlaf',
        new Date(e.bedTime).toLocaleString(),
        new Date(e.wakeTime).toLocaleString(),
        e.sleepDurationMinutes,
        e.wakeDurationMinutes,
        e.sleepLatency,
        e.quality,
        `"${e.notes.replace(/"/g, '""')}"` // Escape quotes
    ]);

    // Explicitly construct CSV string to ensure stats are appended correctly
    let csvContent = headers.join(',') + '\n';
    rows.forEach(row => {
        csvContent += row.join(',') + '\n';
    });

    csvContent += '\nSTATISTIK ZUSAMMENFASSUNG\n';
    csvContent += `Median Bettzeit,${medianBedTime?.display || '-'}\n`;
    csvContent += `Median Aufwachzeit,${medianWakeTime?.display || '-'}\n`;
    csvContent += `Durchschnitt Dauer,${avgDuration}\n`;
    csvContent += `Durchschnitt Qualität,${avgQuality}\n`;

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `SchlafGut_Export_${startDate}_${endDate}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleExportPDF = () => {
    const doc = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });
    
    // Config
    const margin = 15;
    const pageWidth = 297; // Landscape A4
    const contentWidth = pageWidth - (margin * 2);
    let yPos = 20;

    // --- Header ---
    doc.setFillColor(30, 41, 59); // night-800
    doc.rect(0, 0, pageWidth, 40, 'F');
    
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(24);
    doc.text("SchlafGut Report", margin, 20);
    
    doc.setFontSize(10);
    doc.setTextColor(148, 163, 184); // gray-400
    doc.text(`Zeitraum: ${new Date(startDate).toLocaleDateString()} - ${new Date(endDate).toLocaleDateString()}`, margin, 30);
    
    // Move stats below header to avoid color/contrast issues
    yPos = 50;
    doc.setTextColor(0, 0, 0); // Black for stats
    doc.setFontSize(10);
    
    const statText = `Median Bettzeit: ${medianBedTime?.display || '-'} | Median Aufwachzeit: ${medianWakeTime?.display || '-'} | Ø Dauer: ${avgDuration} | Ø Qualität: ${avgQuality}`;
    doc.text(statText, margin, yPos);
    
    yPos += 15;

    // --- Timeline Visualization ---
    doc.setFontSize(14);
    doc.text("Schlafmuster (Visuell)", margin, yPos);
    yPos += 10;

    // Draw Axis Background
    const axisHeight = (filteredEntries.length * 8) + 15;
    doc.setFillColor(241, 245, 249); // slate-100
    doc.rect(margin, yPos, contentWidth, axisHeight, 'F');

    // Draw Granular Axis Labels (every 2 hours)
    doc.setFontSize(7);
    doc.setTextColor(100, 116, 139);
    
    const totalHours = 24;
    const steps = 12; // Every 2 hours
    
    for (let i = 0; i <= steps; i++) {
        const pct = i / steps;
        const x = margin + (pct * contentWidth);
        
        // Time Label
        let hour = AXIS_START_HOUR + (i * 2);
        if (hour >= 24) hour -= 24;
        const label = `${hour.toString().padStart(2,'0')}:00`;
        
        doc.text(label, x - 2, yPos + 5);
        
        // Grid line
        doc.setDrawColor(203, 213, 225);
        doc.setLineWidth(0.1);
        doc.line(x, yPos + 8, x, yPos + axisHeight);
    }

    // --- Draw Median Lines on PDF ---
    if (medianBedPercent > 0) {
        const xBed = margin + ((medianBedPercent / 100) * contentWidth);
        doc.setDrawColor(139, 92, 246); // Violet-500
        doc.setLineWidth(0.4);
        doc.setLineDashPattern([2, 2], 0);
        doc.line(xBed, yPos + 8, xBed, yPos + axisHeight);
        doc.setTextColor(139, 92, 246);
        doc.text("Ø Bett", xBed - 3, yPos + axisHeight + 4);
    }
    
    if (medianWakePercent > 0) {
        const xWake = margin + ((medianWakePercent / 100) * contentWidth);
        doc.setDrawColor(20, 184, 166); // Teal-500
        doc.setLineWidth(0.4);
        doc.setLineDashPattern([2, 2], 0);
        doc.line(xWake, yPos + 8, xWake, yPos + axisHeight);
        doc.setTextColor(20, 184, 166);
        doc.text("Ø Wach", xWake - 3, yPos + axisHeight + 4);
    }
    
    doc.setLineDashPattern([], 0); // Reset dash

    yPos += 12;

    // Helper for time normalization (same as SleepTimeline.tsx)
    const getNormalizedPct = (dateStr: string) => {
        const minutes = getMinutesFromAxisStart(dateStr);
        return (minutes / (AXIS_DURATION_HOURS * 60));
    };

    // Draw Bars
    filteredEntries.forEach((entry) => {
        if (yPos > 180) { // New page if full
            doc.addPage();
            yPos = 20;
        }

        const startPct = getNormalizedPct(entry.bedTime);
        const endPct = getNormalizedPct(entry.wakeTime);
        let widthPct = endPct - startPct;
        if (widthPct < 0) widthPct += 1;

        const xStart = margin + (startPct * contentWidth);
        const wTotal = widthPct * contentWidth;
        
        // Latency
        const latencyPct = (entry.sleepLatency / 60) / AXIS_DURATION_HOURS;
        const wLatency = latencyPct * contentWidth;

        // Colors
        let r, g, b;
        if (entry.quality >= 8) { r=16; g=185; b=129; } // Emerald
        else if (entry.quality >= 5) { r=99; g=102; b=241; } // Indigo
        else { r=244; g=63; b=94; } // Rose

        // Draw Latency Bar (Gray)
        doc.setFillColor(156, 163, 175);
        doc.rect(xStart, yPos, wLatency, 4, 'F');

        // Draw Sleep Bar
        doc.setFillColor(r, g, b);
        doc.rect(xStart + wLatency, yPos, Math.max(0, wTotal - wLatency), 4, 'F');

        // Draw Wake Windows
        entry.wakeWindows.forEach(win => {
            const wStartPct = getNormalizedPct(win.start);
            const wEndPct = getNormalizedPct(win.end);
            let wWidthPct = wEndPct - wStartPct;
            if (wWidthPct < 0) wWidthPct += 1;

            const wx = margin + (wStartPct * contentWidth);
            const ww = wWidthPct * contentWidth;

            doc.setFillColor(251, 146, 60); // Orange
            doc.rect(wx, yPos, ww, 4, 'F');
        });

        // Label Date
        doc.setTextColor(51, 65, 85);
        doc.setFontSize(7);
        const dateLabel = new Date(entry.date).toLocaleDateString(undefined, {weekday:'short', day:'numeric'});
        doc.text(dateLabel, margin - 12, yPos + 3);

        yPos += 7;
    });

    // Save
    doc.save(`SchlafGut_Report_${startDate}_${endDate}.pdf`);
  };


  if (entries.length === 0) {
    return <div className="text-center text-gray-400 py-10">Keine Daten für Statistiken vorhanden.</div>;
  }

  // Generate granular time labels (every 2 hours)
  const timeLabels = [];
  for(let i=0; i<=24; i+=2) {
      let h = AXIS_START_HOUR + i;
      if (h >= 24) h -= 24;
      timeLabels.push({ 
          label: `${h.toString().padStart(2, '0')}:00`, 
          percent: (i/24)*100 
      });
  }

  return (
    <div className="space-y-8 animate-fade-in">
      
      {/* Date Filter & Export */}
      <div className="bg-night-800 p-4 rounded-2xl border border-night-700 flex flex-col md:flex-row gap-4 items-center justify-between">
          <div className="flex flex-col md:flex-row gap-4 items-center">
             <h2 className="text-lg font-bold text-white"><i className="fas fa-filter text-dream-500 mr-2"></i>Zeitraum</h2>
             <div className="flex gap-2">
                  <div>
                      <input 
                        type="date" 
                        value={startDate} 
                        onChange={(e) => setStartDate(e.target.value)}
                        className="bg-night-900 border border-night-600 rounded px-2 py-1 text-white text-xs focus:border-dream-500 outline-none"
                      />
                  </div>
                  <span className="text-gray-500">-</span>
                  <div>
                      <input 
                        type="date" 
                        value={endDate}
                        onChange={(e) => setEndDate(e.target.value)}
                        className="bg-night-900 border border-night-600 rounded px-2 py-1 text-white text-xs focus:border-dream-500 outline-none"
                      />
                  </div>
             </div>
          </div>

          <div className="flex gap-2">
              <button 
                onClick={handleExportCSV}
                className="bg-night-700 hover:bg-night-600 text-gray-200 text-xs font-bold py-2 px-3 rounded flex items-center gap-2 transition-colors"
                title="Für Google Sheets / Excel"
              >
                  <i className="fas fa-file-csv text-green-400"></i>
                  CSV Export
              </button>
              <button 
                onClick={handleExportPDF}
                className="bg-dream-600 hover:bg-dream-500 text-white text-xs font-bold py-2 px-3 rounded flex items-center gap-2 transition-colors"
                title="Drucken / PDF speichern"
              >
                  <i className="fas fa-file-pdf"></i>
                  PDF Export
              </button>
          </div>
      </div>

      {filteredEntries.length === 0 ? (
          <div className="text-center py-12 text-gray-500 bg-night-800 rounded-2xl border border-night-700">
              Keine Einträge im gewählten Zeitraum gefunden.
          </div>
      ) : (
        <>
            {/* Median Stats Header */}
            {(medianBedTime || medianWakeTime) && (
                <div className="grid grid-cols-2 gap-4">
                    <div className="bg-night-800 p-4 rounded-xl border border-l-4 border-night-700 border-l-dream-500 flex justify-between items-center">
                        <div>
                            <p className="text-xs text-gray-400">Median Bettzeit</p>
                            <p className="text-xl font-bold text-white">{medianBedTime?.display || '-'}</p>
                        </div>
                        <i className="fas fa-bed text-dream-500/20 text-3xl"></i>
                    </div>
                    <div className="bg-night-800 p-4 rounded-xl border border-l-4 border-night-700 border-l-teal-500 flex justify-between items-center">
                        <div>
                            <p className="text-xs text-gray-400">Median Aufwachzeit</p>
                            <p className="text-xl font-bold text-white">{medianWakeTime?.display || '-'}</p>
                        </div>
                        <i className="fas fa-sun text-teal-500/20 text-3xl"></i>
                    </div>
                </div>
            )}

            {/* Aligned Timeline Visualization Section */}
            <div className="bg-night-800 p-6 rounded-2xl border border-night-700 shadow-lg overflow-hidden relative">
                <div className="flex justify-between items-center mb-6">
                    <h3 className="text-xl font-bold text-white">Schlafmuster (24h Ansicht)</h3>
                    <div className="hidden md:flex gap-4 text-xs text-gray-400">
                        <span className="flex items-center gap-1"><div className="w-3 h-3 bg-emerald-500/80 rounded-sm"></div> Gut</span>
                        <span className="flex items-center gap-1"><div className="w-3 h-3 bg-indigo-500/80 rounded-sm"></div> Mittel</span>
                        <span className="flex items-center gap-1"><div className="w-3 h-3 bg-rose-500/80 rounded-sm"></div> Schlecht</span>
                        <span className="flex items-center gap-1"><div className="w-3 h-3 bg-orange-400 rounded-sm"></div> Wach</span>
                    </div>
                </div>
                
                {/* Granular Axis Labels */}
                <div className="relative h-6 text-xs text-gray-500 mb-2 border-b border-night-700">
                    {timeLabels.map((t, idx) => (
                        <div 
                            key={idx} 
                            className="absolute transform -translate-x-1/2"
                            style={{ left: `${t.percent}%` }}
                        >
                            {t.label}
                        </div>
                    ))}
                </div>

                <div className="space-y-3 relative">
                     {/* Vertical Guidelines for Hours */}
                     {timeLabels.map((t, idx) => (
                         <div 
                            key={idx}
                            className="absolute top-0 bottom-0 w-px bg-night-700/50"
                            style={{ left: `${t.percent}%` }}
                         ></div>
                     ))}

                     {/* Median Bed Time Line */}
                     {medianBedPercent > 0 && (
                         <div 
                            className="absolute top-0 bottom-0 w-px border-l-2 border-dashed border-dream-500 z-20"
                            style={{ left: `${medianBedPercent}%` }}
                            title={`Median Bettzeit: ${medianBedTime?.display}`}
                         ></div>
                     )}

                     {/* Median Wake Time Line */}
                     {medianWakePercent > 0 && (
                         <div 
                            className="absolute top-0 bottom-0 w-px border-l-2 border-dashed border-teal-500 z-20"
                            style={{ left: `${medianWakePercent}%` }}
                            title={`Median Aufwachzeit: ${medianWakeTime?.display}`}
                         ></div>
                     )}

                    {filteredEntries.slice(0, 30).map(entry => (
                    <div key={entry.id} className="flex items-center gap-4 relative z-10">
                        <div className="w-16 text-right text-xs text-gray-400 shrink-0 flex flex-col items-end">
                             <span>{new Date(entry.date).toLocaleDateString(undefined, {weekday: 'short', day:'numeric'})}</span>
                             {entry.isNap && <span className="text-[10px] text-orange-400"><i className="fas fa-sun"></i> Nap</span>}
                        </div>
                        <div className="grow">
                            <SleepTimeline entry={entry} height="h-5" showLabels={false} alignToAxis={true} />
                        </div>
                    </div>
                    ))}
                </div>
                <div className="flex justify-center gap-6 text-xs text-gray-400 mt-6">
                    <div className="flex items-center gap-2">
                        <div className="w-4 h-0 border-t-2 border-dashed border-dream-500"></div>
                        <span>Median Bettzeit ({medianBedTime?.display})</span>
                    </div>
                    <div className="flex items-center gap-2">
                        <div className="w-4 h-0 border-t-2 border-dashed border-teal-500"></div>
                        <span>Median Aufwachzeit ({medianWakeTime?.display})</span>
                    </div>
                </div>
            </div>

            {/* Duration Chart */}
            <div className="bg-night-800 p-6 rounded-2xl border border-night-700 shadow-lg">
                <h3 className="text-xl font-bold text-white mb-6">Nächtliche Schlafdauer (Stunden)</h3>
                <div className="h-64 w-full">
                <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={sortedForCharts}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                    <XAxis dataKey="date" stroke="#94a3b8" fontSize={12} tickMargin={10} />
                    <YAxis stroke="#94a3b8" fontSize={12} domain={[0, 12]} />
                    <Tooltip content={<CustomTooltip />} />
                    <Line type="monotone" dataKey="duration" name="Dauer (h)" stroke="#6366f1" strokeWidth={3} dot={{ r: 4, fill: '#6366f1' }} activeDot={{ r: 6 }} />
                    </LineChart>
                </ResponsiveContainer>
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                {/* Quality Chart */}
                <div className="bg-night-800 p-6 rounded-2xl border border-night-700 shadow-lg">
                    <h3 className="text-xl font-bold text-white mb-6">Schlafqualität (1-10)</h3>
                    <div className="h-64 w-full">
                    <ResponsiveContainer width="100%" height="100%">
                        <BarChart data={sortedForCharts}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#334155" vertical={false} />
                        <XAxis dataKey="date" stroke="#94a3b8" fontSize={10} />
                        <YAxis stroke="#94a3b8" domain={[0, 10]} hide />
                        <Tooltip content={<CustomTooltip />} />
                        <Bar dataKey="quality" name="Qualität" radius={[4, 4, 0, 0]}>
                            {sortedForCharts.map((entry, index) => (
                            <Cell key={`cell-${index}`} fill={entry.quality >= 8 ? '#34d399' : entry.quality >= 5 ? '#818cf8' : '#fb7185'} />
                            ))}
                        </Bar>
                        </BarChart>
                    </ResponsiveContainer>
                    </div>
                </div>

                {/* Wake Phases Chart */}
                <div className="bg-night-800 p-6 rounded-2xl border border-night-700 shadow-lg">
                    <h3 className="text-xl font-bold text-white mb-6">Wachphasen (Anzahl)</h3>
                    <div className="h-64 w-full">
                    <ResponsiveContainer width="100%" height="100%">
                        <LineChart data={sortedForCharts}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                        <XAxis dataKey="date" stroke="#94a3b8" fontSize={10} />
                        <YAxis stroke="#94a3b8" allowDecimals={false} />
                        <Tooltip content={<CustomTooltip />} />
                        <Line type="step" dataKey="interruptions" name="Unterbrechungen" stroke="#f472b6" strokeWidth={2} dot={{r:3}} />
                        </LineChart>
                    </ResponsiveContainer>
                    </div>
                </div>
            </div>
        </>
      )}
    </div>
  );
};

export default Statistics;
