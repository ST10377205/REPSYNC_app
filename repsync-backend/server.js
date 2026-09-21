const express = require('express');
const dotenv = require('dotenv');
const workoutRoutes = require('./routes/workoutRoutes');

dotenv.config();

const app = express();
const PORT = process.env.PORT || 5000;

// Body parser middleware for handling incoming JSON structures
app.use(express.json());

// Register clean REST base routing matching modern client architectures
app.use('/api/v1', workoutRoutes);

// Health check endpoint
app.get('/health', (req, res) => {
  res.status(200).json({ status: 'UP', timestamp: new Date() });
});

app.listen(PORT, () => {
  console.log(`🚀 REST API Service running smoothly on port ${PORT}`);
});