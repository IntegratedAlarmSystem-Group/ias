/**
 * <code>org.eso.ias.asce.transfer</code> contains the 
 * classes to run (from scala) the java object provided by the user 
 * that elaborates the actual inputs to produce the output
 * of the component.
 * 
 * In particular, classes of this package:
 * <UL
 * 	<LI>converts scala inputs
 * 	<LI>invokes the user provided method to elaborate on the inputs
 * 	<LI>get the result
 * 	<LI>convert the provided result into a scala TypedMonitorPoint[A}
 *      that will be the new output of the component
 * </UL>
 * 
 * @author acaproni
 *
 */
package org.eso.ias.asce.transfer;