package org.eso.ias.cdb.topology

/**
  * The trait that defines a producer of an output.
  *
  * ASCEs and DASUs are producer: they have a set of inputs and produce
  * the output even if in different ways:
  * - the ASCE by the applying the transfer function
  * - DASU by propagating the inputs to the ASCEs
  *
  * To check if there are cycles, CyclesFinder build all the possible paths
  * from the inputs to the out collecting at each step, the output already produced.
  * A cycle is found if a producer generates an output that has already been produced.
  */
trait OutputProducer {

  /** The ID of the producer i.e. the ID of an ASCE or a DASU */
  val id: String

  /** The IDs of the inputs to produce the output */
  val inputsIds: Set[String]

  /** The ID of the output */
  val outputId: String
}

/**
  * Look for cycles.
  *
  * CiclesFinder is a generic class to look for cycles generated by ASCEs running in a DASU
  * or by the DASUs deploied in the system.
  *
  * @author acaproni
  */
class CyclesFinder(val inputsIds: Set[String], val producers: Set[OutputProducer]) {
  require(Option(inputsIds).isDefined && inputsIds.nonEmpty)
  require(Option(producers).isDefined && producers.nonEmpty)

  /**
    * Check if the producers create cycles
    *
    *  The method repeats the same test for each input
    */
  def isACyclic(): Boolean = {

    /**
      * The check is done checking each input and the
      * ASCEs that need it. Then the output produced
      * by a ASCE is checked against the ASCEs that need it
      * and so on until there are no more ASCEs.
      * The ASCEs and input already checked are put in the acc
      * set.
      *
      * For a given output x of an ASCE, acc contains
      * a possible path input, output) of the ASCE that prodiuced it.
      * The acc is a path from the input to the output so there is a cycle if an output of an ASCE
      * is contained in acc.
      *
      * A cycle is resent if the passed input
      * is already present in the acc set.
      *
      * @param in The input to check
      * @param acc The accumulator
      */
    def iasAcyclic(in: String, acc: Set[String]): Boolean = {
      // List of ASCEs that wants the passed input
      val prodsThatWantThisInput: Set[OutputProducer] = producers.filter(_.inputsIds.contains(in))

      // The outputs generated by all the producers that want this output
      val outputs: Set[String] = prodsThatWantThisInput.map(_.outputId)
      if (outputs.isEmpty) true
      else {
        outputs.forall(s => {
          val newSet = acc+in
          !newSet.contains(s) && iasAcyclic(s,newSet)
        })
      }
    }

    inputsIds.forall( inputId => iasAcyclic(inputId, Set()))

  }

}

