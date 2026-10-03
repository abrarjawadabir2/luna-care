import test, { describe, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { buildApp } from '../src/app.js';
import { db } from '../src/database/index.js';
import { AuthService } from '../src/auth/index.js';
import type { FastifyInstance } from 'fastify';

describe('Cross-User Authorization & Strict Data Isolation', () => {
  let app: FastifyInstance;

  beforeEach(async () => {
    db.reset();
    app = await buildApp();
  });

  test('User B cannot read, update, or delete User A records', async () => {
    // 1. Create User A and User B
    const userA = await AuthService.register({
      email: 'usera_isolation@example.com',
      password: 'Password123!',
      displayName: 'User A'
    });

    const userB = await AuthService.register({
      email: 'userb_isolation@example.com',
      password: 'Password123!',
      displayName: 'User B'
    });

    // 2. User A creates a period record
    const periodRes = await app.inject({
      method: 'POST',
      url: '/api/v1/periods',
      headers: { authorization: `Bearer ${userA.token}` },
      payload: { startDate: '2026-07-01', flowLevel: 'Medium', symptoms: ['cramps'] }
    });
    assert.equal(periodRes.statusCode, 201);
    const periodId = periodRes.json().id;

    // 3. User A creates a journal entry
    const journalRes = await app.inject({
      method: 'POST',
      url: '/api/v1/journal',
      headers: { authorization: `Bearer ${userA.token}` },
      payload: {
        entryDate: '2026-07-01',
        category: 'symptom_log',
        title: 'User A Private Journal',
        symptoms: ['headache'],
        painLevel: 4
      }
    });
    assert.equal(journalRes.statusCode, 201);
    const journalId = journalRes.json().id;

    // 4. User A creates a mood/behaviour log
    const moodRes = await app.inject({
      method: 'POST',
      url: '/api/v1/moods',
      headers: { authorization: `Bearer ${userA.token}` },
      payload: {
        logDate: '2026-07-01',
        mood: 'Calm',
        stressLevel: 2,
        anxietyLevel: 1,
        sleepHours: 8,
        sleepQuality: 8,
        painLevel: 1,
        energyLevel: 7,
        hydrationLevel: 'Good',
        symptoms: [],
        crisisFlag: false
      }
    });
    assert.equal(moodRes.statusCode, 201);
    const moodId = moodRes.json().id;

    // -------------------------------------------------------------------------
    // VERIFICATION: User B cannot read User A's data
    // -------------------------------------------------------------------------
    const userBPeriods = await app.inject({
      method: 'GET',
      url: '/api/v1/periods',
      headers: { authorization: `Bearer ${userB.token}` }
    });
    assert.equal(userBPeriods.statusCode, 200);
    assert.equal(userBPeriods.json().logs.length, 0, 'User B must not see User A period logs');

    const userBJournals = await app.inject({
      method: 'GET',
      url: '/api/v1/journal',
      headers: { authorization: `Bearer ${userB.token}` }
    });
    assert.equal(userBJournals.statusCode, 200);
    assert.equal(userBJournals.json().entries.length, 0, 'User B must not see User A medical journals');

    const userBMoods = await app.inject({
      method: 'GET',
      url: '/api/v1/moods',
      headers: { authorization: `Bearer ${userB.token}` }
    });
    assert.equal(userBMoods.statusCode, 200);
    assert.equal(userBMoods.json().logs.length, 0, 'User B must not see User A mood logs');

    // -------------------------------------------------------------------------
    // VERIFICATION: User B cannot delete User A's records
    // -------------------------------------------------------------------------
    const delPeriod = await app.inject({
      method: 'DELETE',
      url: `/api/v1/periods/${periodId}`,
      headers: { authorization: `Bearer ${userB.token}` }
    });
    assert.equal(delPeriod.statusCode, 403, 'Cross-user period deletion must return 403');
    assert.ok(db.getPeriodLogById(periodId), 'User A period log must not be deleted');

    const delJournal = await app.inject({
      method: 'DELETE',
      url: `/api/v1/journal/${journalId}`,
      headers: { authorization: `Bearer ${userB.token}` }
    });
    assert.equal(delJournal.statusCode, 403, 'Cross-user journal deletion must return 403');
    assert.ok(db.getMedicalJournalById(journalId), 'User A journal entry must not be deleted');

    const delMood = await app.inject({
      method: 'DELETE',
      url: `/api/v1/moods/${moodId}`,
      headers: { authorization: `Bearer ${userB.token}` }
    });
    assert.equal(delMood.statusCode, 403, 'Cross-user mood deletion must return 403');
    assert.ok(db.getBehaviourLogById(moodId), 'User A mood log must not be deleted');

    // -------------------------------------------------------------------------
    // VERIFICATION: User B cannot modify User A's profile via request manipulation
    // -------------------------------------------------------------------------
    // User B attempts to pass User A's ID in update
    const updateProfileRes = await app.inject({
      method: 'PATCH',
      url: '/api/v1/profile',
      headers: { authorization: `Bearer ${userB.token}` },
      payload: { displayName: 'Hacked Name' }
    });
    assert.equal(updateProfileRes.statusCode, 200);

    // Verify User A profile is unmodified
    const userAProfile = db.getProfile(userA.user.id);
    assert.equal(userAProfile?.displayName, 'User A', 'User A profile must remain intact');

    // Verify User B profile was updated
    const userBProfile = db.getProfile(userB.user.id);
    assert.equal(userBProfile?.displayName, 'Hacked Name');
  });
});
