package org.eso.ias.cdb;

import org.eso.ias.cdb.json.CdbFiles;
import org.eso.ias.cdb.json.CdbJsonFiles;
import org.eso.ias.cdb.json.JsonReader;
import org.eso.ias.cdb.rdb.RdbReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The factory to get the CdbReader implementation to use.
 *
 * This class checks the parameters of the command line, the java properties
 * and the environment variable to build and return the {@link CdbReader} to use
 * for reading the CDB.
 *
 * It offers a common strategy to be used consistently by all the IAS tools.
 *
 * The strategy is as follow:
 *  - if -cdbClass java.class param is present in the command line then the passed class
 *                 is dynamically loaded and built (empty constructor); the configuration
 *                 parameters eventually expected by such class will be passed by means of java
 *                 properties (-D...)
 *  - else if jCdb file.path param is present in the command line then the JSON CDB
 *                      implementation will be returned passing the passed file path
 *  - else the RDB implementation is returned (note that the parameters to connect to the RDB are passed
 *                 in the hibernate configuration file
 *
 * RDB implementation is the fallback if none of the other possibilities succeeds.
 *
 */
public class CdbReaderFactory {

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(CdbReaderFactory.class);

    /**
     * The parameter to set in the command line to build a CdbReader from a custom java class
     */
    public static final String cdbClassCmdLineParam="-cdbClass";

    /**
     * The long parameter to set in the command line to build a JSON CdbReader
     */
    public static final String jsonCdbCmdLineParamLong ="-jCdb";

    /**
     * The short parameter to set in the command line to build a JSON CdbReader
     */
    public static final String jsonCdbCmdLineParamShort ="-j";

    /**
     * get the value of the passed parameter from the eray of strings.
     *
     * The value of the parameter is in the position next to the passed param name like in
     * -jcdb path
     *
     * @param paramName the not null nor empty parameter
     * @param cmdLine the command line
     * @return the value of the parameter or empty if not found in the command line
     */
    private static Optional<String> getValueOfParam(String paramName, String cmdLine[]) {
        if (paramName==null || paramName.isEmpty()) {
            throw new IllegalArgumentException("Invalid null/empty name of parameter");
        }
        if (cmdLine==null || cmdLine.length<2) {
            return Optional.empty();
        }
        List<String> params = Arrays.asList(cmdLine);
        int pos=params.indexOf(paramName);
        if (pos==-1) {
            // Not found
            return Optional.empty();
        }
        String ret = null;
        try {
            ret=params.get(pos+1);
        } catch (IndexOutOfBoundsException e) {
            logger.error("Missing parameter for {}}",paramName);
        }
        return Optional.ofNullable(ret);
    }

    /**
     * Build and return the user provided CdbReader from the passed class using introspection
     *
     * @param cls The class implementing the CdbReader
     * @return the user defined CdbReader
     */
    private static final CdbReader loadUserDefinedReader(String cls) throws IasCdbException {
        if (cls==null || cls.isEmpty()) {
            throw new IllegalArgumentException("Invalid null/empty class");
        }
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Class<?> theClass=null;
        try {
            theClass=classLoader.loadClass(cls);
        } catch (Exception e) {
            throw new IasCdbException("Error loading the external class "+cls,e);
        }
        Constructor constructor = null;
        try {
            constructor=theClass.getConstructor(null);
        } catch (Exception e) {
            throw new IasCdbException("Error getting the default (empty) constructor of the external class "+cls,e);
        }
        Object obj=null;
        try {
            obj=constructor.newInstance(null);
        } catch (Exception e) {
            throw new IasCdbException("Error building an object of the external class "+cls,e);
        }
        return (CdbReader)obj;
    }

    /**
     * Gets and return the CdbReader to use to read the CDB applying the policiy described in the javadoc
     * of the class
     *
     * @param cmdLine The command line
     * @return the CdbReader to read th CDB
     * @throws Exception in case of error building the CdbReader
     */
    public static CdbReader getCdbReader(String[] cmdLine) throws Exception {
        Objects.requireNonNull(cmdLine,"Invalid null command line");
        Optional<String> userCdbReader = getValueOfParam(cdbClassCmdLineParam,cmdLine);
        if (userCdbReader.isPresent()) {
            logger.info("Using external (user provided) CdbReader from class {}",userCdbReader.get());
            return loadUserDefinedReader(userCdbReader.get());
        } else {
            logger.debug("No external CdbReader found");
        }

        Optional<String> jsonCdbReaderS = getValueOfParam(jsonCdbCmdLineParamShort,cmdLine);
        Optional<String> jsonCdbReaderL = getValueOfParam(jsonCdbCmdLineParamLong,cmdLine);
        if (jsonCdbReaderS.isPresent() && jsonCdbReaderL.isPresent()) {
            throw new Exception("JSON CDB path defined twice: check "+jsonCdbCmdLineParamShort+"and "+jsonCdbCmdLineParamLong+" params in cmd line");
        }
        if (jsonCdbReaderL.isPresent() || jsonCdbReaderS.isPresent()) {
            String cdbPath = jsonCdbReaderL.orElseGet(() -> jsonCdbReaderS.get());
            logger.info("Loading JSON CdbReader with folder {}",cdbPath);
            CdbFiles cdbfiles = new CdbJsonFiles(cdbPath);
            return new JsonReader(cdbfiles);
        } else {
            logger.debug("NO JSON CdbReader requested");
        }
        return new RdbReader();
    }

}
