package org.eso.ias.cdb.topology

/**
  * The topology of the IAS, composed of the Supervisor to run.
  *
  * This class is used to check if the graph of the nodes is a-cyclic.
  *
  * @param dasus The DASUs to run in all the supervisors
  */
class IasTopology(val dasus: List[DasuTopology]) {
  require(Option(dasus).isDefined && dasus.nonEmpty,"No DASUs to run")

  /** The inputs of the alarm system are the inputs of all the DASUs */
  val alSysInputsIds = dasus.foldLeft(Set.empty[String])( (z,dasu) => z++dasu.inputsIds)

  /** Check if there are cycles in the IAS */
  def isAcyclic=CyclesFinder.isACyclic(alSysInputsIds,dasus.toSet)
}
