package castor


import scala.collection.mutable

/**
 * A map from keys to collections of values: you can assign multiple values
 * to any particular key. Also allows lookups in both directions: what values
 * are assigned to a key or what key a value is assigned to.
 */

class MultiBiMap[K, V](){
  private[this] val valueToKeys = mutable.LinkedHashMap.empty[V, mutable.Set[K]]
  private[this] val keyToValues = mutable.LinkedHashMap.empty[K, mutable.Set[V]]
  def containsValue(v: V) = valueToKeys.contains(v)
  def add(k: K, v: V): Unit = {
    valueToKeys.getOrElse(v, mutable.Set()).add(k)
    keyToValues.getOrElseUpdate(k, mutable.Set()).add(v)
  }
  def removeAll(k: K): Set[V] = keyToValues.get(k) match {
    case None => Set()
    case Some(vs) =>
      vs.foreach(valueToKeys(_).remove(k))

      keyToValues.remove(k)
      vs.toSet
  }
}