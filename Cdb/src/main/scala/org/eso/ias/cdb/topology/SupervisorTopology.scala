package org.eso.ias.cdb.topology

/**
  * The topology of the Supervisor contains the DASUs instantiated
  * in the Supervisor and can be used by the Supervisor to move the
  * inputs to the DASUs
  *
  * @param supervId: the identifier of the Supervisor
  * @param dasuTopology: the DASUs deployed in the Supervisor
  */
class SupervisorTopology(val supervId: String, dasuTopology: List[DasuTopology]) {
  require(Option(supervId).isDefined && !supervId.isEmpty,"Invalid empty Supervisor identifier")
  require(Option(dasuTopology).isDefined && !dasuTopology.isEmpty,"No DASUs for Supervisor "+supervId)

  /**
    * The inputs of each Dasu of the Supervisor.
    *
    * These are the inputs to forward to the DASUs that, in tunr, will forward them to its ASCEs.
    * As such, the outputs of the ASCEs are not part of supervisorInputs
    */
  val inputsOfDasus: Map[String,Set[String]] = dasuTopology.foldLeft(Map.empty[String,Set[String]])( (z,dasuTopology) =>
    z+(dasuTopology.id ->dasuTopology.inputsIds))

  /** The inputs of the Supervisor */
  val inputsSupervisor: Set[String] = inputsOfDasus.values.foldLeft(Set.empty[String])( (z,set) => z++set)

  /** The output produced by each DASU in the Supervisor */
  val outputsOfDasus: Map[String,String] = dasuTopology.foldLeft(Map.empty[String,String])( (z,dasuTopology) =>
    z+(dasuTopology.id -> dasuTopology.outputId))

  /** The outputs produced by all the DASU of the Supervisor */
  val supervisorOutputs = dasuTopology.foldLeft(Set.empty[String])( (z,dasuTopology) =>
    z+dasuTopology.outputId)

  require(supervisorOutputs.size==dasuTopology.size,"Supervisor "+supervId+" Number of outputs and number of DASU mismatch")

  /** Human readable representation of the topology */
  override def toString = {
    val ret = new StringBuilder(s"Topology of Supervisor [$supervId]: ")
    ret.append("\tDASUs: ")
    ret.append(inputsOfDasus.keySet.mkString(","))
    ret.append(",\n\tinputs: " + inputsSupervisor.mkString(","))
    ret.append(",\n\toutputs: " + supervisorOutputs.mkString(","))
    ret.toString()
  }
}
