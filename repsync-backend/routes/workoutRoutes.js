const express = require('express');
const router = express.Router();
const workoutController = require('../controllers/workoutController');

router.post('/workouts', workoutController.uploadWorkout);
router.get('/workouts', workoutController.getWorkouts);

module.exports = router;