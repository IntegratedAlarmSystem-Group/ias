package org.eso.ias.cdb.test.extreader;

import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.IasCdbException;
import org.eso.ias.cdb.pojos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * A CdbReader to test the functioning of an external reader
 * passed by the user in the folder pointed by the IAS_EXTERNAL_JARS
 *
 * This is a Mock class whose methods are empty apart of {@link #getIas()}
 */
public class ExtCdbReader implements CdbReader {

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(ExtCdbReader.class);

    public ExtCdbReader() {
        logger.info("External CDB reader built");
    }

    /**
     * Get the Ias configuration from a CDB.
     *
     * @return The ias configuration read from the CDB
     * @throws IasCdbException In case of error getting the IAS
     */
    @Override
    public Optional<IasDao> getIas() throws IasCdbException {
        IasDao iDao = new IasDao();
        iDao.setBsdbUrl("net.ext.cdb.reader.url");
        iDao.setHbFrequency(5);
        iDao.setRefreshRate(3);
        return Optional.of(iDao);
    }

    /**
     * Get the IASIOs.
     *
     * @return The IASIOs read from the CDB
     * @throws IasCdbException In case of error getting the IASIOs
     */
    @Override
    public Optional<Set<IasioDao>> getIasios() throws IasCdbException {
        return Optional.empty();
    }

    /**
     * Get the IASIO with the given ID
     *
     * @param id The ID of the IASIO to read the configuration
     * @return The IASIO read from the CDB
     * @throws IasCdbException In case of error getting the IASIO
     */
    @Override
    public Optional<IasioDao> getIasio(String id) throws IasCdbException {
        return Optional.empty();
    }

    /**
     * Read the supervisor configuration from the CDB.
     *
     * @param id The not null nor empty supervisor identifier
     * @return The Supervisor read from the CDB
     * @throws IasCdbException In case of error getting the Supervisor
     */
    @Override
    public Optional<SupervisorDao> getSupervisor(String id) throws IasCdbException {
        return Optional.empty();
    }

    /**
     * Read the transfer function configuration from the CDB.
     *
     * @param tf_id The not <code>null</code> nor empty transfer function identifier
     * @return The transfer function read from the CDB
     * @throws IasCdbException in case of error reading from the CDB
     */
    @Override
    public Optional<TransferFunctionDao> getTransferFunction(String tf_id) throws IasCdbException {
        return Optional.empty();
    }

    /**
     * Get the transfer functions.
     *
     * @return The transfer functions read from the CDB
     * @throws IasCdbException In case of error getting the transfer functions
     */
    @Override
    public Optional<Set<TransferFunctionDao>> getTransferFunctions() throws IasCdbException {
        return Optional.empty();
    }

    /**
     * Read the ttemplate configuration from the CDB.
     *
     * @param template_id The not <code>null</code> nor empty identifier of the template
     * @return The template read from the CDB
     * @throws IasCdbException in case of error reading from the CDB
     */
    @Override
    public Optional<TemplateDao> getTemplate(String template_id) throws IasCdbException {
        return Optional.empty();
    }

    /**
     * Get the templates.
     *
     * @return The templates read from the CDB
     * @throws IasCdbException In case of error getting the templates
     */
    @Override
    public Optional<Set<TemplateDao>> getTemplates() throws IasCdbException {
        return Optional.empty();
    }

    /**
     * Read the ASCE configuration from the CDB.
     *
     * @param id The not null nor empty ASCE identifier
     * @return The ASCE read from the file
     * @throws IasCdbException In case of error getting the ASCE
     */
    @Override
    public Optional<AsceDao> getAsce(String id) throws IasCdbException {
        return Optional.empty();
    }

    /**
     * Read the DASU configuration from the CDB.
     *
     * @param id The not null nor empty DASU identifier
     * @return The DASU read from the file
     * @throws IasCdbException In case of error getting the DASU
     */
    @Override
    public Optional<DasuDao> getDasu(String id) throws IasCdbException {
        return Optional.empty();
    }

    /**
     * Return the DASUs to deploy in the Supervisor with the given identifier
     *
     * @param id The not <code>null</code> nor empty identifier of the supervisor
     * @return A set of DASUs to deploy in the supervisor with the passed id
     * @throws IasCdbException in case of error reading CDB or if the
     *                         supervisor with the give identifier does not exist
     */
    @Override
    public Set<DasuToDeployDao> getDasusToDeployInSupervisor(String id) throws IasCdbException {
        return null;
    }

    /**
     * Return the ASCEs belonging to the given DASU.
     *
     * @param id The not <code>null</code> nor empty identifier of the DASU
     * @return A set of ASCEs running in the DASU with the passed id
     * @throws IasCdbException in case of error reading CDB or if the
     *                         DASU with the give identifier does not exist
     */
    @Override
    public Set<AsceDao> getAscesForDasu(String id) throws IasCdbException {
        return null;
    }

    /**
     * Return the IASIOs in input to the given ASCE.
     *
     * @param id The not <code>null</code> nor empty identifier of the ASCE
     * @return A set of IASIOs in input to the ASCE
     * @throws IasCdbException in case of error reading CDB or if the
     *                         ASCE with the give identifier does not exist
     */
    @Override
    public Collection<IasioDao> getIasiosForAsce(String id) throws IasCdbException {
        return null;
    }

    /**
     * Get the IDs of the Supervisors.
     * <p>
     * This method is useful to deploy the supervisors
     *
     * @return The the IDs of the supervisors read from the CDB
     * @throws IasCdbException In case of error getting the IDs of the supervisors
     */
    @Override
    public Optional<Set<String>> getSupervisorIds() throws IasCdbException {
        return Optional.empty();
    }

    /**
     * Get the IDs of the DASUs.
     *
     * @return The IDs of the DASUs read from the CDB
     * @throws IasCdbException In case of error getting the IDs of the DASUs
     */
    @Override
    public Optional<Set<String>> getDasuIds() throws IasCdbException {
        return Optional.empty();
    }

    /**
     * Get the IDs of the ASCEs.
     *
     * @return The IDs of the ASCEs read from the CDB
     * @throws IasCdbException In case of error getting the IDs of the ASCEs
     */
    @Override
    public Optional<Set<String>> getAsceIds() throws IasCdbException {
        return Optional.empty();
    }

    /**
     * Return the templated IASIOs in input to the given ASCE.
     * <p>
     * These inputs are the one generated by a different template than
     * that of the ASCE
     * (@see <A href="https://github.com/IntegratedAlarmSystem-Group/ias/issues/124">#124</A>)
     *
     * @param id The not <code>null</code> nor empty identifier of the ASCE
     * @return A set of template instance of IASIOs in input to the ASCE
     * @throws IasCdbException in case of error reading CDB or if the
     *                         ASCE with the give identifier does not exist
     */
    @Override
    public Collection<TemplateInstanceIasioDao> getTemplateInstancesIasiosForAsce(String id) throws IasCdbException {
        return null;
    }

    /**
     * Get the configuraton of the client with the passed identifier.
     * <p>
     * The configuration is passed as a string whose format depends
     * on the client implementation.
     *
     * @param id The not null nor empty ID of the IAS client
     * @return The configuration of the client
     * @throws IasCdbException In case of error getting the configuration of the client
     */
    @Override
    public Optional<ClientConfigDao> getClientConfig(String id) throws IasCdbException {
        return Optional.empty();
    }

    /**
     * Initialize the CDB
     */
    @Override
    public void init() throws IasCdbException {
        logger.info("Initialized");

    }

    /**
     * Close the CDB and release the associated resources
     *
     * @throws IasCdbException
     */
    @Override
    public void shutdown() throws IasCdbException {
        logger.info("Closed");
    }
}
