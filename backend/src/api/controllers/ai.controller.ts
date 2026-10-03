import type { FastifyRequest, FastifyReply } from 'fastify';
import { AiService } from '../../services/index.js';
import { aiChatSchema } from '../schemas/index.js';

export class AiController {
  static async processChat(request: FastifyRequest, reply: FastifyReply) {
    const input = aiChatSchema.parse(request.body);
    const userId = request.user!.id;
    const isPremium = request.user!.isPremium;

    const result = await AiService.processChat(userId, isPremium, input.message, input.contextTopic);
    return reply.status(200).send(result);
  }
}
