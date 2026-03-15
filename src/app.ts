import express from 'express';
import { json } from 'body-parser';
import userRoutes from './routes/userRoutes';
import errorHandler from './middlewares/errorHandler';

const app = express();

// Middleware
app.use(json());

// Routes
app.use('/api/users', userRoutes());

// Error handling middleware
app.use(errorHandler);

export default app;