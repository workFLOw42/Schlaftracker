import { GoogleGenAI, Type } from "@google/genai";
import { SleepEntry, GeminiAnalysis } from "../types";

const GEMINI_API_KEY = process.env.API_KEY || '';

export const analyzeSleepData = async (entries: SleepEntry[]): Promise<GeminiAnalysis> => {
  if (!GEMINI_API_KEY) {
    throw new Error("API Key is missing.");
  }

  const ai = new GoogleGenAI({ apiKey: GEMINI_API_KEY });
  
  // Prepare data for the prompt - only take the last 14 entries to fit context window comfortably and be relevant
  const recentEntries = entries.slice(0, 14).map(e => ({
    date: new Date(e.date).toLocaleDateString(),
    duration: (e.sleepDurationMinutes / 60).toFixed(1) + ' hours',
    wakeTime: e.wakeDurationMinutes + ' minutes',
    interruptions: e.interruptionCount,
    quality: e.quality + '/10',
    tags: e.tags.join(', ')
  }));

  const prompt = `
    Handle als Experte für Schlafforschung (Sleep Scientist).
    Analysiere die folgenden Schlafdaten eines Benutzers.
    Identifiziere Muster, Zusammenhänge zwischen Wachphasen/Qualität und gib konkrete Tipps.
    
    Daten: ${JSON.stringify(recentEntries)}
    
    Bitte antworte im JSON-Format gemäß dem Schema. Die Sprache muss Deutsch sein.
  `;

  try {
    const response = await ai.models.generateContent({
      model: "gemini-2.5-flash",
      contents: prompt,
      config: {
        responseMimeType: "application/json",
        responseSchema: {
          type: Type.OBJECT,
          properties: {
            summary: { type: Type.STRING, description: "Eine kurze Zusammenfassung der letzten Tage." },
            score: { type: Type.INTEGER, description: "Ein berechneter Schlaf-Score von 0 bis 100 basierend auf den Daten." },
            positivePoints: { 
              type: Type.ARRAY, 
              items: { type: Type.STRING },
              description: "Liste von 2-3 positiven Aspekten."
            },
            improvementAreas: {
              type: Type.ARRAY,
              items: { type: Type.STRING },
              description: "Liste von 2-3 Bereichen mit Verbesserungspotenzial."
            },
            recommendation: { type: Type.STRING, description: "Ein konkreter, umsetzbarer Tipp für die nächste Nacht." }
          }
        }
      }
    });

    const text = response.text;
    if (!text) throw new Error("Keine Antwort von Gemini erhalten.");
    
    return JSON.parse(text) as GeminiAnalysis;

  } catch (error) {
    console.error("Gemini Error:", error);
    // Return fallback data in case of error
    return {
      summary: "Entschuldigung, die KI-Analyse ist derzeit nicht verfügbar.",
      score: 0,
      positivePoints: [],
      improvementAreas: [],
      recommendation: "Bitte überprüfe deinen API-Schlüssel oder versuche es später erneut."
    };
  }
};