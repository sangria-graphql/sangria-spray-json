package sangria.marshalling


import sangria.marshalling.sprayJson._
import sangria.marshalling.testkit._

import spray.json._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SprayJsonSupportSpec extends AnyWordSpec with Matchers with MarshallingBehaviour with InputHandlingBehaviour with ParsingBehaviour {
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

    behave like `input parser` (ParseTestSubjects(
      complex = """{"a": [null, 123, [{"foo": "bar"}]], "b": {"c": true, "d": null}}""",
      simpleString = "\"bar\"",
      simpleInt = "12345",
      simpleNull = "null",
      list = "[\"bar\", 1, null, true, [1, 2, 3]]",
      syntaxError = List("[123, FOO BAR")
    ))
  }

  val toRender = JsObject(
    "a" -> JsArray(JsNull, JsNumber(123), JsArray(JsObject("foo" -> JsString("bar")))),
    "b" -> JsObject(
      "c" -> JsBoolean(true),
      "d" -> JsNull))

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
