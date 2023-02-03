package org.eso.ias.utils

import scala.reflect.ClassTag

/**
 * A circular buffer of a fixed size backed by an Array.
 *
 * The buffer does not allow to store null items.
 *
 * The position to read and write elements in the buffer is stored in the [[readPointer]] and [[writePointer]] pointers
 * (i.e. indexes of the array) to avoid moving objects from one position to another in the buffer.
 *
 * The CircularBuffer is thread safe.
 *
 * @tparam T the type of the objects stored in the buffer
 * @Constructor Build a circular buffer
 * @param maxBufferSize The max number of items to store in the circular buffer
 * @author acaproni
 * @since 13.0
 */
class CircularBuffer[T: ClassTag](maxBufferSize: Int) {
  require(maxBufferSize>0)

  /**
   * The pointer to read items from.
   * It points to the oldest item in the buffer. Hoewever the read ointer is valid
   * only if there are items in the buffer
   */
  private[this] var readPointer: Int = 0

  /** The pointer to write items into */
  private[this] var writePointer: Int = 0

  /** The array that backs up the circular buffer */
  private[this] val array: Array[T] = Array.ofDim[T](maxBufferSize)

  /** The number of items in the buffer */
  private[this] var itemsInBuffer: Int = 0

  /** True if the buffer is empty; false otherwise */
  def isEmpty(): Boolean = synchronized { itemsInBuffer==0 }

  /** @return the length of the buffer i.e. the max number of items that the buffer can store */
  def length: Int = array.length

  /** @return the number of objects in the circular buffer */
  def size: Int = synchronized { itemsInBuffer }

  /**
   * Put a new object in the circular buffer deleting the oldest one if the buffer is full
   *
   * @param item the object to add
   */
  def put(item: T): Unit = synchronized {
    require(Option(item).isDefined, "The circular buffer does not accept null objects")

    array(writePointer)=item
    val nextWrite = (writePointer+1)%array.length
    val nextRead = {
      if (itemsInBuffer==0) {  // The buffer is empty
        itemsInBuffer=itemsInBuffer+1
        writePointer
      } else if (itemsInBuffer==array.length) { // The buffer is full: the oldest element must be discarded
        nextWrite
      } else { // One element has been added but the buffer is not full
        itemsInBuffer=itemsInBuffer+1
        readPointer
      }
    }
    readPointer = nextRead
    writePointer = nextWrite
    // Check invariants
    assert(readPointer >= 0 && readPointer<array.length, s"Invalid read pointer $readPointer not in [-1, ${array.length}]")
    assert(writePointer >= 0 && writePointer<array.length, s"Invalid write pointer $writePointer not in [0, ${array.length}]")
    assert(itemsInBuffer>=0 && itemsInBuffer<=array.length, s"Wrong buffer size $itemsInBuffer not in [0, ${array.length}]")
  }

  /**
   * @return the oldest item in the buffer or None if the buffer is empty
   */
  def get(): Option[T] = synchronized {
    if (itemsInBuffer == 0) None // Empty buffer
    else {
      val ret = Some(array(readPointer))
      itemsInBuffer = itemsInBuffer-1
      readPointer = (readPointer+1)%array.length
      // Check invariants
      assert(readPointer >= 0 && readPointer<array.length, s"Invalid read pointer $readPointer not in [-1, ${array.length}]")
      assert(writePointer >= 0 && writePointer<array.length, s"Invalid write pointer $writePointer not in [0, ${array.length}]")
      assert(itemsInBuffer>=0 && itemsInBuffer<=array.length, s"Wrong buffer size $itemsInBuffer not in [0, ${array.length}]")
      ret
    }
  }

  /**
   * Put all the passed items in the buffer
   *
   * @param items the items to put in the buffer
   */
  def putAll(items: List[T]): Unit = synchronized {
    items.foreach(put(_))
  }

  /**
   * @return the items in the buffer
   */
  def getAll(): List[T] = synchronized {
    val numOfItems = itemsInBuffer
    val ret = for {
      i <- 1 to numOfItems
      v = get().get
    } yield (v)
    ret.toList
  }

  /** Remove all the elements from the buffer */
  def clear(): Unit = synchronized {
    readPointer = 0
    writePointer = 0
    itemsInBuffer  = 0
  }

  /**
   * Debug method to dump the content of the buffer and the value
   * the indexes in the array stored by the pointers
   */
  def dump(): Unit = synchronized {
    println(s"readP=$readPointer, writeP=$writePointer, itemsInBuffer=$itemsInBuffer: ${array.mkString(":")}")
  }
}

object CircularBuffer {

  /**
   * Builds a circular buffer
   *
   * @tparam T the type of the objects in the buffer
   * @param size the max number of items in the circular buffer
   */
  def apply[T: ClassTag](size: Int): CircularBuffer[T] = new CircularBuffer[T](size)
}
