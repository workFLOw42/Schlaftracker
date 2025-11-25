import React, { useState } from 'react';
import { User } from '../types';
import { saveUser, seedDemoData } from '../services/storageService';

interface AuthProps {
  onLogin: (user: User) => void;
}

const Auth: React.FC<AuthProps> = ({ onLogin }) => {
  const [name, setName] = useState('');
  const [isRegistering, setIsRegistering] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    // Simulate Google ID generation
    const newUser: User = {
      id: `user_${Date.now()}`,
      name: name,
      email: `${name.toLowerCase().replace(/\s/g, '.')}@gmail.com`,
      avatarUrl: `https://ui-avatars.com/api/?name=${encodeURIComponent(name)}&background=6366f1&color=fff`
    };

    saveUser(newUser);
    seedDemoData(newUser.id); // Add some fake data for the "Google" feeling
    onLogin(newUser);
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-night-900 p-4">
      <div className="bg-night-800 p-8 rounded-2xl shadow-2xl max-w-md w-full border border-night-700">
        <div className="text-center mb-8">
            <div className="w-16 h-16 bg-dream-500 rounded-full flex items-center justify-center mx-auto mb-4 text-2xl text-white">
                <i className="fas fa-moon"></i>
            </div>
          <h1 className="text-3xl font-bold text-white mb-2">SchlafGut</h1>
          <p className="text-night-400 text-sm">Dein intelligenter Schlafbegleiter</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-400 mb-2">
              Wie sollen wir dich nennen?
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full bg-night-900 border border-night-600 rounded-lg px-4 py-3 text-white focus:outline-none focus:border-dream-500 focus:ring-1 focus:ring-dream-500 transition-colors"
              placeholder="Dein Name"
              required
            />
          </div>

          <button
            type="submit"
            className="w-full bg-white text-night-900 font-bold py-3 px-4 rounded-lg hover:bg-gray-100 transition-colors flex items-center justify-center gap-2"
          >
            <img src="https://www.svgrepo.com/show/475656/google-color.svg" alt="Google" className="w-5 h-5" />
            {isRegistering ? 'Registrieren' : 'Mit Google anmelden'}
          </button>
          
          <p className="text-xs text-center text-gray-500 mt-4">
            (Hinweis: Dies ist eine Demo. Die "Google-Anmeldung" simuliert lediglich einen Benutzeraccount lokal.)
          </p>
        </form>
      </div>
    </div>
  );
};

export default Auth;