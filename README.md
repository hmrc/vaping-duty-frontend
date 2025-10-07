# Vaping Duty Frontend
This is the frontend microservice for the Vaping Duty service.

Backend: https://github.com/hmrc/vaping-duty

Stub: TCB

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
sbt 'run 8140'
```

## Test the application

To test the application execute:

```
sbt clean test it/test
```

### License

This code is open source software licensed under the [Apache 2.0 License].

