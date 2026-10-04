import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

describe('MCP Security, Policies & Pinned Tooling Tests', () => {
  const rootDir = path.resolve(process.cwd(), '..');
  const mcpConfigPath = path.join(rootDir, 'mcp/config/servers.example.json');
  const toolAllowlistPath = path.join(rootDir, 'mcp/policies/tool-allowlist.json');
  const networkAllowlistPath = path.join(rootDir, 'mcp/policies/network-allowlist.json');
  const filesystemPolicyPath = path.join(rootDir, 'mcp/policies/filesystem-policy.json');
  const gitignorePath = path.join(rootDir, '.gitignore');

  test('1. MCP config exists and contains pinned stable Playwright MCP and Context7 template', () => {
    assert.ok(fs.existsSync(mcpConfigPath), 'mcp/config/servers.example.json must exist');
    const content = JSON.parse(fs.readFileSync(mcpConfigPath, 'utf8'));

    assert.ok(content.mcpServers.playwright, 'Playwright MCP server must be configured');
    const playwrightArgs: string[] = content.mcpServers.playwright.args;
    assert.ok(playwrightArgs.includes('@playwright/mcp@0.0.83'), 'Playwright MCP must be pinned to 0.0.83');

    assert.ok(content.mcpServers.context7, 'Context7 MCP server must be configured');
    assert.equal(content.mcpServers.context7.remoteUrl, 'https://mcp.context7.com/mcp');
  });

  test('2. MCP config contains strictly placeholders and NO real secrets or tokens', () => {
    const raw = fs.readFileSync(mcpConfigPath, 'utf8');
    assert.equal(raw.includes('AIza'), false, 'Must not contain Google API key');
    assert.equal(raw.includes('sk-'), false, 'Must not contain sk- API keys');
    assert.equal(raw.includes('ghp_'), false, 'Must not contain GitHub personal access tokens');
    assert.ok(raw.includes('${CONTEXT7_API_KEY}'), 'Must use environment placeholder for Context7 key');
  });

  test('3. MCP tool allowlist enforces deny-by-default and bans shell / raw SQL execution', () => {
    assert.ok(fs.existsSync(toolAllowlistPath), 'tool-allowlist.json must exist');
    const policy = JSON.parse(fs.readFileSync(toolAllowlistPath, 'utf8'));

    assert.equal(policy.defaultAction, 'deny');
    const denied: string[] = policy.deniedTools;
    assert.ok(denied.includes('execute_shell'), 'execute_shell must be explicitly denied');
    assert.ok(denied.includes('query_any_sql'), 'query_any_sql must be explicitly denied');
    assert.ok(denied.includes('run_arbitrary_code'), 'run_arbitrary_code must be explicitly denied');
    assert.ok(denied.includes('dump_environment'), 'dump_environment must be explicitly denied');
    assert.ok(denied.includes('reveal_secrets'), 'reveal_secrets must be explicitly denied');
  });

  test('4. MCP network allowlist prohibits 0.0.0.0 and forbids user health data egress', () => {
    assert.ok(fs.existsSync(networkAllowlistPath), 'network-allowlist.json must exist');
    const policy = JSON.parse(fs.readFileSync(networkAllowlistPath, 'utf8'));

    assert.ok(policy.bindings.deniedHostBindings.includes('0.0.0.0'), '0.0.0.0 binding must be denied');
    assert.equal(policy.dataPrivacyRules.allowHealthDataEgress, false, 'Health data egress must be false');
    assert.equal(policy.dataPrivacyRules.allowUserJournalEgress, false, 'User journal egress must be false');
    assert.equal(policy.dataPrivacyRules.allowCredentialEgress, false, 'Credential egress must be false');
  });

  test('5. MCP filesystem policy forbids unrestricted file access and maintains sandbox', () => {
    assert.ok(fs.existsSync(filesystemPolicyPath), 'filesystem-policy.json must exist');
    const policy = JSON.parse(fs.readFileSync(filesystemPolicyPath, 'utf8'));

    assert.equal(policy.flags.allowUnrestrictedFileAccess, false);
    assert.equal(policy.flags.noSandbox, false);
    assert.equal(policy.flags.ignoreHttpsErrors, false);
    assert.equal(policy.filesystemBounds.workspaceRootOnly, true);
  });

  test('6. .gitignore protects local MCP secret files and Playwright test artifacts', () => {
    assert.ok(fs.existsSync(gitignorePath), '.gitignore must exist');
    const gitignore = fs.readFileSync(gitignorePath, 'utf8');

    assert.ok(gitignore.includes('.mcp.local.json'));
    assert.ok(gitignore.includes('mcp.local.json'));
    assert.ok(gitignore.includes('mcp/config/local.json'));
    assert.ok(gitignore.includes('.antigravity/local.json'));
    assert.ok(gitignore.includes('.antigravity/secrets.json'));
  });
});
