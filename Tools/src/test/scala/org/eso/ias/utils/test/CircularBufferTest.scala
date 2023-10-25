package org.eso.ias.utils.test

import org.eso.ias.logging.IASLogger
import org.eso.ias.utils.CircularBuffer
import org.scalatest.flatspec.AnyFlatSpec

/** Test the circular buffer */
class CircularBufferTest extends AnyFlatSpec {
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass);

  behavior of "The Circular buffer"

  it must "get the passed length" in {
    val c1 = CircularBuffer[String](100)
    assert(c1.length == 100)
  }

  it must "update the size when new elements are added/removed" in {
    val c = CircularBuffer[Int](5)
    c.put(1)
    assert(c.size == 1)
    assert(c.get().isDefined)
    assert(c.size == 0)

    c.put(10)
    c.put(11)
    c.put(12)
    assert(c.size == 3)

    assert(c.get().isDefined)
    assert(c.get().isDefined)
    assert(c.size == 1)

    c.put(12)
    c.put(13)
    assert(c.size == 3)

    c.put(14)
    c.put(15)
    assert(c.size == 5)

    assert(c.get().isDefined)
    assert(c.size == 4)

    c.put(20)
    c.put(21)
    c.put(22)
    c.put(22)
    c.put(44)
    assert(c.size == 5)
  }

  it must "clear the buffer" in {
    val c = CircularBuffer[Int](5)
    c.put(1)
    assert(c.size == 1)
    assert(!c.isEmpty())
    assert(c.get().isDefined)
    assert(c.size == 0)
    assert(c.isEmpty())

    c.put(10)
    c.put(11)
    c.put(12)
    assert(c.size == 3)

    c.clear()
    assert(c.size == 0)
    assert(c.isEmpty())
  }

  it must "Return all the items at once" in {
    val c = CircularBuffer[Int](5)
    assert(c.getAll().isEmpty)

    c.put(1)
    val i = c.getAll()
    assert(c.isEmpty())
    assert(i.size==1)
    assert(i.head==1)

    c.put(10)
    c.put(11)
    c.put(12)
    val i2 = c.getAll()
    assert(i2.size==3)
    assert(c.isEmpty())

    c.put(20)
    c.put(21)
    c.put(22)
    c.put(23)
    c.put(24)
    c.put(25)
    c.put(26)
    c.put(27)
    assert(c.size == 5)
    val i3 = c.getAll()
    assert(i3.size==5)
    assert(c.isEmpty())
    assert(i3==List(23, 24, 25, 26, 27))
  }

  it must "push a list of values" in {
    val c = CircularBuffer[Int](4)
    c.putAll(List())
    assert(c.isEmpty())

    c.putAll(List(1,2,3))
    assert(c.size==3)

    c.putAll(List(10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21))
    assert(c.size==4)
    val l = c.getAll()
    assert(l==List(18, 19, 20, 21))

  }
}
