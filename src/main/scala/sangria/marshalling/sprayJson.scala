package sangria.marshalling

import spray.json._

import scala.util.Try

object sprayJson extends SprayJsonSupportLowPrioImplicits {

  implicit object SprayJsonResultMarshaller extends ResultMarshaller {
    type Node = JsValue
    type MapBuilder = ArrayMapBuilder[Node]

    def emptyMapNode(keys: Seq[String]) = new ArrayMapBuilder[Node](keys)
    def addMapNodeElem(builder: MapBuilder, key: String, value: Node, optional: Boolean) = builder.add(key, value)

    def mapNode(builder: MapBuilder) = JsObject(builder.toMap)
    def mapNode(keyValues: Seq[(String, JsValue)]) = JsObject(keyValues: _*)

    def arrayNode(values: Vector[JsValue]) = JsArray(values.toVector)
    def optionalArrayNodeValue(value: Option[JsValue]) = value match {
      case Some(v) => v
      case None => nullNode
    }

    def scalarNode(value: Any, typeName: String, info: Set[ScalarValueInfo]) = value match {
      case v: String => JsString(v)
      case v: Boolean => JsBoolean(v)
      case v: Int => JsNumber(v)
      case v: Long => JsNumber(v)
      case v: Float => JsNumber(v)
      case v: Double => JsNumber(v)
      case v: BigInt => JsNumber(v)
      case v: BigDecimal => JsNumber(v)
      case v => throw new IllegalArgumentException("Unsupported scalar value: " + v)
    }

    def enumNode(value: String, typeName: String) = JsString(value)

    def nullNode = JsNull

    def renderCompact(node: JsValue) = node.compactPrint
    def renderPretty(node: JsValue) = node.prettyPrint
  }

  implicit object SprayJsonMarshallerForType extends ResultMarshallerForType[JsValue] {
    val marshaller = SprayJsonResultMarshaller
  }

  implicit object SprayJsonInputUnmarshaller extends InputUnmarshaller[JsValue] {
    def getRootMapValue(node: JsValue, key: String) = node.asInstanceOf[JsObject].fields get key

    def isListNode(node: JsValue) = node.isInstanceOf[JsArray]
    def getListValue(node: JsValue) = node.asInstanceOf[JsArray].elements

    def isMapNode(node: JsValue) = node.isInstanceOf[JsObject]
    def getMapValue(node: JsValue, key: String) = node.asInstanceOf[JsObject].fields get key
    def getMapKeys(node: JsValue) = node.asInstanceOf[JsObject].fields.keys

    def isDefined(node: JsValue) = node != JsNull
    def getScalarValue(node: JsValue) = node match {
      case JsBoolean(b) => b
      case JsNumber(d) => d.toBigIntExact getOrElse d
      case JsString(s) => s
      case _ => throw new IllegalStateException(s"$node is not a scalar value")
    }

    def getScalaScalarValue(node: JsValue) = getScalarValue(node)

    def isEnumNode(node: JsValue) = node.isInstanceOf[JsString]

    def isScalarNode(node: JsValue) = node match {
      case _: JsBoolean | _: JsNumber | _: JsString => true
      case _ => false
    }

    def isVariableNode(node: JsValue) = false
    def getVariableName(node: JsValue) = throw new IllegalArgumentException("variables are not supported")

    def render(node: JsValue) = node.compactPrint
  }

  private object SprayJsonToInput extends ToInput[JsValue, JsValue] {
    def toInput(value: JsValue) = (value, SprayJsonInputUnmarshaller)
  }

  private object SprayJsonFromInput extends FromInput[JsValue] {
    val marshaller = SprayJsonResultMarshaller
    def fromResult(node: marshaller.Node) = node
  }

  implicit def sprayJsonToInput[T <: JsValue]: ToInput[T, JsValue] =
    SprayJsonToInput.asInstanceOf[ToInput[T, JsValue]]

  implicit def sprayJsonFromInput[T <: JsValue]: FromInput[T] =
    SprayJsonFromInput.asInstanceOf[FromInput[T]]

  implicit def sprayJsonWriterToInput[T : JsonWriter]: ToInput[T, JsValue] =
    new ToInput[T, JsValue] {
      def toInput(value: T) = implicitly[JsonWriter[T]].write(value) -> SprayJsonInputUnmarshaller
    }

  implicit def sprayJsonReaderFromInput[T : JsonReader]: FromInput[T] =
    new FromInput[T] {
      val marshaller = SprayJsonResultMarshaller
      def fromResult(node: marshaller.Node) = try implicitly[JsonReader[T]].read(node) catch {
        case e: DeserializationException => throw InputParsingError(Vector(e.msg))
      }
    }

  implicit object SprayJsonInputParser extends InputParser[JsValue] {
    def parse(str: String) = Try(str.parseJson)
  }
}

trait SprayJsonSupportLowPrioImplicits {
  implicit val SprayJsonInputUnmarshallerJObject =
    sprayJson.SprayJsonInputUnmarshaller.asInstanceOf[InputUnmarshaller[JsObject]]
}
