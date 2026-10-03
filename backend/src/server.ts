import { buildApp } from './app.js';
import { getEnv } from './config/index.js';
import { logger } from './utils/index.js';
import { scheduler } from './automation/index.js';

async function startServer() {
  try {
    const env = getEnv();
    const app = await buildApp();

    const address = await app.listen({
      port: env.PORT,
      host: env.HOST
    });

    // Start background automation scheduler
    scheduler.start();

    logger.info('server.started', {
      address,
      nodeEnv: env.NODE_ENV,
      port: env.PORT
    });

    // Graceful shutdown
    const signals: NodeJS.Signals[] = ['SIGINT', 'SIGTERM'];
    for (const signal of signals) {
      process.on(signal, async () => {
        logger.info('server.shutting_down', { signal });
        try {
          scheduler.stop();
          await app.close();
          logger.info('server.stopped');
          process.exit(0);
        } catch (err) {
          logger.error('server.shutdown_error', { error: err instanceof Error ? err.message : 'Unknown' });
          process.exit(1);
        }
      });
    }
  } catch (err) {
    logger.error('server.startup_failed', {
      error: err instanceof Error ? err.message : 'Unknown'
    });
    process.exit(1);
  }
}

if (process.env['NODE_ENV'] !== 'test') {
  startServer();
}
