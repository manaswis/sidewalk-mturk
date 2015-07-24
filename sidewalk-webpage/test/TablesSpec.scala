import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api._
import play.api.test.Helpers._
import play.api.test._

import play.api.db.slick.Config.driver.simple._
import models._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class SidewalkEdgeTableSpec extends Specification {
  val fakeApplicationWithGlobal = FakeApplication(withGlobal = Some(new GlobalSettings() {
    override def onStart(app: Application) { println("Hello world!") }
  }))

  // https://www.playframework.com/documentation/2.3.x/ScalaFunctionalTestingWithSpecs2
  // https://github.com/pvoznenko/play-slick-angular-test-example

  val appWithMemoryDatabase = FakeApplication(additionalConfiguration = inMemoryDatabase("test"))

  "SidewalkEdgeTable#save" should {
  }

  "SidewalkEdgeTable#all" should {
  }
}