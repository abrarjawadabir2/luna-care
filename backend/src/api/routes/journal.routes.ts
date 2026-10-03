import type { FastifyPluginAsync } from 'fastify';
import { JournalController } from '../controllers/index.js';
import { requireAuth } from '../middleware/auth.js';

export const journalRoutes: FastifyPluginAsync = async (fastify) => {
  fastify.addHook('preHandler', requireAuth);

  // GET /api/v1/journal
  fastify.get('/', JournalController.getMedicalJournals);

  // POST /api/v1/journal
  fastify.post('/', JournalController.createMedicalJournal);

  // DELETE /api/v1/journal/:id
  fastify.delete('/:id', JournalController.deleteMedicalJournal);
};
