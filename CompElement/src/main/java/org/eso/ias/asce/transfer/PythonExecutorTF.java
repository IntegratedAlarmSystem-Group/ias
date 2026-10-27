package org.eso.ias.asce.transfer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Stub implementation of PythonTransferExecutor.
 * Python TF support via JEP has been removed. This class will be replaced by
 * GrpcPythonExecutorTF that connects to an external Python gRPC server.
 *
 * Until the gRPC implementation is in place, any attempt to run a Python TF
 * will result in an UnsupportedOperationException.
 */
public class PythonExecutorTF<T> extends PythonTransferExecutor<T> {

    private static final Logger logger = LoggerFactory.getLogger(PythonExecutorTF.class);

    private final String pythonFullClassName;

    /**
     * Constructor. Keeps the same 5-arg signature so that TransferFunctionSetting
     * continues to compile.
     *
     * @param cEleId            The id of the ASCE
     * @param cEleRunningId     the running ID of the ASCE
     * @param validityTimeFrame The time frame (msec) to invalidate monitor points
     * @param props             The properties for the executor
     * @param pythonClassName   The name of the python class to which to delegate
     */
    public PythonExecutorTF(
            String cEleId,
            String cEleRunningId,
            long validityTimeFrame,
            Properties props,
            String pythonClassName) throws Exception {
        super(cEleId, cEleRunningId, validityTimeFrame, props);

        if (pythonClassName == null || pythonClassName.isEmpty()) {
            throw new Exception("Missing python class name");
        }
        this.pythonFullClassName = pythonClassName;

        System.out.println("[PythonExecutorTF] WARNING: Python TF execution is not available (JEP removed). "
                + "ASCE: " + cEleId + ", Python class: " + pythonClassName
                + ". gRPC replacement not yet implemented.");
        logger.warn("Python TF execution is not available (JEP removed). ASCE: {}, Python class: {}. "
                + "gRPC replacement not yet implemented.", cEleId, pythonClassName);
    }

    @Override
    public IasIOJ<T> eval(Map<String, IasIOJ<?>> compInputs, IasIOJ<T> actualOutput) throws Exception {
        throw new UnsupportedOperationException("Python TF execution is not available (JEP removed). "
                + "This will be replaced by GrpcPythonExecutorTF.");
    }

    @Override
    public void initialize(Set<IasioInfo> inputsInfo, IasioInfo outputInfo) throws Exception {
        throw new UnsupportedOperationException("Python TF execution is not available (JEP removed). "
                + "This will be replaced by GrpcPythonExecutorTF.");
    }

    @Override
    public void shutdown() throws Exception {
        logger.info("Python executor stub shut down for {}", compElementRunningId);
    }
}
