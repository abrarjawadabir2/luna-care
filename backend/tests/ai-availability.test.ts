import { test } from 'node:test';
import assert from 'node:assert/strict';
import { buildApp } from '../src/app.js';
import { AuthService } from '../src/auth/index.js';
import { db } from '../src/database/index.js';
import { rateLimiter } from '../src/api/middleware/rateLimit.js';

test('a disconnected AI service never sends a fake answer or charges a message', async () => {
  const previous = process.env['GEMINI_API_KEY'];
  delete process.env['GEMINI_API_KEY'];
  db.reset();
  rateLimiter.reset();
  const app = await buildApp();

  try {
    const user = await AuthService.register({
      email: 'ai-offline-user@example.test',
      password: 'ExamplePassword123!',
      displayName: 'Synthetic AI User'
    });
    const initial = db.getProfile(user.user.id)?.aiMessagesUsedThisMonth;
    assert.equal(initial, 0);

    const response = await app.inject({
      method: 'POST',
      url: '/api/v1/ai/chat',
      headers: { authorization: `Bearer ${user.token}` },
      payload: {
        message: 'Where can I read general menstrual health information?',
        contextTopic: 'general'
      }
    });
    assert.equal(response.statusCode, 503, response.body);
    assert.equal(response.json().error.code, 'AI_SERVICE_UNAVAILABLE');
    assert.match(response.json().error.message, /allowance was not used/);
    assert.equal(db.getProfile(user.user.id)?.aiMessagesUsedThisMonth, initial);
    assert.equal(response.json().reply, undefined);
  } finally {
    await app.close();
    if (previous === undefined) delete process.env['GEMINI_API_KEY'];
    else process.env['GEMINI_API_KEY'] = previous;
  }
});
