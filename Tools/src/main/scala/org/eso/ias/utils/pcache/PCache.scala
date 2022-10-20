package org.eso.ias.utils.pcache

import org.eso.ias.utils.pcache.impl.InMemoryCacheImpl

import java.util.Optional

/**
 * PCache is a cache that optionally persists objects in a DB or a file or other means
 * (depending on the implementation).
 *
 * Objects implementing this class store in memory up to n objects or up to
 * the size of the objects in memory passes a given threshold;
 * when the in-memory cache is filled, new objects are persisted.
 *
 * Giving a max number of objects  (or memory used by them) equals to 0 force all
 * objects to be persisted. The first threshold that is passed )(num. of items
 * or memory consumption) triggers the storage of new item in the non-volatile cache.
 *
 * The way objects are persisted depends on the implementation, it can be on file
 * or a RDBMS or other means) and it is transparent to the users.
 *
 * Each object persisted in PCache is identified by a key (pretty much like a Map).
 * In the scope of the IAS, it makes sense to store in the cache JSON strings
 * as almost everything can be translated to/from JSON strings and has an ID (IASIOs, CONVERTERS...).
 *
 * The memory used by the objects in the map consists on the sum of the sizes of
 * the keys plus the sizes of the JSON strings i.e. PCache does not take into account the memory
 * used to store the objects (for example if the objects are stored in a Map,
 * the memory does not consider the memory used by the Map itself).
 * There are more accurate solutions but we do not want to instrument the code or add complexity
 * when not strictly necessary.
 *
 * Besides the key and the JSON string, what PCache ultimately stores depends on
 * the memory and persistence implementations.
 *
 * There are many caches available on the open source world but actually in the IAS
 * the need for a cache is to store objects in memory avoiding the risk of OoM.
 * The objects will be put in cache and retrieved by their IDs (pretty much a single table of a RDBMS).
 * As such, PCache is basically a Map protected against OoM.
 *
 * PCache delegates the in-memory cache to implementers of [[InMemoryCache]] that can either use scala/java code
 * or delegate to third party open source tools.
 * For persisting objects in non-volatile memory, (implementers of [[NonVolatileCache]]), PCache delegates
 * to open source tools. Implementers of [[InMemoryCache]] can rely on external services (elasticsearch, redis...)
 * especially if such services are already available in the system.
 *
 * @param maxSize    The max number of items to keep in memory
 * @param maxMemSize The max size (MBytes) of memory that can be used by the object in memory
 *
 */
trait PCache(val maxSize: Integer = 0, val maxMemSize: Integer = 0) {

  /** The in memory cache */
  val inMemoryCache: InMemoryCache = new InMemoryCacheImpl(maxSize, maxMemSize)

  /**
   * Puts/update a value in the cache
   *
   * @param key   The unique key to identify the object
   * @param value : The string to store in the cache
   * @return true if the object has been stored in the cache,
   *              false otherwise
   * */
  def put(key: String, value: String): Boolean

  /**
   * Get an object from the cache
   *
   * @param key
   * @return The Object in the cache if it exists, empty otherwise
   */
  def get(key: String): Option[String]

  /**
   * Remove an object from the cache
   *
   * @param key The key of the object to remove
   * @return True if the object has been removed,
   *         False otherwise (for example the object was not in the cache)
   */
  def del(key: String): Boolean

  /**
   * Get an object from the cache
   * (support method to avoid that java code deeals with the scala Option)
   *
   * @param key
   * @return The Object in the cache if it exists, empty otherwise
   */
  def jget(key: String): Optional[String]

  /** @return the number of objects in the cache (both in memory and non-volatile) */
  def size: Int = inMemorySize + persistedSize

  /** @return the number of objects in memory */
  def inMemorySize: Int = inMemoryCache.size

  /** @return The number of objects persisted */
  def persistedSize: Int
}
