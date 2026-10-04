import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

describe('MCP Configuration & Policy Integrity Tests', () => {
  const mcpDir = path.resolve(__dirname, '..');
  const mcpConfigPath = path.join(mcpDir, 'config/servers.example.json');
  const toolAllowlistPath = path.join(mcpDir, 'policies/tool-allowlist.json');
  const networkAllowlistPath = path.join(mcpDir, 'policies/network-allowlist.json');
  const filesystemPolicyPath = path.join(mcpDir, 'policies/filesystem-policy.json');

  test('1. MCP config contains pinned Playwright MCP (0.0.83) and placeholder Context7', () => {
    assert.ok(fs.existsSync(mcpConfigPath));
    const config = JSON.parse(fs.readFileSync(mcpConfigPath, 'utf8'));
    assert.ok(config.mcpServers.playwright.args.includes('@playwright/mcp@0.0.83'));
    assert.equal(config.mcpServers.context7.remoteUrl, 'https://mcp.context7.com/mcp');
  });

  test('2. Tool allowlist explicitly prohibits arbitrary shell and SQL execution', () => {
    assert.ok(fs.existsSync(toolAllowlistPath));
    const policy = JSON.parse(fs.readFileSync(toolAllowlistPath, 'utf8'));
    assert.equal(policy.defaultAction, 'deny');
    assert.ok(policy.deniedTools.includes('execute_shell'));
    assert.ok(policy.deniedTools.includes('query_any_sql'));
  });

  test('3. Network allowlist enforces privacy and bans 0.0.0.0', () => {
    assert.ok(fs.existsSync(networkAllowlistPath));
    const policy = JSON.parse(fs.readFileSync(networkAllowlistPath, 'utf8'));
    assert.ok(policy.bindings.deniedHostBindings.includes('0.0.0.0'));
    assert.equal(policy.dataPrivacyRules.allowHealthDataEgress, false);
  });

  test('4. Filesystem policy disallows unrestricted file access', () => {
    assert.ok(fs.existsSync(filesystemPolicyPath));
    const policy = JSON.parse(fs.readFileSync(filesystemPolicyPath, 'utf8'));
    assert.equal(policy.flags.allowUnrestrictedFileAccess, false);
  });
});
