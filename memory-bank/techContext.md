# Technical Context: Vaping Duty Frontend

## Technologies Used

### Core Technologies

- **Scala 3.3.6**: Programming language
- **Play Framework 3.0**: Web application framework
- **JRE 21+**: Java Runtime Environment
- **SBT**: Build tool for Scala projects

### Frontend Components

- **HMRC Frontend Components**: `play-frontend-hmrc-play-30` v12.31.0
- **GOV.UK Frontend**: For standardized government service UI components
- **Twirl Templates**: Server-side templating engine for HTML generation

### Backend & Infrastructure

- **Bootstrap Frontend**: `bootstrap-frontend-play-30` v10.5.0
- **MongoDB**: `hmrc-mongo-play-30` v2.12.0 for data persistence
- **HTTP Client**: Play's WS API for HTTP requests
- **Cats**: Functional programming library (`cats-core` v2.13.0)

### Testing

- **ScalaTest**: Main testing framework
- **Bootstrap Test**: `bootstrap-test-play-30` for frontend testing
- **MongoDB Test**: `hmrc-mongo-test-play-30` for testing with MongoDB
- **ScalaCheck**: Property-based testing via `scalacheck-1-18` v3.2.19.0

## Development Setup

### Local Development Environment

1. **Requirements**: JDK 21+, SBT

2. **Service Manager Configuration**:
   - Full stack: `sm2 --start VAPING_DUTY_ALL`
   - Frontend only: Stop SM service with `sm2 --stop VAPING_DUTY_FRONTEND` then run locally with `sbt run`

3. **Port Configuration**:
   - Frontend runs on port 8140
   - Related services:
     - Auth: 8500
     - Vaping Duty: 8141
     - Vaping Duty Account: 8144
     - Email Verification: 9891
     - Email Verification Frontend: 9890

### Build and Testing

1. **Build Commands**:
   - Full checks: `sbt runAllChecks` (clean, compile, test, IT test, style check, coverage)
   - Local checks: `sbt runLocalChecks` (clean, compile, test, style check, coverage)

2. **Coverage Requirements**:
   - Statement coverage: 76% minimum
   - Branch coverage: 72% minimum

3. **Testing Structure**:
   - Unit tests in `/test`
   - Integration tests in `/it`
   - Test utilities in `/test-utils`

## Technical Constraints

1. **Scala Version**: Must use Scala 3.3.6

2. **Accessibility**: Must comply with WCAG standards and provide both English and Welsh language support

3. **Coverage Thresholds**: Must maintain minimum coverage levels

4. **Session Timeout**: 
   - Session timeout: 900 seconds (15 minutes)
   - Timeout countdown: 120 seconds (2 minutes)

5. **MongoDB TTL**: Documents expire after 900 seconds (15 minutes)

## Dependencies and Integrations

### External Service Integrations

1. **Authentication Service**: 
   - Host: localhost:8500 (in dev)
   - Provides user authentication

2. **Vaping Duty Backend**:
   - Host: localhost:8141 (in dev)
   - Core business logic and data processing

3. **Vaping Duty Account**:
   - Host: localhost:8144 (in dev)
   - Account management functionality

4. **Email Verification**:
   - Host: localhost:9891 (in dev)
   - Email verification journeys
   - Endpoint: `/email-verification/verify-email`

5. **Email Verification Frontend**:
   - Host: localhost:9890 (in dev)
   - User-facing email verification pages

### Frontend Libraries

1. **HMRC Frontend**: Provides standardized GOV.UK components and styling

2. **Play Frontend HMRC**: Integrates HMRC-specific components with Play

3. **HMRC Auth Client**: Handles authentication with the HMRC auth service

### Configuration Management

1. **Application Configuration**: `conf/application.conf` for service settings

2. **Routes**: 
   - Main routes: `conf/app.routes`
   - Production routes: `conf/prod.routes`

3. **Messages**: 
   - English: `conf/messages.en`
   - Welsh: `conf/messages.cy`

## Tool Usage Patterns

1. **Service Manager**: Used to manage the full stack of services

2. **SBT**: Primary build and development tool
   - Custom command aliases for common tasks
   - Manages dependencies and compilation

3. **ScalaFmt**: Enforces code formatting standards via `.scalafmt.conf`

4. **ScalaStyle**: Enforces code quality via `scalastyle-config.xml`

5. **Coverage Tools**: Tracks test coverage with minimum thresholds

## Deployment Information

The service is deployed through standard HMRC deployment pipelines with configuration in `app-config-base`.