const db = require('../config/firebase');

/**
 * 1. Upload/Save a workout record (POST)
 * Path: /api/v1/workouts
 */
exports.uploadWorkout = async (req, res) => {
  try {
    const { userId, workoutTitle, durationMinutes, timestamp } = req.body;

    // Secure parameter validation matching data model rules
    if (userId === undefined || !workoutTitle || typeof durationMinutes !== 'number') {
      return res.status(400).json({
        error: 'Missing or invalid parameters. userId, workoutTitle, and durationMinutes are required.'
      });
    }

    const newWorkout = {
      userId: parseInt(userId, 10),
      workoutTitle: String(workoutTitle),
      durationMinutes: Number(durationMinutes),
      timestamp: Number(timestamp) || Date.now()
    };

    // Save record to the "workouts" collection in Firestore
    const docRef = await db.collection('workouts').add(newWorkout);

    return res.status(201).json({
      id: docRef.id,
      message: 'Workout synchronised successfully to Cloud Firestore.'
    });
  } catch (error) {
    console.error('Error uploading workout:', error);
    return res.status(500).json({ error: 'Internal Server Error' });
  }
};

/**
 * 2. Fetch workout records filtered by specific userId (GET)
 * Path: /api/v1/workouts?userId=XYZ
 */
exports.getWorkouts = async (req, res) => {
  try {
    const { userId } = req.query;

    if (!userId) {
      return res.status(400).json({ error: 'Query parameter userId is required.' });
    }

    const targetUserId = parseInt(userId, 10);

    // Fetch and filter via query index directly on Firebase
    const snapshot = await db.collection('workouts')
      .where('userId', '==', targetUserId)
      .orderBy('timestamp', 'desc')
      .get();

    const workouts = [];
    snapshot.forEach(doc => {
      workouts.push({
        id: doc.id,
        ...doc.data()
      });
    });

    return res.status(200).json(workouts);
  } catch (error) {
    console.error('Error fetching workouts:', error);
    return res.status(500).json({ error: 'Internal Server Error' });
  }
};