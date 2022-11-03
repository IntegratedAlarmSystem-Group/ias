package org.eso.ias.utils.pcache.impl

import scala.collection.mutable
import org.eso.ias.utils.pcache.InMemoryCache

/** The in memory cache that delegates to [[HashMap]]
 *
 * This class is not thread safe.
 *
 * @param maxSize    The max number of items to keep in memory
 * @param maxMemSize The max size of memory (MBytes) that can be used by the object in memory
 */
class InMemoryCacheImpl(val maxSize: Integer=0, maxMemSize: Integer=0) extends InMemoryCache {
  require(maxSize >= 0 && maxMemSize >= 0)

  //* Max memory in bytes */
  val maxMemSizeBytes = 1048576 * maxMemSize

  /** The actual cache */
  val cache: collection.mutable.Map[String, CacheObj] = new mutable.HashMap[String, CacheObj]()

  /** The size in memory of the objects stored in cache */
  var memSize: Long = 0L

  /** The objects stored in cache with:
   * - The memory used to store the object
   * - Timestamp of last update
   * - The JSON string representing the object
   */
  case class CacheObj(val memSize: Long, val timestamp: Long, val jsonStr: String)

  /**
   * Puts/update a value in the cache
   *
   * @param key   The unique key to identify the object
   * @param value : The string to store in the cache
   * @return true if the object has been stored in the cache,
   *              false otherwise
   */
  override def put(key: String, value: String): Boolean = {
    require(key.nonEmpty && value.nonEmpty)
    def putItem(key: String, value: String): Unit = {
      val sz = key.size+value.size
      val obj = CacheObj(sz, System.currentTimeMillis(), value)
      val oldItem: Option[CacheObj] = cache.put(key, obj)
      if (oldItem.nonEmpty) memSize = memSize - oldItem.get.memSize
      memSize += sz
    }

    (maxSize, maxMemSizeBytes, cache.contains(key)) match {
      case (0, 0, _) => false
      case (n, 0, false) => if (size<n) { putItem(key, value); true } else false
      case (0, m, false) => if (memSize<m) { putItem(key, value); true } else false
      case (n, m, false)=> if (size<n && memSize<m) { putItem(key, value); true } else false
      case (_, _, true) => putItem(key, value); true
    }
  }

  /**
   * Get an object from the cache
   *
   * @param key The unique key to identify the object
   * @return The Object in the cache if it exists, empty otherwise
   */
  override def get(key: String): Option[String] = {
    require(key.nonEmpty)
    cache.get(key).map(_.jsonStr)
  }

  /**
   * Remove an object from the cache
   *
   * @param key The key of the object to remove
   * @return True if the object has been removed,
   *         False otherwise (for example the object was not in the cache)
   */
  override def del(key: String): Boolean = {
    require (key.nonEmpty)
    val removedObj = cache.remove(key)
    memSize = memSize-removedObj.map(_.memSize).getOrElse(0L)
    removedObj.nonEmpty
  }

  /** Collects all keys of this map in a set */
  override def keySet: Set[String] = cache.keySet.toSet

  /** @return the number of objects in this cache (both in memory and persisted) */
  def size: Int = cache.size

  /** Empty the cache */
  override def clear(): Unit = cache.clear

}
