import { beforeEach, afterEach, describe, test } from 'node:test';
import assert from 'node:assert/strict';
import type { FastifyInstance } from 'fastify';
import { buildApp } from '../src/app.js';
import { db } from '../src/database/index.js';
import { rateLimiter } from '../src/api/middleware/rateLimit.js';
import { isRealCalendarDate } from '../src/api/schemas/calendar-date.js';

describe('Realistic user journeys (synthetic users, local API only)', () => {
  let app: FastifyInstance;

  beforeEach(async () => {
    db.reset();
    rateLimiter.reset();
    app = await buildApp();
  });
  afterEach(async () => {
    await app.close();
  });

  test('two people sign up, log in, claim a unique username and keep health entries private', async () => {
    const createUser = async (email: string, name: string) => {
      const response = await app.inject({
        method: 'POST',
        url: '/api/v1/auth/register',
        payload: { email, password: 'ExamplePassword123!', displayName: name }
      });
      assert.equal(response.statusCode, 201, response.body);
      assert.ok(response.json().token);
      return response.json();
    };

    const a = await createUser('person-a-ux@example.test', 'Person A');
    const b = await createUser('person-b-ux@example.test', 'Person B');
    const aAuth = { authorization: `Bearer ${a.token}` };
    const bAuth = { authorization: `Bearer ${b.token}` };

    const login = await app.inject({
      method: 'POST', url: '/api/v1/auth/login',
      payload: { email: 'PERSON-A-UX@example.test', password: 'ExamplePassword123!' }
    });
    assert.equal(login.statusCode, 200, login.body);

    const claimed = await app.inject({
      method: 'PATCH', url: '/api/v1/users/me',
      headers: aAuth, payload: { username: 'CycleJourney' }
    });
    assert.equal(claimed.statusCode, 200, claimed.body);

    const availability = await app.inject({
      method: 'GET', url: '/api/v1/users/username-availability?username=cyclejourney'
    });
    assert.equal(availability.statusCode, 200);
    assert.equal(availability.json().available, false);

    const duplicated = await app.inject({
      method: 'PATCH', url: '/api/v1/users/me',
      headers: bAuth, payload: { username: 'CYCLEJOURNEY' }
    });
    assert.equal(duplicated.statusCode, 409);
    assert.equal(duplicated.json().error.message, 'Username is not available.');

    const period = await app.inject({
      method: 'POST', url: '/api/v1/periods',
      headers: aAuth, payload: { startDate: '2026-11-01', endDate: '2026-11-05', flowLevel: 'Medium' }
    });
    assert.equal(period.statusCode, 201, period.body);

    const journal = await app.inject({
      method: 'POST', url: '/api/v1/journal',
      headers: aAuth,
      payload: {
        entryDate: '2026-11-01', category: 'notes', title: 'Private record',
        painLevel: 3, symptoms: ['cramps']
      }
    });
    assert.equal(journal.statusCode, 201, journal.body);

    const bPeriods = await app.inject({ method: 'GET', url: '/api/v1/periods', headers: bAuth });
    const bJournal = await app.inject({ method: 'GET', url: '/api/v1/journal', headers: bAuth });
    assert.deepEqual(bPeriods.json().logs, []);
    assert.deepEqual(bJournal.json().entries, []);
    assert.match(String(bJournal.headers['cache-control']), /no-store/);

    const bDeletion = await app.inject({
      method: 'DELETE', url: `/api/v1/journal/${journal.json().id}`, headers: bAuth
    });
    assert.equal(bDeletion.statusCode, 403);
    assert.ok(db.getMedicalJournalById(journal.json().id));

    const aJournal = await app.inject({ method: 'GET', url: '/api/v1/journal', headers: aAuth });
    assert.equal(aJournal.statusCode, 200);
    assert.equal(aJournal.json().entries.length, 1);

    const publicView = await app.inject({ method: 'GET', url: '/api/v1/users/CycleJourney' });
    assert.equal(publicView.statusCode, 200, publicView.body);
    const responseText = publicView.body.toLowerCase();
    assert.doesNotMatch(responseText, /person-a-ux@example\.test|passwordhash|periodlogs|medicaljournal/);
  });

  test('impossible dates and end-before-start do not enter health records', async () => {
    const register = await app.inject({
      method: 'POST', url: '/api/v1/auth/register',
      payload: { email: 'calendar-ux@example.test', password: 'ExamplePassword123!', displayName: 'Calendar User' }
    });
    assert.equal(register.statusCode, 201, register.body);
    const headers = { authorization: `Bearer ${register.json().token}` };

    assert.equal(isRealCalendarDate('2028-02-29'), true);
    assert.equal(isRealCalendarDate('2027-02-29'), false);
    assert.equal(isRealCalendarDate('2026-13-20'), false);
    assert.equal(isRealCalendarDate('2026-11-01'), true);

    for (const payload of [
      { startDate: '2026-02-30', flowLevel: 'Light' },
      { startDate: '2026-11-07', endDate: '2026-11-01', flowLevel: 'Medium' }
    ]) {
      const response = await app.inject({
        method: 'POST', url: '/api/v1/periods', headers, payload
      });
      assert.equal(response.statusCode, 400, response.body);
    }

    for (const entryDate of ['2026-02-30', '2026-99-01']) {
      const response = await app.inject({
        method: 'POST', url: '/api/v1/journal',
        headers,
        payload: { entryDate, title: 'Invalid date', category: 'notes', painLevel: 3 }
      });
      assert.equal(response.statusCode, 400, response.body);
    }

    const journalInvalidAppointment = await app.inject({
      method: 'POST', url: '/api/v1/journal', headers,
      payload: {
        entryDate: '2026-11-01', nextAppointment: '2026-02-30',
        title: 'Appointment', category: 'notes', painLevel: 2
      }
    });
    assert.equal(journalInvalidAppointment.statusCode, 400);

    const [periods, journals] = await Promise.all([
      app.inject({ method: 'GET', url: '/api/v1/periods', headers }),
      app.inject({ method: 'GET', url: '/api/v1/journal', headers })
    ]);
    assert.deepEqual(periods.json().logs, []);
    assert.deepEqual(journals.json().entries, []);
  });

  test('browser CORS preflight completes for an allowed local origin', async () => {
    const response = await app.inject({
      method: 'OPTIONS',
      url: '/api/v1/auth/login',
      headers: {
        origin: 'http://localhost:3000',
        'access-control-request-method': 'POST',
        'access-control-request-headers': 'content-type'
      }
    });
    assert.ok(response.statusCode >= 200 && response.statusCode < 300, response.body);
    assert.equal(response.headers['access-control-allow-origin'], 'http://localhost:3000');
  });
});
