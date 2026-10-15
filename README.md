# Vaping Duty Frontend
This is the frontend microservice for the Vaping Duty service.

Backend: https://github.com/hmrc/vaping-duty

Account: https://github.com/hmrc/vaping-duty-account

Finance: https://github.com/hmrc/vaping-duty-finance

Stub: https://github.com/hmrc/vaping-duty-stubs

## Requirements
Written in Scala 3 with Play Framework and suitable to be run on JRE 21 or later.

## Running the service

### To run entirely under Service Manager
```
sm2 --start VAPING_DUTY_ALL
```

### To run locally
Launch as if running entirely under service manager above.

Stop the frontend service running under Service Manager:
```
sm2 --stop VAPING_DUTY_FRONTEND 
```

Start the service running locally:
```
sbt run
```

## Test the application

To run the full set of test suites with coverage reports:

```
sbt runAllChecks
```
To run the unit test suites with coverage reports:

```
sbt runLocalChecks
```

## Lint the application

If when running the checks above you encounter linting errors from scalafix, please use the folloiwng to correct these:

> Note that running this command will write changes to the project.

```bash
sbt scalafixAll
```

## Non-Repudiation Service (NRS)

### Overview
The NRS (Non-Repudiation Service) integration provides cryptographic proof that specific return submissions were made by authenticated users at specific times. This is a critical compliance requirement for tax submissions.

### Architecture
The frontend service implements NRS submission through an asynchronous work item pattern:

1. **Immediate Submission**: When a user submits a return via `DeclarationController`, the return is submitted to the backend immediately
2. **NRS Work Item Creation**: An `NrsSubmissionWorkItem` is created and stored in MongoDB for asynchronous processing
3. **Scheduled Processing**: The `NrsScheduledService` runs periodically to process pending work items
4. **NRS Submission**: Work items are processed by `NrsService`, which submits to the NRS API via `NrsConnector`
5. **Retry Logic**: Failed submissions are automatically retried according to the work item repository configuration

**Architecture Flow:**
```
Returns Submission (Primary Flow):
DeclarationController → SubmitReturnService → Backend API

NRS Submission (Parallel Flow):
DeclarationController → NrsService.queueSubmission()
                         ↓
                    NrsWorkItemRepository (MongoDB)
                         ↓
                    NrsScheduledService (Background)
                         ↓
                    NrsConnector
                         ↓
                    NRS API
```

### Components

#### NrsService
Location: `app/services/nrs/NrsService.scala`

Orchestrates NRS submission by:
- Building NRS metadata from return submission data
- Encoding the payload
- Creating work items for async processing
- Submitting to NRS via the connector
- Handling success/failure responses

#### NrsConnector
Location: `app/connectors/nrs/NrsConnector.scala`

Handles HTTP communication with the NRS API:
- Constructs NRS submission requests
- Manages authentication headers (X-API-Key)
- Handles response parsing and error cases
- Implements retry logic for transient failures

#### NrsWorkItemRepository
Location: `app/repositories/NrsWorkItemRepository.scala`

MongoDB repository for managing NRS work items:
- Stores pending submissions
- Tracks processing status
- Implements retry logic with exponential backoff
- Manages work item lifecycle
- Automatically expires old items based on TTL

#### NrsScheduledService
Location: `app/scheduling/NrsScheduledService.scala`

Scheduled job that:
- Runs at configured intervals
- Pulls pending work items from the repository
- Processes each item via NrsService
- Updates work item status based on results
- Handles failures and schedules retries

### Configuration

NRS configuration is managed in `conf/application.conf`:

```hocon
microservice.services.nrs {
  protocol = http
  host = localhost
  port = 9389
  api-key = "test-api-key"
}

nrs-scheduled-service {
  enabled = true
  interval = 30.seconds
  initial-delay = 5.seconds
}

mongodb.nrs-work-item {
  ttl = 30.days
  retry {
    initial-delay = 1.minute
    max-attempts = 5
    backoff-factor = 2
  }
}
```

### Data Models

#### NrsMetadata
Location: `app/models/nrs/NrsMetadata.scala`

Contains all metadata required for NRS submission:
- Business ID (VPD registration number)
- Notable event type
- Payload content type
- Payload SHA-256 checksum
- User authentication details
- Submission timestamp
- Identity data (name, email, etc.)
- User headers

#### NrsSubmissionWorkItem
Location: `app/models/nrs/NrsSubmissionWorkItem.scala`

Work item model for async processing:
- Submission ID
- NRS metadata
- Encoded payload
- Processing status
- Retry count
- Created/updated timestamps

### Testing

The NRS implementation includes comprehensive test coverage:

- **Unit Tests**: 
  - `test/services/nrs/NrsServiceSpec.scala`
  - `test/models/nrs/NrsMetadataSpec.scala`
- **Integration Tests**: 
  - `it/test/connectors/nrs/NrsConnectorISpec.scala`
  - `it/test/repositories/NrsWorkItemRepositoryISpec.scala`
  - `it/test/scheduling/NrsScheduledServiceISpec.scala`
- **Test Data**: `test-utils/data/NrsTestData.scala`

### Monitoring

Monitor NRS submission health by checking:
- Work item repository for stuck/failed items
- Application logs for NRS submission errors
- NRS API response times and error rates
- Scheduled service execution logs
- Work item processing metrics

### Troubleshooting

Common issues and solutions:

1. **Work items not processing**: 
   - Check that `nrs-scheduled-service.enabled = true`
   - Verify the scheduler is running in application logs
   - Check MongoDB connectivity

2. **Authentication failures**: 
   - Verify the NRS API key is correctly configured
   - Check the X-API-Key header is being sent

3. **Payload encoding errors**: 
   - Check that the return data is valid JSON before encoding
   - Verify the payload SHA-256 checksum is correct

4. **Stuck work items**: 
   - Items that exceed max retry attempts will remain in failed state
   - Investigate error logs for the specific work item
   - Manually resolve or delete if necessary

5. **High retry rates**:
   - Check NRS service availability
   - Review network connectivity
   - Verify retry configuration is appropriate

### License

This code is open source software licensed under the [Apache 2.0 License].

