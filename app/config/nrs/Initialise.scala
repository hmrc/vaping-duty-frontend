package config.nrs

import connectors.nrs.NrsConnector

import javax.inject.*
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class Initialise @Inject()(submissionConnector: NrsConnector) {

  // Delete this once confirmation of successful request.
  Await.result(submissionConnector.invalidSubmit, 20.seconds)
}