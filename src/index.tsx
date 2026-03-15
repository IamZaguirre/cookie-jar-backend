import express from 'express';
import dotenv from 'dotenv';
import cors from 'cors';
import bodyParser from 'body-parser';
import { setUserRoutes } from './routes/userRoutes';
import { setProductRoutes } from './routes/productRoutes';
import { setOrderRoutes } from './routes/orderRoutes';
import { errorHandler } from './middlewares/errorHandler';

dotenv.config();

const app = express();
app.use(cors());
app.use(bodyParser.json());

// register routes (userRoutes.ts assumed present)
setUserRoutes(app);
setProductRoutes(app);
setOrderRoutes(app);

app.use(errorHandler);

const port = Number(process.env.PORT || 3000);
app.listen(port, () => {
  console.log(`Server listening on http://localhost:${port}`);
});