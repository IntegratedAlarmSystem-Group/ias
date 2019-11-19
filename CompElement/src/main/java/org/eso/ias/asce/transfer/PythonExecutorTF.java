package org.eso.ias.asce.transfer;

import jep.Interpreter;
import jep.SharedInterpreter;
import org.eso.ias.asce.CompEleThreadFactory;
import org.eso.ias.types.*;
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
 * this class is always loaded by the ASCE.
 * Objects of this class then build the python object to which all the calls from ASCE are ultimately delegated.
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
     * The name of the python class to which this java TF delegates as received in the
     * constructor
     *
     * The name can or cannot have a dot ('.') and will be used by jep to import the class:
     * - pythonClassName=X translates to from X import X
     * - pythonClassName=Z.Y.X translates to from Z.Y.X import X
     */
    private final String pythonFullClassName;

    /**
     * The type of the output produced by this TF
     */
    private IASTypes outputType=null;

    /**
     * Constructor.
     *
     * This constructor differs from the other TFs because it needs to know the name of the python
     * class to load.
     *
     * @param cEleId            : The id of the ASCE
     * @param cEleRunningId     : the running ID of the ASCE
     * @param validityTimeFrame : The time frame (msec) to invalidate monitor points
     * @param props             : The properties for the executor
     * @param pythonClassName   : The name of the python class to which to delegate
     */
    public PythonExecutorTF(
            String cEleId,
            String cEleRunningId,
            long validityTimeFrame,
            Properties props,
            String pythonClassName) throws Exception  {
        super(cEleId, cEleRunningId, validityTimeFrame, props);

        if (pythonClassName==null || pythonClassName.isEmpty()) {
            throw new Exception("Missing python class name");
        }
        this.pythonFullClassName =pythonClassName;

        logger.debug("Python TF executor built for ASCE {}: will delegate to python TF {}",
                compElementRunningId,
                pythonClassName);

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
        actualOutput.getValue().ifPresent(v -> logger.debug("Actual output value {}",v));
        pythonInterpreter.exec("inputs = {}");
        for (String key: compInputs.keySet()) {
            logger.debug("Converting input {}", key);

            pythonInterpreter.set("id", compInputs.get(key).getId());
            pythonInterpreter.set("runningId", compInputs.get(key).getFullrunningId());
            pythonInterpreter.set("mode", compInputs.get(key).getMode());
            pythonInterpreter.set("iasType", compInputs.get(key).getType());
            pythonInterpreter.set("validity", compInputs.get(key).getValidity());
            assert compInputs.get(key).getValue().isPresent() : "Input value shall not be empty";

            // Translate the value of the input to a proper (possibly native) python type
            switch (compInputs.get(key).getType()) {
                case LONG:
                case INT:
                case SHORT:
                case BYTE:
                case TIMESTAMP:
                    pythonInterpreter.exec("iasValue=int("+compInputs.get(key).getValue().get()+")");
                    break;
                case DOUBLE:
                case FLOAT:
                    pythonInterpreter.exec("iasValue=float("+compInputs.get(key).getValue().get()+")");
                    break;
                case BOOLEAN:
                    if (compInputs.get(key).getValue().get()==Boolean.TRUE) {
                        pythonInterpreter.exec("iasValue=True");
                    } else {
                        pythonInterpreter.exec("iasValue=False");
                    }
                    break;
                case CHAR:
                    pythonInterpreter.exec("iasValue='"+((Character)(compInputs.get(key).getValue().get())).charValue()+"'");
                    break;
                case STRING:
                    pythonInterpreter.exec("iasValue='"+compInputs.get(key).getValue().get().toString()+"'");
                    break;
                case ARRAYOFDOUBLES:
                    NumericArray nad = (NumericArray)compInputs.get(key).getValue().get();
                    Double[] doubles=nad.toArrayOfDouble();
                    pythonInterpreter.exec("iasValue=[]");
                    for (Double num: doubles) {
                        pythonInterpreter.exec("iasValue.append(float("+num+"))");
                    }
                    break;
                case ARRAYOFLONGS:
                    NumericArray nal = (NumericArray)compInputs.get(key).getValue().get();
                    Double[] longs=nal.toArrayOfDouble();
                    pythonInterpreter.exec("iasValue=[]");
                    for (Double num: longs) {
                        pythonInterpreter.exec("iasValue.append(int("+num+"))");
                    }
                    break;
                case ALARM:
                    Alarm alarm = (Alarm)compInputs.get(key).getValue().get();
                    pythonInterpreter.exec("from IasBasicTypes.Alarm import Alarm");
                    switch (alarm) {
                        case CLEARED:
                            pythonInterpreter.exec("iasValue=Alarm.CLEARED");
                            break;
                        case SET_LOW:
                            pythonInterpreter.exec("iasValue=Alarm.SET_LOW");
                            break;
                        case SET_MEDIUM:
                            pythonInterpreter.exec("iasValue=Alarm.SET_MEDIUM");
                            break;
                        case SET_HIGH:
                            pythonInterpreter.exec("iasValue=Alarm.SET_HIGH");
                            break;
                        case SET_CRITICAL:
                            pythonInterpreter.exec("iasValue=Alarm.SET_CRITICAL");
                            break;
                    }
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported input type "+compInputs.get(key));
            }
            assert compInputs.get(key).productionTStamp().isPresent() : "Input production timestamp shall not be empty";
            pythonInterpreter.set("prodTStamp", compInputs.get(key).productionTStamp().get());


            pythonInterpreter.exec("props = {}");
            Map<String, String> props = compInputs.get(key).getProps();
            if (props != null && !props.isEmpty()) {
                for (String k : props.keySet()) {
                    pythonInterpreter.exec("props['"+k+"']='"+props.get(k)+"'");
                    logger.debug("Setting input property {}={}",k,props.get(k));
                }
            } else {
                logger.debug("No properties set in input {}",compInputs.get(key).getId());
            }

            pythonInterpreter.exec("input = IASIO(id,runningId,mode,iasType,validity,iasValue,prodTStamp,props)");
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
            pythonInterpreter.set("outputValue", actualOutput.getValue().get());
            pythonInterpreter.exec("print('*** ----> ',outputValue)");
        } else {
            pythonInterpreter.exec("outputValue = None");
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
        logger.debug("Output ready to be sent to python TF with value {}, mode {} type {}",
                pythonInterpreter.getValue("outputValue"),
                pythonInterpreter.getValue("mode"),
                pythonInterpreter.getValue("iasType"));

        pythonInterpreter.exec("actualOutput = IASIO(id,runningId,mode,iasType,validity,outputValue,prodTStamp,props)");
        logger.debug("Invoking eval on the python TF for ASCE {}",compElementRunningId);
        pythonInterpreter.exec("out = pyTF.eval(inputs,actualOutput)");

        logger.debug("Decoding the output generated by the python TF of ASCE {}",compElementRunningId);

        T newValue = (T)pythonInterpreter.getValue("out.value");

        String newModeStr = (String)pythonInterpreter.getValue("out.mode");
        int dotIidx = newModeStr.lastIndexOf('.');
        OperationalMode newMode;
        if (dotIidx!=-1) {
            newMode = OperationalMode.valueOf(newModeStr.substring(dotIidx+1));
        } else {
            newMode = OperationalMode.valueOf(newModeStr);
        }

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

        T value=convertPyOutputValueToJava(pythonInterpreter.getValue("out.value"));

        return actualOutput.updateProps(newProps).updateMode(newMode).updateValue(value);
    }

    /**
     * Converts a python object representing the value of an IASIO to java.
     *
     * jep defines a mapping between java and python but objects whose mapping does not
     * exist are mapped into strings: to be in the safe side, this code checks if the object is a string even for
     * the cases when the jep mapping is defined.
     * This method checks the type of the object to return the java object of the proper type.
     *
     * @param pyObj The python object usually retrieved with a jep getValue()
     * @return the java object
     */
    private T convertPyOutputValueToJava(Object pyObj) throws Exception {
        Objects.requireNonNull(outputType,"Unknown output type");
        Objects.requireNonNull(pyObj,"Can't convert a null value");

        // The object returned by jep is a String that must be converted to the proper
        // java object
        switch (outputType) {
            case LONG:
            case TIMESTAMP:
                if (pyObj instanceof String) {
                    return (T)Long.valueOf((String) pyObj);
                } else {
                    return (T)pyObj;
                }
            case INT:
                if (pyObj instanceof String) {
                    return (T)Integer.valueOf((String) pyObj);
                } else {
                    return (T)Integer.valueOf(((Long)pyObj).intValue());
                }
            case SHORT:
                if (pyObj instanceof String) {
                    return (T)Short.valueOf((String) pyObj);
                } else {
                    return (T)Short.valueOf(((Long)pyObj).shortValue());
                }
            case BYTE:
                if (pyObj instanceof String) {
                    return (T)Byte.valueOf((String) pyObj);
                } else {
                    return (T)Byte.valueOf(((Long)pyObj).byteValue());
                }
            case DOUBLE:
                if (pyObj instanceof String) {
                    return (T)Double.valueOf((String) pyObj);
                } else {
                    return (T)pyObj;
                }
            case FLOAT:
                if (pyObj instanceof String) {
                    return (T)Float.valueOf((String) pyObj);
                } else {
                    return (T) Float.valueOf(((Double)pyObj).floatValue());
                }
            case BOOLEAN:
                if (pyObj instanceof String) {
                    return (T)Boolean.valueOf((String) pyObj);
                } else {
                    return (T)pyObj;
                }
            case CHAR: return (T) Character.valueOf(((String) pyObj).charAt(0));
            case STRING: return (T)(String)pyObj;
            case ARRAYOFDOUBLES:
                ArrayList<Double> doubles = (ArrayList<Double>)pyObj;
                return (T)(new NumericArray(NumericArray.NumericArrayType.DOUBLE,doubles));
            case ARRAYOFLONGS:
                ArrayList<Long> longs = (ArrayList<Long>)pyObj;
                return (T)(new NumericArray(NumericArray.NumericArrayType.LONG,longs));
            case ALARM:
                if (pyObj instanceof String) {
                    String temp = (String) pyObj;
                    int idx = temp.lastIndexOf('.');
                    logger.debug("Temp = {}, index={}", temp, idx);
                    if (idx >= 0) {
                        temp = temp.substring(idx + 1);
                    }
                    logger.debug("New temp = {}, index={}", temp, idx);
                    return (T) Alarm.valueOf((String) temp);
                } else {
                    return (T)pyObj;
                }
            default:
                throw new UnsupportedOperationException("Unsupported type "+outputType);
        }

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

        outputType = outputInfo.iasioType();

        logger.info("Python TF for {} initialized",compElementRunningId);
    }

    /**
     * Build the statement to import the python class name as described in {@link PythonExecutorTF#pythonFullClassName}
     *
     * @return the python statement to import the python class
     */
    private String buildImportPythonClassNameStatement() {
        String[] parts=null;
        if (pythonFullClassName.contains(".")) {
            parts = pythonFullClassName.split("\\.");
        } else {
            parts = new String[]{pythonFullClassName};
        }
        return String.format("from %s import %s", pythonFullClassName,parts[parts.length-1]);
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
        String importOfPythonClass = buildImportPythonClassNameStatement();
        logger.debug("Load python class with python statement '{}'",importOfPythonClass);
        pythonInterpreter.exec(importOfPythonClass);

        // Build the python object that implements the TF
        pythonInterpreter.set("asceId",this.compElementId);
        pythonInterpreter.set("asceRunningId",this.compElementRunningId);
        pythonInterpreter.set("validityTimeFrame",this.validityTimeFrame);
        pythonInterpreter.exec("userProps = {}");

        if (props.isEmpty()) {
            logger.debug("No properties have been set for ASCE {}",compElementRunningId);
        } else {
            for (String k: props.stringPropertyNames()) {
                pythonInterpreter.exec("userProps['"+k+"']='"+props.get(k)+"'");
                logger.debug("Set property {}={}",k,props.get(k));
            }
        }

        // Get the name of the python class from importOfPythonClass
        // whose format is "from X.Y.Z import C"
        String[] parts = importOfPythonClass.split(" ");
        String pyClassName = parts[parts.length-1];

        // Build the python class
        logger.debug("Building python class {}",pyClassName);
        pythonInterpreter.exec("pyTF = "+pyClassName+"(asceId,asceRunningId,validityTimeFrame,userProps)");
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
