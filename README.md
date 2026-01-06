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

### License

This code is open source software licensed under the [Apache 2.0 License].

