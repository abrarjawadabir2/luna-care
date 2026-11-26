import type { FastifyRequest, FastifyReply } from 'fastify';
import { AiService } from '../../services/index.js';
import { aiChatSchema } from '../schemas/index.js';

export class AiController {
  static async processChat(request: FastifyRequest, reply: FastifyReply) {
    const input = aiChatSchema.parse(request.body);
    const userId = request.user!.id;
    const isPremium = request.user!.isPremium;

    try {
      const result = await AiService.processChat(userId, isPremium, input.message, input.contextTopic);
      return reply.status(200).send(result);
    } catch (err: unknown) {
      if (err instanceof Error) {
        if (err.message === 'AI_QUOTA_EXCEEDED') {
          return reply.status(429).send({
            error: {
              code: 'AI_QUOTA_EXCEEDED',
              message: 'Monthly AI wellness message limit reached.',
              requestId: request.requestId
            }
          });
        }
        if (err.message === 'PROFILE_NOT_FOUND') {
          return reply.status(404).send({
            error: {
              code: 'PROFILE_NOT_FOUND',
              message: 'User profile not found.',
              requestId: request.requestId
            }
          });
        }
        if (err.message === 'AI_SERVICE_UNAVAILABLE') {
          return reply.status(503).send({
            error: {
              code: 'AI_SERVICE_UNAVAILABLE',
              message: 'AI chat is currently unavailable. Your message allowance was not used.',
              requestId: request.requestId
            }
          });
        }
      }
      throw err;
    }
  }
}
