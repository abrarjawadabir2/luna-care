import type { FastifyRequest, FastifyReply } from 'fastify';
import { AuditService } from '../../audit/index.js';

export class AuditController {
  static async getLogs(_request: FastifyRequest, reply: FastifyReply) {
    const logs = AuditService.getAuditLogs();
    return reply.status(200).send({
      total: logs.length,
      logs: logs.slice(-100)
    });
  }
}
