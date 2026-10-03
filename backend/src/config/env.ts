import dotenv from 'dotenv';
import { z } from 'zod';

// Load environment variables safely
dotenv.config();

const envSchema = z.object({
  PORT: z.coerce.number().default(3000),
  NODE_ENV: z.enum(['development', 'production', 'test']).default('development'),
  HOST: z.string().default('127.0.0.1'),
  JWT_SECRET: z.string().min(32, 'JWT_SECRET must be at least 32 characters').default('development_jwt_secret_key_must_be_long_and_secure_min_32_chars'),
  SERVER_SECRET: z.string().min(32, 'SERVER_SECRET must be at least 32 characters').default('development_server_secret_for_purpose_separated_hmac_hashing'),
  INTERNAL_SERVICE_KEY: z.string().min(16).default('internal_dev_service_auth_key_16_chars'),
  GEMINI_API_KEY: z.string().optional(),
  DATABASE_URL: z.string().optional(),
  CORS_ORIGIN: z.string().default('http://localhost:3000'),
  RUST_INSPECTOR_PATH: z.string().default('./backend-rust/file-inspector'),
  AI_FREE_TIER_LIMIT: z.coerce.number().default(20),
  AI_PREMIUM_TIER_LIMIT: z.coerce.number().default(200)
});

export type Env = z.infer<typeof envSchema>;

let cachedEnv: Env | null = null;

export function getEnv(): Env {
  if (cachedEnv) return cachedEnv;

  const result = envSchema.safeParse(process.env);
  if (!result.success) {
    // SECURITY: Safe error reporting — log variable names that failed, NEVER print secret values
    const errorDetails = result.error.errors.map(e => `${e.path.join('.')}: ${e.message}`).join(', ');
    throw new Error(`Invalid environment configuration: ${errorDetails}`);
  }

  // Enforce production constraints
  if (result.data.NODE_ENV === 'production') {
    if (result.data.JWT_SECRET.includes('development') || result.data.SERVER_SECRET.includes('development')) {
      throw new Error('Production environment cannot use development secrets.');
    }
  }

  cachedEnv = result.data;
  return cachedEnv;
}
