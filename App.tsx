
import React, { useState, useEffect } from 'react';
import { User, SleepEntry, AppView } from './types';
import { getActiveUser, getEntries, saveEntry, setActiveUser, deleteEntry } from './services/storageService';
import Auth from './components/Auth';
import SleepLogger from './components/SleepLogger';
import Statistics from './components/Statistics';
import AICoach from './components/AICoach';
import SleepTimeline from './components/SleepTimeline';
import Profile from './components/Profile';

const App: React.FC = () => {
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [view, setView] = useState<AppView>(AppView.DASHBOARD);
  const [entries, setEntries] = useState<SleepEntry[]>([]);
  
  // New state for editing
  const [editingEntry, setEditingEntry] = useState<SleepEntry | null>(null);

  // Initial Load
  useEffect(() => {
    const storedUser = getActiveUser();
    if (storedUser) {
      setCurrentUser(storedUser);
      loadEntries(storedUser.id);
    }
  }, []);

  const loadEntries = (userId: string) => {
    setEntries(getEntries(userId));
  };

  const handleLogin = (user: User) => {
    setCurrentUser(user);
    loadEntries(user.id);
    setView(AppView.DASHBOARD);
  };

  const handleLogout = () => {
    setCurrentUser(null);
    setActiveUser(null);
    setEntries([]);
  };

  const handleUpdateUser = (updatedUser: User) => {
      setCurrentUser(updatedUser);
  };

  const handleSaveEntry = (entry: SleepEntry) => {
    saveEntry(entry);
    loadEntries(entry.userId);
    setEditingEntry(null);
    setView(AppView.DASHBOARD);
  };

  const handleDeleteEntry = (entryId: string) => {
      deleteEntry(entryId);
      if (currentUser) loadEntries(currentUser.id);
      setEditingEntry(null);
      setView(AppView.DASHBOARD);
  };

  const startEdit = (entry: SleepEntry) => {
      setEditingEntry(entry);
      setView(AppView.LOG_SLEEP);
  };

  const startNewLog = () => {
      setEditingEntry(null);
      setView(AppView.LOG_SLEEP);
  };

  const handleCancelLog = () => {
      setEditingEntry(null);
      setView(AppView.DASHBOARD);
  };

  // Calculations for Dashboard Summary (Night sleep only)
  const nightEntries = entries.filter(e => !e.isNap);
  const lastEntry = nightEntries.length > 0 ? nightEntries[0] : null;
  const avgDuration = nightEntries.length > 0 
    ? (nightEntries.reduce((acc, curr) => acc + curr.sleepDurationMinutes, 0) / nightEntries.length / 60).toFixed(1) 
    : '0.0';
  const avgQuality = nightEntries.length > 0 
    ? (nightEntries.reduce((acc, curr) => acc + curr.quality, 0) / nightEntries.length).toFixed(1) 
    : '0.0';

  if (!currentUser) {
    return <Auth onLogin={handleLogin} />;
  }

  return (
    <div className="min-h-screen bg-night-900 text-gray-100 pb-24 md:pb-0 font-sans">
      
      {/* Header */}
      <header className="bg-night-800 border-b border-night-700 sticky top-0 z-50">
        <div className="max-w-4xl mx-auto px-4 py-4 flex justify-between items-center">
          <div className="flex items-center gap-3">
             <div className="w-10 h-10 bg-dream-500 rounded-lg flex items-center justify-center text-white shadow-lg shadow-dream-500/20">
                <i className="fas fa-moon"></i>
             </div>
             <div>
                <h1 className="font-bold text-lg leading-none">SchlafGut</h1>
                <p className="text-xs text-gray-400">Hallo, {currentUser.name}</p>
             </div>
          </div>
          <div className="flex items-center gap-4">
            <button onClick={() => setView(AppView.PROFILE)} className="text-gray-400 hover:text-white transition-colors">
                <i className="fas fa-user-cog text-xl"></i>
            </button>
            <button onClick={handleLogout} className="text-gray-400 hover:text-white transition-colors">
                <i className="fas fa-sign-out-alt text-xl"></i>
            </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-4xl mx-auto px-4 py-6">
        
        {view === AppView.DASHBOARD && (
          <div className="space-y-6 animate-fade-in">
             {/* Summary Cards */}
             <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div className="bg-night-800 p-4 rounded-xl border border-night-700">
                   <p className="text-xs text-gray-400 mb-1">Letzte Nacht</p>
                   <p className="text-2xl font-bold text-white">
                      {lastEntry ? (lastEntry.sleepDurationMinutes / 60).toFixed(1) + ' h' : '-'}
                   </p>
                </div>
                <div className="bg-night-800 p-4 rounded-xl border border-night-700">
                   <p className="text-xs text-gray-400 mb-1">Qualität Ø</p>
                   <p className="text-2xl font-bold text-yellow-400">{avgQuality}</p>
                </div>
                <div className="bg-night-800 p-4 rounded-xl border border-night-700">
                   <p className="text-xs text-gray-400 mb-1">Dauer Ø</p>
                   <p className="text-2xl font-bold text-dream-400">{avgDuration} h</p>
                </div>
                <div className="bg-night-800 p-4 rounded-xl border border-night-700">
                   <p className="text-xs text-gray-400 mb-1">Einträge</p>
                   <p className="text-2xl font-bold text-white">{entries.length}</p>
                </div>
             </div>

             {/* Recent Entry List */}
             <div className="bg-night-800 rounded-2xl border border-night-700 overflow-hidden">
                <div className="p-4 border-b border-night-700 flex justify-between items-center">
                   <h2 className="font-bold">Verlauf</h2>
                </div>
                <div className="divide-y divide-night-700">
                   {entries.length === 0 ? (
                      <div className="p-8 text-center text-gray-500">
                        Keine Einträge vorhanden. Starte jetzt!
                      </div>
                   ) : (
                      entries.slice(0, 5).map(entry => (
                         <div key={entry.id} className="p-4 hover:bg-night-700/50 transition-colors group cursor-pointer" onClick={() => startEdit(entry)}>
                            <div className="flex items-start justify-between mb-3">
                                <div className="flex items-center gap-4">
                                   <div className={`w-10 h-10 rounded-full flex items-center justify-center font-bold ${entry.quality >= 8 ? 'bg-emerald-500/20 text-emerald-400' : entry.quality >= 5 ? 'bg-indigo-500/20 text-indigo-400' : 'bg-rose-500/20 text-rose-400'}`}>
                                      {entry.isNap ? <i className="fas fa-sun text-sm"></i> : entry.quality}
                                   </div>
                                   <div>
                                      <div className="flex items-center gap-2">
                                          <p className="text-white font-medium">
                                            {new Date(entry.date).toLocaleDateString('de-DE', { weekday: 'long', day: 'numeric', month: 'short' })}
                                            {entry.isNap && <span className="ml-2 text-xs bg-orange-500/20 text-orange-300 px-2 py-0.5 rounded">Mittagsschlaf</span>}
                                          </p>
                                          <i className="fas fa-pencil-alt text-xs text-gray-600 group-hover:text-dream-400 transition-colors"></i>
                                      </div>
                                      <p className="text-xs text-gray-400">
                                        {(entry.sleepDurationMinutes / 60).toFixed(1)}h Schlaf {entry.interruptionCount > 0 ? `• ${entry.interruptionCount} Wachphasen` : ''}
                                      </p>
                                   </div>
                                </div>
                                <div className="text-right">
                                   <span className="text-xs text-gray-500 block">Start: {new Date(entry.bedTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</span>
                                </div>
                            </div>
                            
                            {/* Visual Timeline in List */}
                            <div className="mt-2 pl-14">
                                <SleepTimeline entry={entry} height="h-2" showLabels={false} />
                            </div>
                         </div>
                      ))
                   )}
                </div>
                {entries.length > 5 && (
                    <div className="p-3 text-center border-t border-night-700">
                        <button onClick={() => setView(AppView.STATISTICS)} className="text-sm text-dream-400 hover:text-dream-300">Alle anzeigen</button>
                    </div>
                )}
             </div>
          </div>
        )}

        {view === AppView.LOG_SLEEP && currentUser && (
          <SleepLogger 
            userId={currentUser.id}
            existingEntries={entries} 
            initialData={editingEntry}
            defaultLatency={currentUser.settings?.defaultSleepLatency || 15}
            onSave={handleSaveEntry} 
            onCancel={handleCancelLog} 
            onDelete={handleDeleteEntry}
          />
        )}

        {view === AppView.STATISTICS && (
           <Statistics entries={entries} />
        )}

        {view === AppView.AI_COACH && (
           <AICoach entries={entries} />
        )}

        {view === AppView.PROFILE && currentUser && (
            <Profile 
                user={currentUser} 
                onUpdateUser={handleUpdateUser} 
                onBack={() => setView(AppView.DASHBOARD)} 
            />
        )}

      </main>

      {/* Bottom Navigation */}
      <nav className="fixed bottom-0 left-0 right-0 bg-night-800 border-t border-night-700 md:hidden z-50">
        <div className="flex justify-around items-center h-16">
          <button 
             onClick={() => setView(AppView.DASHBOARD)}
             className={`flex flex-col items-center justify-center w-full h-full ${view === AppView.DASHBOARD ? 'text-dream-400' : 'text-gray-500'}`}
          >
             <i className="fas fa-home text-lg mb-1"></i>
             <span className="text-[10px]">Home</span>
          </button>
          <button 
             onClick={() => setView(AppView.STATISTICS)}
             className={`flex flex-col items-center justify-center w-full h-full ${view === AppView.STATISTICS ? 'text-dream-400' : 'text-gray-500'}`}
          >
             <i className="fas fa-chart-bar text-lg mb-1"></i>
             <span className="text-[10px]">Stats</span>
          </button>
          <button 
             onClick={startNewLog}
             className="relative -top-5 bg-dream-500 text-white rounded-full w-14 h-14 flex items-center justify-center shadow-lg shadow-dream-500/40 border-4 border-night-900"
          >
             <i className="fas fa-plus text-xl"></i>
          </button>
          <button 
             onClick={() => setView(AppView.AI_COACH)}
             className={`flex flex-col items-center justify-center w-full h-full ${view === AppView.AI_COACH ? 'text-dream-400' : 'text-gray-500'}`}
          >
             <i className="fas fa-magic text-lg mb-1"></i>
             <span className="text-[10px]">Coach</span>
          </button>
        </div>
      </nav>

      {/* Desktop Navigation FAB (Floating Action Button style for desktop) */}
      <div className="hidden md:flex fixed bottom-8 right-8 gap-4">
         <button 
           onClick={startNewLog}
           className="bg-dream-500 hover:bg-dream-400 text-white rounded-full px-6 py-4 shadow-lg flex items-center gap-2 font-bold transition-transform hover:scale-105"
         >
           <i className="fas fa-plus"></i> Schlaf eintragen
         </button>
         <div className="bg-night-800 rounded-full shadow-lg border border-night-700 p-2 flex">
             <button onClick={() => setView(AppView.DASHBOARD)} className={`p-3 rounded-full transition-colors ${view === AppView.DASHBOARD ? 'bg-night-700 text-dream-400' : 'text-gray-400 hover:text-white'}`} title="Dashboard"><i className="fas fa-home"></i></button>
             <button onClick={() => setView(AppView.STATISTICS)} className={`p-3 rounded-full transition-colors ${view === AppView.STATISTICS ? 'bg-night-700 text-dream-400' : 'text-gray-400 hover:text-white'}`} title="Statistik"><i className="fas fa-chart-bar"></i></button>
             <button onClick={() => setView(AppView.AI_COACH)} className={`p-3 rounded-full transition-colors ${view === AppView.AI_COACH ? 'bg-night-700 text-dream-400' : 'text-gray-400 hover:text-white'}`} title="AI Coach"><i className="fas fa-magic"></i></button>
         </div>
      </div>

    </div>
  );
};

export default App;
