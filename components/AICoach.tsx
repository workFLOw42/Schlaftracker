import React, { useState, useEffect } from 'react';
import { SleepEntry, GeminiAnalysis } from '../types';
import { analyzeSleepData } from '../services/geminiService';

interface AICoachProps {
  entries: SleepEntry[];
}

const AICoach: React.FC<AICoachProps> = ({ entries }) => {
  const [analysis, setAnalysis] = useState<GeminiAnalysis | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    // Check if we have data to analyze
    if (entries.length === 0) return;
    
    // Avoid re-fetching if we already have analysis for the same data length (simple cache logic)
    // In a real app, use a more robust hash or timestamp check
    setLoading(true);
    analyzeSleepData(entries)
      .then(result => {
        setAnalysis(result);
      })
      .catch(err => console.error(err))
      .finally(() => setLoading(false));
      
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entries.length]); // Only re-analyze when entry count changes to save tokens

  if (entries.length === 0) {
    return (
      <div className="bg-night-800 p-8 rounded-2xl border border-night-700 text-center">
        <div className="text-6xl mb-4">😴</div>
        <h3 className="text-xl font-bold text-white mb-2">Noch keine Daten</h3>
        <p className="text-gray-400">Erfasse mindestens eine Nacht, damit Gemini deinen Schlaf analysieren kann.</p>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="bg-night-800 p-8 rounded-2xl border border-night-700 text-center animate-pulse">
        <div className="w-16 h-16 bg-dream-500/20 rounded-full mx-auto mb-4 flex items-center justify-center">
             <i className="fas fa-sparkles text-dream-400 text-2xl animate-spin"></i>
        </div>
        <h3 className="text-xl font-bold text-white mb-2">Gemini analysiert deinen Schlaf...</h3>
        <p className="text-gray-400">Suche nach Mustern und erstelle deinen persönlichen Plan.</p>
      </div>
    );
  }

  if (!analysis) return null;

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Score Card */}
      <div className="bg-gradient-to-r from-indigo-900 to-purple-900 rounded-2xl p-6 shadow-xl border border-indigo-700/50 relative overflow-hidden">
        <div className="absolute top-0 right-0 p-4 opacity-10">
            <i className="fas fa-brain text-9xl text-white"></i>
        </div>
        
        <div className="relative z-10 flex items-center justify-between">
            <div>
                <h2 className="text-2xl font-bold text-white mb-1">Schlaf-Score</h2>
                <p className="text-indigo-200 text-sm">Basierend auf Dauer, Qualität & Unterbrechungen</p>
            </div>
            <div className="text-5xl font-bold text-white">
                {analysis.score}
            </div>
        </div>
      </div>

      {/* Main Analysis */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="bg-night-800 p-6 rounded-2xl border border-night-700">
             <h3 className="text-green-400 font-bold mb-4 flex items-center gap-2">
                 <i className="fas fa-check-circle"></i> Das läuft gut
             </h3>
             <ul className="space-y-2">
                 {analysis.positivePoints.map((point, idx) => (
                     <li key={idx} className="text-gray-300 text-sm flex gap-2">
                         <span className="text-green-500/50">•</span> {point}
                     </li>
                 ))}
             </ul>
          </div>

          <div className="bg-night-800 p-6 rounded-2xl border border-night-700">
             <h3 className="text-yellow-400 font-bold mb-4 flex items-center gap-2">
                 <i className="fas fa-exclamation-circle"></i> Verbesserungspotenzial
             </h3>
             <ul className="space-y-2">
                 {analysis.improvementAreas.map((point, idx) => (
                     <li key={idx} className="text-gray-300 text-sm flex gap-2">
                         <span className="text-yellow-500/50">•</span> {point}
                     </li>
                 ))}
             </ul>
          </div>
      </div>

      {/* Recommendation */}
      <div className="bg-night-800 p-6 rounded-2xl border border-night-700 border-l-4 border-l-dream-500">
          <h3 className="text-white font-bold mb-2 flex items-center gap-2">
              <i className="fas fa-lightbulb text-dream-400"></i> Tipp für heute Nacht
          </h3>
          <p className="text-gray-300 leading-relaxed">
              {analysis.recommendation}
          </p>
      </div>
      
      <div className="text-xs text-center text-gray-600 mt-4">
          Generiert von Google Gemini. KI kann Fehler machen.
      </div>
    </div>
  );
};

export default AICoach;