cat << 'SERVER_EOF' >> backend/server.js

// --- Global Leaderboard Endpoints ---
const globalLeaderboard = [
  { username: "AlexReads", level: 12, xp: 5200 },
  { username: "BookWorm99", level: 10, xp: 4100 },
  { username: "LiteratureLover", level: 8, xp: 3200 },
  { username: "StudyMaster", level: 5, xp: 1800 },
  { username: "NoviceReader", level: 2, xp: 500 }
];

app.get('/api/leaderboard', (req, res) => {
  res.json({ leaderboard: globalLeaderboard });
});

app.post('/api/leaderboard', (req, res) => {
  const { username, level, xp } = req.body;
  if (!username || level === undefined || xp === undefined) {
    return res.status(400).json({ error: 'Missing username, level, or xp' });
  }
  
  const existing = globalLeaderboard.find(u => u.username === username);
  if (existing) {
    existing.level = Math.max(existing.level, level);
    existing.xp = Math.max(existing.xp, xp);
  } else {
    globalLeaderboard.push({ username, level, xp });
  }
  
  globalLeaderboard.sort((a, b) => b.xp - a.xp);
  
  res.json({ success: true, leaderboard: globalLeaderboard.slice(0, 100) });
});
SERVER_EOF
