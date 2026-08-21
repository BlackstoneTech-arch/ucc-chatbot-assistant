# Security Policy

## Security Overview

The UCC Chatbot Assistant handles sensitive university information and user data. Security is a top priority throughout the development lifecycle.

## Security Principles

1. **Least Privilege** - Users and services have only the permissions they need
2. **Defense in Depth** - Multiple layers of security controls
3. **Secure by Default** - Security features enabled by default
4. **Data Protection** - Protect data at rest and in transit
5. **Transparency** - Clear security policies and practices

## Threat Model

### Assets to Protect

| Asset | Sensitivity | Protection Level |
|-------|-------------|------------------|
| AI API Keys | High | Environment variables, never in frontend |
| JWT Secrets | High | Strong secrets, secure storage |
| Database Credentials | High | Environment variables, restricted access |
| User Conversations | Medium | Session isolation, no cross-user access |
| Knowledge Base | Medium | Access controls, audit logging |
| Admin Functions | High | Role-based access control |

### Threats

- **Data Breach** - Unauthorized access to sensitive data
- **Injection Attacks** - SQL injection, NoSQL injection
- **Authentication Bypass** - Unauthorized access to protected resources
- **Rate Limit Abuse** - DoS attacks, resource exhaustion
- **Information Disclosure** - Leaking sensitive information
- **Man-in-the-Middle** - Intercepting communications

## Security Controls

### Authentication

#### JWT-Based Authentication

```typescript
// Token structure
{
  "id": "user-uuid",
  "email": "user@example.com",
  "role": "admin",
  "iat": 1690000000,
  "exp": 1690600000
}
```

**Security Measures:**
- Strong JWT secret (minimum 32 characters, cryptographically random)
- Short expiration times (7 days for access tokens)
- Secure HTTP-only storage in production
- Refresh token rotation

#### Password Security

```typescript
// Password hashing with bcrypt
const passwordHash = await bcrypt.hash(password, 12);
```

- Bcrypt with cost factor 12
- Minimum password requirements enforced
- Never store plaintext passwords
- Never return passwords in API responses

### Authorization

#### Role-Based Access Control

| Role | Permissions |
|------|-------------|
| `user` | Chat, submit feedback |
| `admin` | Chat, manage knowledge, view analytics |
| `superadmin` | All permissions including user management |

**Implementation:**
```typescript
export const authorize = (...roles: string[]) => {
  return (req: AuthRequest, res: Response, next: NextFunction) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required' });
    }
    if (!roles.includes(req.user.role)) {
      return res.status(403).json({ error: 'Insufficient permissions' });
    }
    next();
  };
};
```

### Input Validation

All user inputs are validated:

```typescript
// Chat message validation
if (!message || typeof message !== 'string' || message.trim().length === 0) {
  return res.status(400).json({ error: 'Message is required' });
}

if (message.length > 2000) {
  return res.status(400).json({ error: 'Message too long' });
}
```

**Validation Rules:**
- Type checking for all inputs
- Length limits to prevent buffer overflow
- Format validation for emails, dates, etc.
- SQL injection prevention via parameterized queries
- XSS prevention via output encoding

### Rate Limiting

Two-tier rate limiting:

| Endpoint | Window | Limit |
|----------|--------|-------|
| General API | 15 minutes | 100 requests |
| Chat endpoint | 1 minute | 20 messages |

```typescript
const rateLimiter = () => {
  const windowMs = 900000; // 15 minutes
  const max = 100;
  const requests = new Map();
  
  return (req, res, next) => {
    const key = req.ip || 'unknown';
    const now = Date.now();
    
    const record = requests.get(key);
    if (!record || now > record.resetTime) {
      requests.set(key, { count: 1, resetTime: now + windowMs });
      return next();
    }
    
    record.count++;
    if (record.count > max) {
      return res.status(429).json({ error: 'Too many requests' });
    }
    
    next();
  };
};
```

### CORS Configuration

```typescript
const allowedOrigins = env.CORS_ORIGIN.split(',');
const origin = req.headers.origin;

if (allowedOrigins.includes(origin) || env.NODE_ENV === 'development') {
  res.header('Access-Control-Allow-Origin', origin);
}
```

- Whitelist of allowed origins
- Credentials only for trusted origins
- Preflight requests handled correctly

### Security Headers

```typescript
res.setHeader('X-Content-Type-Options', 'nosniff');
res.setHeader('X-Frame-Options', 'DENY');
res.setHeader('X-XSS-Protection', '1; mode=block');
res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains');
res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
res.setHeader('Permissions-Policy', 'geolocation=(), microphone=(), camera=()');
```

### SQL Injection Prevention

All database queries use parameterized statements:

```typescript
// Safe - parameterized query
const result = await pool.query(
  'SELECT * FROM users WHERE email = $1',
  [email]
);

// Never do this - string concatenation
const result = await pool.query(
  `SELECT * FROM users WHERE email = '${email}'`
);
```

### XSS Prevention

- Input sanitization on server
- Output encoding in responses
- Content Security Policy headers
- React's built-in JSX escaping

### CSRF Protection

- SameSite cookies where applicable
- CSRF tokens for state-changing operations
- Origin header validation

### HTTPS

Production deployments must use HTTPS:

- TLS 1.3 preferred, TLS 1.2 minimum
- Strong cipher suites only
- Valid SSL certificates
- HTTP to HTTPS redirect

### Secret Management

**Never do:**
- Commit secrets to version control
- Expose secrets in frontend code
- Log secrets in application logs
- Share secrets via insecure channels

**Always do:**
- Use environment variables
- Use `.env` files (gitignored)
- Rotate secrets regularly
- Use secret management services in production

```bash
# .gitignore must include
.env
.env.local
.env.*.local
*.pem
*.key
```

### Audit Logging

All sensitive operations are logged:

```typescript
await pool.query(
  'INSERT INTO audit_logs (user_id, action, resource_type, resource_id, ip_address) VALUES ($1, $2, $3, $4, $5)',
  [userId, 'DOCUMENT_UPDATE', 'document', documentId, req.ip]
);
```

**Logged Events:**
- Authentication (login, logout)
- Authorization failures
- Document changes
- FAQ creation/modification
- Admin actions
- API errors

### Data Isolation

User sessions are isolated:

```typescript
// Each conversation has a unique session_id
const sessionId = crypto.randomUUID();

// Messages are linked to conversations
// Conversations are linked to users (when authenticated)
// No cross-user data access
```

### Privacy Protection

**What we DON'T collect:**
- Passwords (only hashes)
- Examination results
- Bank details
- Payment credentials
- National identification
- Private academic records

**What we DO collect:**
- Chat messages (for service improvement)
- Anonymous usage statistics
- Feedback ratings

## Security Checklist

### Development

- [ ] No secrets in code or version control
- [ ] All inputs validated
- [ ] All queries parameterized
- [ ] Authentication on protected routes
- [ ] Authorization checked per role
- [ ] Rate limiting enabled
- [ ] Error messages don't leak sensitive info
- [ ] Logs don't contain sensitive data

### Production

- [ ] HTTPS enabled with valid certificate
- [ ] Strong JWT secret configured
- [ ] Database credentials secured
- [ ] Firewall configured
- [ ] Rate limiting enabled
- [ ] Security headers set
- [ ] CORS properly configured
- [ ] Regular backups scheduled
- [ ] Monitoring and alerting active

### Dependencies

- [ ] Dependencies up to date
- [ ] Security advisories checked
- [ ] `npm audit` passes
- [ ] No known vulnerabilities in dependencies

## Incident Response

### Security Incident Process

1. **Identify** - Detect and confirm the incident
2. **Contain** - Isolate affected systems
3. **Investigate** - Determine scope and impact
4. **Remediate** - Fix vulnerabilities
5. **Recover** - Restore normal operations
6. **Review** - Document lessons learned

### Reporting Security Issues

Report security vulnerabilities to:

- **Email:** security@ucc.co.tz
- **Subject:** [SECURITY] UCC Chatbot Assistant Vulnerability

Include:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)

## Compliance

### Data Protection

- User data collected only for stated purposes
- Data retention policies enforced
- Right to erasure supported
- Data portability where applicable

### UCC Policies

- Align with University of Dar es Salaam IT policies
- Follow Tanzania data protection regulations
- Respect user privacy rights

## Security Updates

- Monitor security advisories for all dependencies
- Apply security patches promptly
- Notify users of security incidents as appropriate
- Document security changes in CHANGELOG
