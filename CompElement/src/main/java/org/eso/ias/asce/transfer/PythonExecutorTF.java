package org.eso.ias.asce.transfer;

import jep.Interpreter;
import jep.SharedInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
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
        Objects.requireNonNull(pythonInterpreter,"eval invoked by the python interpreter is null");
        logger.debug("Setting up params for python TF for ASCE {}",compElementRunningId);

        pythonInterpreter.exec("inputs = {}}");
        for (String key: compInputs.keySet()) {
            logger.debug("Converting input {}", key);

            pythonInterpreter.set("id", compInputs.get(key).getId());
            pythonInterpreter.set("runningId", compInputs.get(key).getFullrunningId());
            pythonInterpreter.set("mode", compInputs.get(key).getMode());
            pythonInterpreter.set("iasType", compInputs.get(key).getType());
            pythonInterpreter.set("validity", compInputs.get(key).getValidity());
            pythonInterpreter.set("value", compInputs.get(key).getValue());
            pythonInterpreter.set("prodTStamp", compInputs.get(key).productionTStamp());

            pythonInterpreter.exec("props = {}");
            Map<String, String> props = compInputs.get(key).getProps();
            if (props != null && !props.isEmpty()) {
                for (String k : props.keySet()) {
                    pythonInterpreter.set("key", k);
                    pythonInterpreter.set("value", props.get(k));
                    pythonInterpreter.exec("props[key]=value");
                }
            }

            pythonInterpreter.exec("input = IASIO(id,runningId,mode,iasType,validity,value,prodTStamp,props");
            pythonInterpreter.exec("inputs[id]=input");
        }


        logger.debug("Converting output {}",actualOutput.getId());
        pythonInterpreter.set("id", actualOutput.getId());
        pythonInterpreter.set("runningId", actualOutput.getFullrunningId());
        pythonInterpreter.set("mode", actualOutput.getMode());
        pythonInterpreter.set("iasType", actualOutput.getType());
        pythonInterpreter.set("validity", actualOutput.getValidity());
        pythonInterpreter.set("value", actualOutput.getValue());
        pythonInterpreter.set("prodTStamp", actualOutput.productionTStamp());

        pythonInterpreter.exec("props = {}");
        Map<String,String> props = actualOutput.getProps();
        if (props!=null && !props.isEmpty()) {
            for (String key: props.keySet()) {
                pythonInterpreter.set("key", key);
                pythonInterpreter.set("value", props.get(key));
                pythonInterpreter.exec("props[key]=value");
            }
        }

        pythonInterpreter.exec("actualOutput = IASIO(id,runningId,mode,iasType,validity,value,prodTStamp,props");

        logger.debug("Invoking the python TF of ASCE {}",compElementRunningId);


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
        pythonInterpreter.exec("from IasTransferFunction.IASIO import IASIO");
        pythonInterpreter.exec("from IasBasicTypes.IasType import IASType");
        pythonInterpreter.exec("inputs = []");
        logger.debug("Building python input infos for ASCE {}",compElementRunningId);
        for (IasioInfo inputInfo: inputsInfo) {
            logger.debug("Processing input:  [name={}, type={}] for ASCE {}",
                    inputInfo.iasioId(),inputInfo.iasioType(),compElementRunningId);

            Object typeName=null;
            pythonInterpreter.set("typeName", inputInfo.iasioType().toString());
            pythonInterpreter.set("typeId", inputInfo.iasioId());
            pythonInterpreter.exec("iasioType = IASType.fromString(typeName)");
            pythonInterpreter.exec("ci = IasioInfo(typeId,iasioType)");
            pythonInterpreter.exec("inputs.append(ci)");
        }
        logger.debug("Inputs processed for ASCE {}",compElementRunningId);
        logger.debug("Building python output info for ASCE {}",compElementRunningId);
        Object typeName=null;
        logger.debug("Processing output:  name={}, type={}",outputInfo.iasioId(),outputInfo.iasioType());
        pythonInterpreter.set("typeName", outputInfo.iasioType().toString());
        pythonInterpreter.set("typeId", outputInfo.iasioId());
        pythonInterpreter.exec("iasioType = IASType.fromString(typeName)");
        pythonInterpreter.exec("out = IasioInfo(typeId,iasioType)");

        logger.debug("Initializing python TF implementation of ASCE {}",compElementRunningId);
        pythonInterpreter.exec("pyTF.initialize(inputs,out)");

        logger.info("Python TF for {} initialized",compElementRunningId);
    }

    @Override
    public void shutdown() throws Exception {
        logger.debug("Shutting down python TF for {}",compElementRunningId);
    }
}
