package org.eso.ias.types;

/**
 * The types of an identifier.
 * <P>
 * There is a relationship between types, meaning that
 * a type can have zero, one or more types of parents of a specified type.
 * 
 * 
 *  
 * @author acaproni
 * @see Identifier
 */
public enum IdentifierType {
	
	/**
	 * The type for the monitored software system that
	 * produced a monitor point value or alarm
	 * <P>
	 * For example: [ID=ACS, type={@link #MONITORED_SOFTWARE_SYSTEM}]
	 * 
	 */
	MONITORED_SOFTWARE_SYSTEM(),
	
	/**
	 * The type of a plugin that retrieved
	 * a monitor point value or alarm from a monitored system
	 * <P>
	 * For example: [ID=ACS_Plugin, type={@link #PLUGIN}
	 */
	PLUGIN(MONITORED_SOFTWARE_SYSTEM),
	
	/**
	 * The type of the identifier for the converter
	 * that translates a value or alarm produced by a remote
	 * monitored system into a valid IAS data structure.
	 */
	CONVERTER(PLUGIN),
	
	/**
	 * The type of a supervisor identifier
	 */
	SUPERVISOR(),
	
	/**
	 * The type of a distributed unit identifier
	 */
	DASU(SUPERVISOR),
	
	/**
	 * The type of a computing element identifier
	 */
	ASCE(DASU),
	
	/**
	 * The type of IASIO identifier
	 */
	IASIO(CONVERTER,ASCE),
	
	/**
	 * SINK clients are those that get values from the core topic and perform some action
     * like sending email or saving in the LTDB.
     * The name, sink, comes from the fact that these clients do not put anything back in the kafka topics
     * but consumes events published there.
     *
     * The alarm system monitors this kind of clients and report alarms if they are not running.
	 */
	SINK(),

    /**
     * A generic client that like a engineering panel.
     *
     * The alarm system will not make any check on the functioning of this kind of clients
     * that are assumed not important for the functioning of the alarm system.
     *
     *
     */
	CLIENT();
	
	/**
	 * Possible parents of a identifier
	 */
	public final IdentifierType[] parents;
	
	/**
	 * Constructor 
	 * @param theParents The parents of the type
	 */
	private IdentifierType(IdentifierType... theParents) {
		parents = theParents;
	}
}
