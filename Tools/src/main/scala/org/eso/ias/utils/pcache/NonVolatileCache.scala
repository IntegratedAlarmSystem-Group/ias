package org.eso.ias.utils.pcache

/**
 * The trait for the cache of objects persisted in non-volatile memory
 *
 * This interface is te same as [[InMemoryCache]]: we keep both as they might diverge
 * in future and reduce teh chance of errors instantiating the higher level cache
 */
trait NonVolatileCache {
  /**
   * Puts/update a value in the cache
   *
   * @param key   The unique key to identify the object
   * @param value : The string to store in the cache
   * @return true if the object has been stored in the cache,
   *              false otherwise
   */
  def put(key: String, value: String): Boolean

  /**
   * Get an object from the cache
   *
   * @param key The key of the object
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

  /** @return the number of objects in the cache  */
  def size: Int
}
