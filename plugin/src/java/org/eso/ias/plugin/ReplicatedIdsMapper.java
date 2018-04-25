package org.eso.ias.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eso.ias.types.Identifier;

/**
 * The mapper of monitor points ID to concrete ID in case
 * the plugin is replicated.
 * <P>
 * The mapping follows the same rule of the IDs of the IASIOS of replicated
 * DASUs.
 * This is the place to implement more sophisticated mapping if needed in future.
 * 
 * @author acaproni
 *
 */
public class ReplicatedIdsMapper {
	
	/**
	 * The number of the instance of a replicated plugin.
	 */
	private final int instance;
	
	/**
	 * The cache of mapped IDs after applying the mapping
	 * avoids to build new strings every time a conversion is needed.
	 * <P> 	
	 * The IDs to be converted are just the plugin ID and the IDs 
	 * of the collected monitor points and alarms to send to the BSDB
	 * so, the cache will be full after initialization and will not grow
	 * anymore.
	 */
	private final Map<String, String> cache = new HashMap<>(); 
	
	/**
	 * Constructor 
	 * 
	 * @param instanceNumber The number of the instance of a replicated plugin; 
	 *                       empty if the plugin is not replicated.
	 */
	public ReplicatedIdsMapper(int instanceNumber) {
		if (instanceNumber<0) {
			throw new IllegalArgumentException("Invalid negative instance");
		}
		this.instance = instanceNumber;
	}
	
	/**
	 * Convert the passed identifier to a concrete identifier.
	 * <P>
	 * If the plugin is not templated, the ID is not changed
	 * but if it is replicated then the ID contains the number of
	 * the instance.
	 *  
	 * @param id the identifier to convert to a real identifier 
	 * @return the real ID if the plugin is replicated
	 */
	public String toRealId(String id) {
		if (Objects.isNull(id) || id.isEmpty()) {
			throw new IllegalArgumentException("Invalid nul or empty identifier");
		}
		// Is the realId already in the cache?
		Optional<String> newId = Optional.ofNullable(cache.get(id));
		return newId.orElseGet( () -> {
			String mappedId = Identifier.buildIdFromTemplate(id,instance); 
			cache.put(id, mappedId);
			return mappedId;
		});
	}
}
