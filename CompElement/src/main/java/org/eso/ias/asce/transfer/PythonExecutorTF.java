package org.eso.ias.asce.transfer;

import jep.Interpreter;
import jep.SharedInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Execution of TF in python is orchestrated by objects of this class:
 * when there is a python TF, this transfer function implementation is executed:
 * PythonExecutorTF delegates method execution to the python code through jep.
 *
 * That means that when python language for the TF is set in the configuration,
 * this class is always loaded by the ASCE. This class then build the python object
 * to which all the calls from ASCE are ultimately delegated.
 */
public class PythonExecutorTF<T> extends JavaTransferExecutor<T> {

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(PythonExecutorTF.class);

    /**
     * JEP python interpreter
     */
    private Interpreter pythonInterpreter;

    /**
     * The python object that implements the TF and to witch all methods
     * delegate
     */
    private Object pythonImpl = null;

    /**
     * Constructor
     *
     * @param cEleId            : The id of the ASCE
     * @param cEleRunningId     : the running ID of the ASCE
     * @param validityTimeFrame : The time frame (msec) to invalidate monitor points
     * @param props             : The properties for the executor
     */
    public PythonExecutorTF(String cEleId, String cEleRunningId, long validityTimeFrame, Properties props) {
        super(cEleId, cEleRunningId, validityTimeFrame, props);
        logger.debug("Python TF executor built for ASCE {}",compElementRunningId);
    }

    @Override
    public IasIOJ<T> eval(Map<String, IasIOJ<?>> compInputs, IasIOJ<T> actualOutput) throws Exception {
        return null;
    }

    @Override
    public void initialize(Set<IasioInfo> inputsInfo, IasioInfo outputInfo) throws Exception {
        logger.debug("Initializing python TF for {}",compElementRunningId);
        pythonInterpreter = new SharedInterpreter();

        logger.debug("Python interpreter built for {}",compElementRunningId);
        pythonInterpreter.exec("from IasTransferFunction.Impls.MinMaxThreshold import MinMaxThreshold");

        // Build the python object that implements the TF
        pythonInterpreter.set("asceId",this.compElementId);
        pythonInterpreter.set("asceRunningId",this.compElementRunningId);
        pythonInterpreter.set("validityTimeFrame",this.validityTimeFrame);
        pythonInterpreter.exec("pyTF = MinMaxThreshold(asceId,asceRunningId,validityTimeFrame,None)");
        pythonImpl = pythonInterpreter.getValue("pyTF");
        if (pythonImpl==null) {
            throw new Exception("Error building the python object for ASCE "+compElementRunningId);
        } else {
            logger.info("Python TF built for ASCE {}",compElementRunningId);
        }

        pythonInterpreter.exec("from IasTransferFunction.IasioInfo import IasioInfo");
        pythonInterpreter.exec("from IasBasicTypes.IasType import IASType");
        pythonInterpreter.exec("inputs = []");
        logger.debug("Building python input infos for ASCE {}",compElementRunningId);
        for (IasioInfo inputInfo: inputsInfo) {
            Object typeName=null;
            pythonInterpreter.set("typeName", inputInfo.iasioType().typeName);
            pythonInterpreter.exec("iasioType = IASType.fromString(typeName)");
            pythonInterpreter.exec("ci = IasioInfo("+inputInfo.iasioId()+",iasioType)");
            pythonInterpreter.exec("inputs.add(ci)");
        }
        logger.debug("Building python output info for ASCE {}",compElementRunningId);
        Object typeName=null;
        pythonInterpreter.set("typeName", outputInfo.iasioType().typeName);
        pythonInterpreter.exec("iasioType = IASType.fromString(typeName)");
        pythonInterpreter.exec("out = IasioInfo("+outputInfo.iasioId()+",iasioType)");

        logger.debug("Initililizing python TF implementation of ASCE {}",compElementRunningId);
        pythonInterpreter.exec("pyTF.initialize(inputs,out");

        logger.info("Python TF for {} initialized",compElementRunningId);
    }

    @Override
    public void shutdown() throws Exception {
        logger.debug("Shutting down python TF for {}",compElementRunningId);
    }
}
