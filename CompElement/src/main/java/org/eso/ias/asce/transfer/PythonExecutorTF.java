package org.eso.ias.asce.transfer;

import jep.Interpreter;
import jep.SharedInterpreter;
import jep.python.PyObject;
import org.eso.ias.asce.CompEleThreadFactory;
import org.eso.ias.types.Alarm;
import org.eso.ias.types.IASTypes;
import org.eso.ias.types.NumericArray;
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
                return PythonExecutorTF.this.evalPythonTask(compInputs,actualOutput);
            }
        };

        Future<IasIOJ<T>> future = executor.submit(callable);

        IasIOJ<T> newOutput;
        try {
            newOutput=future.get(1,TimeUnit.MINUTES);
        } catch (Exception e) {
            e.printStackTrace();;
            throw new Exception(
                    "Exception caught while waiting for the execution of the python TF task of ASCE "+compElementRunningId,
                    e);
        }
        return newOutput;
    }

    /**
     * Convert the java value of the given type to the right python value
     * and assign it to variable name in the python intepreter
     *
     * @param value the value to assign to the variable
     * @param pythonVarName the name of the variable to set in the interpreter
     */
    private void convertJavaIasValueToPython(IasIOJ<?> value, String pythonVarName) {
        Objects.requireNonNull(value, "Invalid null value");

        switch (value.getType()) {
            case LONG:
            case INT:
            case SHORT:
            case BYTE:
            case TIMESTAMP:
                pythonInterpreter.exec(pythonVarName+"=int("+value.getValue().get()+")");
                break;
            case DOUBLE:
            case FLOAT:
                pythonInterpreter.exec(pythonVarName+"=float("+value.getValue().get()+")");
                break;
            case BOOLEAN:
                if (value.getValue().get()==Boolean.TRUE) {
                    pythonInterpreter.exec(pythonVarName+"=True");
                } else {
                    pythonInterpreter.exec(pythonVarName+"=False");
                }
                break;
            case CHAR:
                pythonInterpreter.exec(pythonVarName+"='"+((Character)(value.getValue().get())).charValue()+"'");
                break;
            case STRING:
                pythonInterpreter.exec(pythonVarName+"='"+value.getValue().get().toString()+"'");
                break;
            case ARRAYOFDOUBLES:
                NumericArray nad = (NumericArray)value.getValue().get();
                Double[] doubles=nad.toArrayOfDouble();
                pythonInterpreter.exec(pythonVarName+"=[]");
                for (Double num: doubles) {
                    pythonInterpreter.exec(pythonVarName+".append(float("+num+"))");
                }
                break;
            case ARRAYOFLONGS:
                NumericArray nal = (NumericArray)value.getValue().get();
                Double[] longs=nal.toArrayOfDouble();
                pythonInterpreter.exec(pythonVarName+"=[]");
                for (Double num: longs) {
                    pythonInterpreter.exec(pythonVarName+".append(int("+num+"))");
                }
                break;
            case ALARM:
                Alarm alarm = (Alarm)value.getValue().get();
                pythonInterpreter.exec("from IasBasicTypes.Alarm import Alarm");
                pythonInterpreter.exec("from IasBasicTypes.AlarmState import AlarmState");
                pythonInterpreter.exec("from IasBasicTypes.Priority import Priority");
                pythonInterpreter.exec(pythonVarName+"=Alarm(AlarmState."+alarm.alarmState.name()+", Priority."+alarm.priority.name()+")");
                break;
            default:
                throw new UnsupportedOperationException("Unsupported input type "+value.getType());
        }

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
            logger.debug("Converting input param {} of type {}", compInputs.get(key).getId(), compInputs.get(key).getType());

            pythonInterpreter.set("id", compInputs.get(key).getId());
            pythonInterpreter.set("runningId", compInputs.get(key).getFullrunningId());
            pythonInterpreter.set("mode", compInputs.get(key).getMode());
            pythonInterpreter.set("iasType", compInputs.get(key).getType());
            pythonInterpreter.set("validity", compInputs.get(key).getValidity());
            assert compInputs.get(key).getValue().isPresent() : "Input value shall not be empty";

            convertJavaIasValueToPython(compInputs.get(key), "iasValue");

            logger.debug("Input params converted");
            assert compInputs.get(key).productionTStamp().isPresent() : "Input production timestamp shall not be empty";
            pythonInterpreter.set("prodTStamp", compInputs.get(key).productionTStamp().get());

            pythonInterpreter.exec("props = {}");
            Map<String, String> props = compInputs.get(key).getProps();
            if (props != null && !props.isEmpty()) {
                for (String k : props.keySet()) {
                    pythonInterpreter.exec("props['"+k+"']='"+props.get(k)+"'");
                }
            }
            logger.debug("Properties converted to python");

            pythonInterpreter.exec("input = IASIO(id,runningId,mode,iasType,validity,iasValue,prodTStamp,props)");
            pythonInterpreter.exec("inputs[id]=input");
        }

        logger.debug("Converting output {} of type {}",actualOutput.getId(), actualOutput.getType().name());
        pythonInterpreter.set("id", actualOutput.getId());
        pythonInterpreter.set("runningId", actualOutput.getFullrunningId());
        pythonInterpreter.set("mode", actualOutput.getMode());
        pythonInterpreter.set("iasType", actualOutput.getType());
        pythonInterpreter.set("validity", actualOutput.getValidity());
        if (actualOutput.getValue().isPresent()) {
            convertJavaIasValueToPython(actualOutput, "outputValue");
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

        pythonInterpreter.exec("actualOutput = IASIO(id,runningId,mode,iasType,validity,outputValue,prodTStamp,props)");
        logger.debug("Invoking eval on the python TF for ASCE {}",compElementRunningId);
        pythonInterpreter.exec("out = pyTF.eval(inputs,actualOutput)");

        logger.debug("Decoding the output generated by the python TF of ASCE {}",compElementRunningId);

        String newModeStr = (String)pythonInterpreter.getValue("out.mode.name");
        logger.debug("New mode will be decoded from string {}", newModeStr);
        int dotIidx = newModeStr.lastIndexOf('.');
        OperationalMode newMode;
        if (dotIidx!=-1) {
            newMode = OperationalMode.valueOf(newModeStr.substring(dotIidx+1));
        } else {
            newMode = OperationalMode.valueOf(newModeStr);
        }
        logger.debug("New operatopnal mode {}",newMode);

        // Save the properties in a java HashMap
        HashMap<String,String> newProps = new HashMap<>();
        pythonInterpreter.set("propsMap",newProps);
        pythonInterpreter.exec("for k,v in out.props.items():\n  propsMap.put(str(k),str(v))");
        logger.debug("Got {} properties", newProps.size());

        logger.debug("Getting the value calculated by the python TF...");
        Object str = pythonInterpreter.getValue("out");
        logger.debug("Got the output produced by the python TF");

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
                if (pyObj instanceof PyObject) {
                    String alarmStr = ((PyObject)pyObj).getAttr("string_repr").toString();
                    return (T)Alarm.valueOf(alarmStr);
                } else if (pyObj instanceof String) {
                    return (T) Alarm.valueOf((String)pyObj);
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
                PythonExecutorTF.this.initializePythonTask(inputsInfo, outputInfo);
                return null;
            }
        };


        logger.debug("Submitting initialization task of python TF for ASCE {}",compElementRunningId);
        Future<?> future = executor.submit(callable);


        try {
            future.get(1,TimeUnit.MINUTES);
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

        if (!props.isEmpty()) {
            for (String k: props.stringPropertyNames()) {
                pythonInterpreter.exec("userProps['"+k+"']='"+props.get(k)+"'");
            }
        }
        logger.debug("Properties processed");

        // Get the name of the python class from importOfPythonClass
        // whose format is "from X.Y.Z import C"
        String[] parts = importOfPythonClass.split(" ");
        String pyClassName = parts[parts.length-1];

        // Build the python class
        logger.debug("Building python class {}",pyClassName);
        String instanceStr = "None";
        if (getTemplateInstance().isPresent()) {
            instanceStr = getTemplateInstance().get().toString();
        }
        pythonInterpreter.exec("pyTF = "+pyClassName+"(asceId,asceRunningId,validityTimeFrame,userProps,"+instanceStr+")");
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
            Object typeName=null;
            pythonInterpreter.set("typeName", inputInfo.iasioType().toString());
            pythonInterpreter.set("typeId", inputInfo.iasioId());
            pythonInterpreter.exec("iasioType = IASType.fromString(typeName)");
            pythonInterpreter.exec("ci = IasioInfo(typeId,iasioType)");
            pythonInterpreter.exec("inputs.append(ci)");
        }
        logger.debug("Building python output info for ASCE {}",compElementRunningId);
        Object typeName=null;
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
