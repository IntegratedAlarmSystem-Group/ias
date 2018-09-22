package org.eso.ias.cdb.topology

/**
  * The topology of the IAS, composed of the Supervisor to run.
  *
  * This class is used to check if the graph of the nodes is a-cyclic.
  *
  * @param supervisorTopology The Supervisors to run
  */
class IasTopology(val supervisorTopology: List[SupervisorTopology]) {
  require(Option(supervisorTopology).isDefined && !supervisorTopology.isEmpty,"No supervisors to run")

  /** Human readable representation of the topology */
  override def toString = {
    val ret = new StringBuilder("Topology of the IAS: Supervisors ")
    ret.append(supervisorTopology.map(_.supervId).mkString(","))
    ret.toString()
  }

}
