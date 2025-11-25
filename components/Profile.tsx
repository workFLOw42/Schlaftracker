
import React, { useState } from 'react';
import { User, AppView } from '../types';
import { saveUser } from '../services/storageService';

interface ProfileProps {
  user: User;
  onUpdateUser: (user: User) => void;
  onBack: () => void;
}

const Profile: React.FC<ProfileProps> = ({ user, onUpdateUser, onBack }) => {
  const [defaultLatency, setDefaultLatency] = useState(user.settings?.defaultSleepLatency || 15);
  const [successMsg, setSuccessMsg] = useState('');

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    const updatedUser = {
      ...user,
      settings: {
        ...user.settings,
        defaultSleepLatency: defaultLatency
      }
    };
    
    saveUser(updatedUser);
    onUpdateUser(updatedUser);
    setSuccessMsg('Einstellungen gespeichert!');
    setTimeout(() => setSuccessMsg(''), 3000);
  };

  return (
    <div className="bg-night-800 rounded-2xl p-6 shadow-xl border border-night-700 animate-fade-in">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-white flex items-center gap-2">
            <i className="fas fa-cog text-gray-400"></i> Einstellungen
        </h2>
        <button onClick={onBack} className="text-gray-400 hover:text-white">
            <i className="fas fa-times"></i>
        </button>
      </div>

      <div className="flex items-center gap-4 mb-8 border-b border-night-700 pb-6">
          <img src={user.avatarUrl} alt={user.name} className="w-16 h-16 rounded-full border-2 border-dream-500" />
          <div>
              <h3 className="text-xl font-bold text-white">{user.name}</h3>
              <p className="text-gray-400 text-sm">{user.email}</p>
          </div>
      </div>

      <form onSubmit={handleSave} className="space-y-6">
          <div>
              <label className="block text-gray-300 font-medium mb-2">
                  <i className="fas fa-stopwatch mr-2 text-dream-400"></i>
                  Standard Einschlafzeit (Minuten)
              </label>
              <p className="text-xs text-gray-500 mb-2">
                  Dieser Wert wird automatisch vor ausgefüllt, wenn du einen neuen Schlafeintrag erstellst.
              </p>
              <div className="flex items-center gap-4">
                  <input 
                      type="number" 
                      min="0" 
                      max="120"
                      value={defaultLatency}
                      onChange={(e) => setDefaultLatency(parseInt(e.target.value) || 0)}
                      className="w-24 bg-night-900 border border-night-600 rounded p-3 text-white focus:border-dream-500 outline-none text-center font-bold"
                  />
                  <span className="text-gray-400">Minuten</span>
              </div>
          </div>

          <button
             type="submit"
             className="w-full py-3 bg-dream-500 hover:bg-dream-400 text-white font-bold rounded-lg transition-all"
           >
             Speichern
           </button>
           
           {successMsg && (
               <div className="bg-green-500/20 text-green-400 p-3 rounded text-center text-sm animate-fade-in">
                   {successMsg}
               </div>
           )}
      </form>
    </div>
  );
};

export default Profile;
