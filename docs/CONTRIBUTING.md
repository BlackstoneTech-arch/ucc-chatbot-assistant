# Contributing Guide

Thank you for your interest in contributing to the UCC Chatbot Assistant project. This document provides guidelines and instructions for contributing.

## Code of Conduct

- Be respectful and inclusive
- Focus on constructive feedback
- Prioritize accuracy of UCC information
- Follow the project's coding standards

## Getting Started

### 1. Fork the Repository

Click the "Fork" button on GitHub to create your own copy of the repository.

### 2. Clone Your Fork

```bash
git clone https://github.com/YOUR_USERNAME/ucc-chatbot-assistant.git
cd ucc-chatbot-assistant
```

### 3. Create a Branch

```bash
git checkout -b feature/your-feature-name
# or
git checkout -b fix/your-bug-fix
```

Branch naming conventions:
- `feature/` - New features
- `fix/` - Bug fixes
- `docs/` - Documentation updates
- `refactor/` - Code refactoring
- `test/` - Test additions or fixes

### 4. Set Up Development Environment

Follow the [Setup Guide](./SETUP.md) to configure your local environment.

## Development Workflow

### Making Changes

1. Make your changes in the appropriate directory
2. Follow the existing code style and conventions
3. Add tests for new functionality
4. Update documentation if needed

### Code Style

#### TypeScript/JavaScript
- Use TypeScript for all new code
- Follow ESLint configuration (`.eslintrc.cjs`)
- Use meaningful variable and function names
- Add JSDoc comments for public APIs

#### React Components
- Use functional components with hooks
- Keep components small and focused
- Use the project's Tailwind CSS classes
- Follow the existing component structure

#### Database
- Use parameterized queries (never string concatenation)
- Add indexes for new frequently-queried columns
- Update migrations for schema changes

### Testing

```bash
# Run all tests
npm test

# Run backend tests
npm test --workspace=backend

# Run tests in watch mode
npm run test:watch --workspace=backend
```

### Linting

```bash
# Lint all packages
npm run lint

# Lint backend
npm run lint --workspace=backend

# Type check
npm run typecheck
```

## Commit Guidelines

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>

[optional body]

[optional footer(s)]
```

### Types
- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation only
- `style:` - Formatting, missing semicolons, etc.
- `refactor:` - Code change that neither fixes a bug nor adds a feature
- `test:` - Adding or updating tests
- `chore:` - Maintenance tasks

### Examples
```
feat(backend): add hybrid search to knowledge base
fix(frontend): resolve chat widget scrolling issue
docs(api): update authentication endpoints
```

## Pull Request Process

### 1. Ensure Quality

Before submitting:
- [ ] Code compiles without errors
- [ ] Tests pass
- [ ] Linting passes
- [ ] Type checking passes
- [ ] Documentation is updated

### 2. Create Pull Request

- Use a clear, descriptive title
- Reference any related issues
- Describe the changes and rationale
- Include screenshots for UI changes

### 3. Review Process

- Maintainers will review your PR
- Address any requested changes
- Once approved, a maintainer will merge

## Adding Knowledge Base Content

### Document Format

Knowledge base documents are Markdown files in the `knowledge-base/` directory.

### Categories

Add documents to the appropriate category folder:
- `about-ucc/`
- `admissions/`
- `programmes/`
- `fees/`
- `academic/`
- `registration/`
- `examinations/`
- `accommodation/`
- `student-services/`
- `ict-support/`
- `professional-training/`
- `software-services/`
- `infrastructure/`
- `consulting/`
- `campuses/`
- `contacts/`
- `news/`
- `events/`
- `regulations/`
- `faqs/`

### Metadata

At the top of each document, include:

```markdown
---
title: Document Title
category: Category Name
academic_year: 2026/2027
source: Official UCC brochure
status: ACTIVE
---
```

### Important Rules

1. **Never invent UCC information** - Only add verified, official information
2. **Use official sources** - Reference https://ucc.co.tz/ and official documents
3. **Separate by academic year** - Keep information for different years separate
4. **Update documents, don't delete** - Use versioning and status changes
5. **Add source attribution** - Every factual claim should have a source

## Bug Reports

When reporting bugs, include:

1. **Description** - Clear description of the bug
2. **Steps to Reproduce** - Step-by-step instructions
3. **Expected Behavior** - What should happen
4. **Actual Behavior** - What actually happens
5. **Environment** - OS, browser, Node.js version
6. **Screenshots** - If applicable

## Feature Requests

When requesting features:

1. **Use Case** - Describe the problem you're solving
2. **Proposed Solution** - How you think it should work
3. **Alternatives** - Other solutions you've considered
4. **Impact** - Who benefits and how

## Questions?

- Open an issue for technical questions
- Contact the UCC ICT office for UCC-specific information
- Check existing documentation and issues first

## License

By contributing, you agree that your contributions will be licensed under the same license as the project.
