package org.eso.ias.asce.transfer;

import jep.Interpreter;
import jep.SharedInterpreter;
import org.eso.ias.asce.CompEleThreadFactory;
import org.eso.ias.types.Alarm;
import org.eso.ias.types.IASTypes;
import org.eso.ias.types.IasValidity;
import org.eso.ias.types.OperationalMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

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
     * The thread to run python code in jep
     *
     * jep requires that all python calls must run in the same thread
     */
    private final ExecutorService executor;

    /**
     * The properties
     */
    private final Optional<Properties> propertiesOpt;

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

        propertiesOpt=Optional.ofNullable(props);

        executor = Executors.newSingleThreadExecutor(new CompEleThreadFactory("Python code executor thread for ASCE "+cEleId));
    }

    /**
     * Run the TF by delegating to the python implementation in th eproper thread
     *
     * @param compInputs: the inputs to the ASCE
     * @param actualOutput: the actual output of the ASCE
     * @return the new output
     * @throws Exception
     */
    @Override
    public IasIOJ<T> eval(Map<String, IasIOJ<?>> compInputs, IasIOJ<T> actualOutput) throws Exception {
        Objects.requireNonNull(pythonInterpreter,"eval invoked by the python interpreter is null");
        logger.debug("Running python TF for ASCE {}",compElementRunningId);

        Callable<IasIOJ<T>>  callable = new Callable<IasIOJ<T>>() {
            @Override
            public IasIOJ<T> call() throws Exception {

                logger.debug("The thread will run the python TF for ASCE {}",compElementRunningId);
                IasIOJ<T> ret = PythonExecutorTF.this.evalPythonTask(compInputs,actualOutput);
                logger.debug("The python TF for ASCE {} terminated without errors",compElementRunningId);
                return ret;
            }
        };

        logger.debug("Submitting python TF for ASCE {}",compElementRunningId);
        Future<IasIOJ<T>> future = executor.submit(callable);

        IasIOJ<T> newOutput;
        try {
            logger.debug("Waiting for termination python TF task for ASCE {}",compElementRunningId);
            newOutput=future.get(1,TimeUnit.MINUTES);
            logger.debug("Python TF task for ASCE {} terminated",compElementRunningId);
        } catch (Exception e) {
            e.printStackTrace();;
            throw new Exception(
                    "Exception caught while waiting for the execution of the python TF task of ASCE "+compElementRunningId,
                    e);
        }
        return newOutput;
    }

    /**
     * Run the TF in the python code
     *
     * @param compInputs: the inputs to the ASCE
     * @param actualOutput: the actual output of the ASCE
     * @return the new output
     * @throws Exception
     */
    private IasIOJ<T> evalPythonTask(Map<String, IasIOJ<?>> compInputs, IasIOJ<T> actualOutput) throws Exception {
        logger.debug("Setting up params to send to python object for ASCE {}",compElementRunningId);
        pythonInterpreter.exec("inputs = {}");
        for (String key: compInputs.keySet()) {
            logger.debug("Converting input {}", key);

            pythonInterpreter.set("id", compInputs.get(key).getId());
            pythonInterpreter.set("runningId", compInputs.get(key).getFullrunningId());
            pythonInterpreter.set("mode", compInputs.get(key).getMode());
            pythonInterpreter.set("iasType", compInputs.get(key).getType());
            pythonInterpreter.set("validity", compInputs.get(key).getValidity());
            assert compInputs.get(key).getValue().isPresent() : "Input value shall not be empty";
            pythonInterpreter.set("value", compInputs.get(key).getValue().get());
            assert compInputs.get(key).productionTStamp().isPresent() : "Input production timestamp shall not be empty";
            pythonInterpreter.set("prodTStamp", compInputs.get(key).productionTStamp().get());


            pythonInterpreter.exec("props = {}");
            Map<String, String> props = compInputs.get(key).getProps();
            if (props != null && !props.isEmpty()) {
                for (String k : props.keySet()) {
                    pythonInterpreter.set("key", k);
                    pythonInterpreter.set("value", props.get(k));
                    pythonInterpreter.exec("props[key]=value");
                }
            }

            pythonInterpreter.exec("input = IASIO(id,runningId,mode,iasType,validity,value,prodTStamp,props)");
            pythonInterpreter.exec("inputs[id]=input");
        }
        logger.debug("Inputs ready to be sent to python TF");


        logger.debug("Converting output {}",actualOutput.getId());
        pythonInterpreter.set("id", actualOutput.getId());
        pythonInterpreter.set("runningId", actualOutput.getFullrunningId());
        pythonInterpreter.set("mode", actualOutput.getMode());
        pythonInterpreter.set("iasType", actualOutput.getType());
        pythonInterpreter.set("validity", actualOutput.getValidity());
        if (actualOutput.getValue().isPresent()) {
            pythonInterpreter.set("value", actualOutput.getValue().get());
        } else {
            pythonInterpreter.exec("value = None");
        }
        if (actualOutput.productionTStamp().isPresent()) {
            pythonInterpreter.set("prodTStamp", actualOutput.productionTStamp().get());
        } else {
            pythonInterpreter.exec("prodTStamp = None");
        }

        pythonInterpreter.exec("props = {}");
        Map<String,String> props = actualOutput.getProps();
        if (props!=null && !props.isEmpty()) {
            for (String key: props.keySet()) {
                pythonInterpreter.set("key", key);
                pythonInterpreter.set("value", props.get(key));
                pythonInterpreter.exec("props[key]=value");
            }
        }
        logger.debug("Output ready to be sent to python TF");

        pythonInterpreter.exec("actualOutput = IASIO(id,runningId,mode,iasType,validity,value,prodTStamp,props)");
        logger.debug("Invoking eval on the python TF for ASCE {}",compElementRunningId);
        pythonInterpreter.exec("out = pyTF.eval(inputs,actualOutput)");

        logger.debug("Decoding the output generated by the python TF of ASCE {}",compElementRunningId);

        T newValue = (T)pythonInterpreter.getValue("out.value");
        OperationalMode newMode = (OperationalMode) pythonInterpreter.getValue("out.mode");
        IASTypes newType = (IASTypes) pythonInterpreter.getValue("out.iasType");
        IasValidity newValidity = (IasValidity)pythonInterpreter.getValue("out.validity");
        logger.debug("Python TF produced value {} of class {}",newValue.toString(),newValue.getClass().getName());
        logger.debug("Python TF produced mode {} of class {}",newMode.toString(),newMode.getClass().getName());
        logger.debug("Python TF produced type {} of class {}",newType.toString(),newType.getClass().getName());
        logger.debug("Python TF produced validity {} of class {}",newValidity.toString(),newValidity.getClass().getName());


        // Save the properties in a java HashMap
        HashMap<String,String> newProps = new HashMap<>();
        pythonInterpreter.set("propsMap",newProps);
        pythonInterpreter.exec("for k,v in out.props.items():\n  propsMap.put(str(k),str(v))");

        Object str = pythonInterpreter.getValue("out");
        logger.debug("Props {} of class {}",str,str.getClass().getName());

        // jep return values as String so we have to convert them
        // to the proper Java type before updating the value of the output
        Object value=null;
        switch (newType) {
            case ALARM:
                String temp = (String)pythonInterpreter.getValue("out.value");

                int idx = temp.lastIndexOf('.');
                logger.debug("Temp = {}, index={}",temp,idx);
                if (idx>=0) {
                    temp =temp.substring(idx+1);
                }
                logger.debug("New temp = {}, index={}",temp,idx);
                value = Alarm.valueOf((String)temp);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported type "+newType);
        }


        return actualOutput.updateProps(newProps).updateValue((T)value);
    }

    @Override
    public void initialize(Set<IasioInfo> inputsInfo, IasioInfo outputInfo) throws Exception {
        logger.debug("Initializing python TF for {}",compElementRunningId);

        Callable<?>  callable = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                logger.debug("The thread will run the python code for ASCE {}",compElementRunningId);
                PythonExecutorTF.this.initializePythonTask(inputsInfo, outputInfo);
                logger.debug("The thread run the python code for ASCE {} terminated without errors",compElementRunningId);
                return null;
            }
        };


        logger.debug("Submitting initialization task of python TF for ASCE {}",compElementRunningId);
        Future<?> future = executor.submit(callable);


        try {
            logger.debug("Waiting for termination of initialization task of python TF for ASCE {}",compElementRunningId);
            future.get(1,TimeUnit.MINUTES);
            logger.debug("Initialization task of python TF for ASCE {} terminated",compElementRunningId);
        } catch (Exception e) {
            throw new Exception(
                    "Exception caught while waiting for the initialization of the python TF of ASCE "+compElementRunningId,
                    e);
        }

        logger.info("Python TF for {} initialized",compElementRunningId);
    }

    /**
     * Runs the initialize in the python code
     *
     * This method must be run with the executor because jep requires that
     * all python code must be run in the same thread
     */
    private void initializePythonTask(Set<IasioInfo> inputsInfo, IasioInfo outputInfo) throws Exception {
        pythonInterpreter = new SharedInterpreter();

        logger.debug("Python interpreter built for {}",compElementRunningId);
        pythonInterpreter.exec("from IasTransferFunction.Impls.MinMaxThreshold import MinMaxThreshold");

        // Build the python object that implements the TF
        pythonInterpreter.set("asceId",this.compElementId);
        pythonInterpreter.set("asceRunningId",this.compElementRunningId);
        pythonInterpreter.set("validityTimeFrame",this.validityTimeFrame);
        pythonInterpreter.exec("userProps = {}");
        propertiesOpt.ifPresent( props -> {
            props.keySet().forEach( k -> {});
        });
        pythonInterpreter.exec("pyTF = MinMaxThreshold(asceId,asceRunningId,validityTimeFrame,userProps)");
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
    }

    @Override
    public void shutdown() throws Exception {
        logger.debug("Shutting down python TF for {}",compElementRunningId);
        executor.shutdownNow();
        pythonInterpreter.close();
        logger.info("Java executor of python TF for {} is shut down",compElementRunningId);
    }
}
