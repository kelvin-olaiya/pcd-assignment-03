package pcd.assignment03.ex2

import com.google.gson.{Gson, JsonDeserializationContext, JsonDeserializer, JsonElement, JsonObject, JsonPrimitive, JsonSerializationContext, JsonSerializer}
import pcd.assignment03.ex2.pixelart.Brush

import java.lang.reflect
import java.util.UUID
import scala.collection.concurrent.TrieMap

object MapTypeAdapter extends JsonSerializer[TrieMap[UUID, Brush]] with JsonDeserializer[TrieMap[UUID, Brush]] {
  override def serialize(src: TrieMap[UUID, Brush], typeOfSrc: reflect.Type, context: JsonSerializationContext): JsonElement = {
    val jsonObject = new JsonObject()
    for ((key, value) <- src) {
      val jsonKey = new JsonPrimitive(key.toString)
      val jsonValue = context.serialize(value)
      jsonObject.add(jsonKey.getAsString, jsonValue)
    }
    jsonObject
  }

  override def deserialize(json: JsonElement, typeOfT: reflect.Type, context: JsonDeserializationContext): TrieMap[UUID, Brush] = {
    val map = new scala.collection.mutable.HashMap[UUID, Brush]()
    val jsonObject = json.getAsJsonObject
    val entrySet = jsonObject.entrySet()
    val iterator = entrySet.iterator()
    while (iterator.hasNext) {
      val entry = iterator.next()
      val key = UUID.fromString(entry.getKey)
      println(entry.getValue)
      val value = Gson().fromJson(entry.getValue, classOf[Brush])
      map.put(key, value)
    }
    TrieMap.from(map)
  }
}
