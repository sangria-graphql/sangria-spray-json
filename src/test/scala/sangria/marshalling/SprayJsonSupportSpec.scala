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

  val toRender = JsObject(
    "a" → JsArray(JsNull, JsNumber(123), JsArray(JsObject("foo" → JsString("bar")))),
    "b" → JsObject(
      "c" → JsBoolean(true),
      "d" → JsNull))

  "InputUnmarshaller" should {
    "throw an exception on invalid scalar values" in {
      an [IllegalStateException] should be thrownBy
          SprayJsonInputUnmarshaller.getScalarValue(JsObject.empty)
    }

    "throw an exception on variable names" in {
      an [IllegalArgumentException] should be thrownBy
          SprayJsonInputUnmarshaller.getVariableName(JsString("$foo"))
    }

    "render JSON values" in {
      val rendered = SprayJsonInputUnmarshaller.render(toRender)

      rendered should be ("""{"a":[null,123,[{"foo":"bar"}]],"b":{"c":true,"d":null}}""")
    }
  }

  "ResultMarshaller" should {
    "render pretty JSON values" in {
      val rendered = SprayJsonResultMarshaller.renderPretty(toRender)

      println(rendered)
      rendered.replaceAll("\r", "") should be (
        """{
          |  "a": [null, 123, [{
          |    "foo": "bar"
          |  }]],
          |  "b": {
          |    "c": true,
          |    "d": null
          |  }
          |}""".stripMargin.replaceAll("\r", ""))
    }

    "render compact JSON values" in {
      val rendered = SprayJsonResultMarshaller.renderCompact(toRender)

      rendered should be ("""{"a":[null,123,[{"foo":"bar"}]],"b":{"c":true,"d":null}}""")
    }
  }
}
