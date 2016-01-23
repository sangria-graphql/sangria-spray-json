package sangria.marshalling

import org.scalatest.{Matchers, WordSpec}

import sangria.marshalling.sprayJson._
import sangria.marshalling.testkit.{InputHandlingBehaviour, MarshallingBehaviour}

import spray.json._

class SprayJsonSupportSpec extends WordSpec with Matchers with MarshallingBehaviour with InputHandlingBehaviour {
  object JsonProtocol extends DefaultJsonProtocol {
    implicit val commentFormat = jsonFormat2(Comment.apply)
    implicit val articleFormat = jsonFormat4(Article.apply)
  }

  "SprayJson integration" should {
    import JsonProtocol._

    behave like `value (un)marshaller` (SprayJsonResultMarshaller)

    behave like `AST-based input unmarshaller` (sprayJsonFromInput[JsValue])
    behave like `AST-based input marshaller` (SprayJsonResultMarshaller)

    behave like `case class input unmarshaller`
    behave like `case class input marshaller` (SprayJsonResultMarshaller)
  }

}
