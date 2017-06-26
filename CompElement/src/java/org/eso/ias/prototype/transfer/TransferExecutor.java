package org.eso.ias.prototype.transfer;

import java.util.Properties;

import org.eso.ias.prototype.input.Identifier;

/**
 * 
 * TransferExecutor is the abstract for the
 * implementators of the transfer function in java.
 * 
 * @author acaproni
 *
 */
public abstract class TransferExecutor {
	
	/**
	 * The ID of the computational element that runs this
	 * transfer function
	 */
	protected final String compElementId;
	
	/**
	 * The ID of the computational element that runs this
	 * transfer function extended with the IDs of its parents
	 * 
	 * @see Identifier
	 */
	protected final String compElementRunningId;
	
	/**
	 * Properties for this executor.
	 */
	protected final Properties props;
	
	/**
	 * Constructor
	 * 
	 * @param cEleId: The id of the ASCE
	 * @param cEleRunningId: the running ID of the ASCE
	 * @param props The properties fro the executor
	 */
	public TransferExecutor(
			String cEleId, 
			String cEleRunningId,
			Properties props) {
		if (cEleId==null) {
			throw new NullPointerException("The ID is null!");
		}
		this.compElementId=cEleId;
		if (cEleRunningId==null) {
			throw new NullPointerException("The running ID is null!");
		}
		this.compElementRunningId=cEleRunningId;
		if (props==null) {
			throw new NullPointerException("The properties is null!");
		}
		this.props=props;
	}
	
	/**
	 * Initialize the BehaviorRunner.
	 * 
	 * The life cycle method is called once by the IAS and always before running eval.
	 * User initialization code goes here. In particular long lasting operations
	 * like reading from a database should go here while eval is supposed 
	 * to return as soon as possible.
	 * 
	 * @throws Exception In case of error initializing
	 */
	public abstract void initialize() throws Exception;
	
	/**
	 * Shuts down the BehaviorRunner when the IAS does not need it anymore.
	 * 
	 * This life cycle method is called last, to clean up the resources.
	 * 
	 * It is supposed to return quickly, even if not mandatory.
	 * 
	 * @throws Exception In case of error shutting down
	 */
	public abstract void shutdown() throws Exception;
}
