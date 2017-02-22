package org.eso.ias.cdb.rdb;

import java.io.File;

import org.eso.ias.cdb.pojos.AsceDao;
import org.eso.ias.cdb.pojos.DasuDao;
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.pojos.IasTypeDao;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.cdb.pojos.LogLevelDao;
import org.eso.ias.cdb.pojos.PropertyDao;
import org.eso.ias.cdb.pojos.SupervisorDao;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author acaproni
 *
 */
public class IasCdbRdb {
	static final Logger logger = LoggerFactory.getLogger(IasCdbRdb.class);

	private static SessionFactory sessionFactory=null;
    private static ServiceRegistry serviceRegistry;
    
    public static SessionFactory createSessionFactory() {
    	
        try{
        	logger.info("Bootstrapping hibernate");
        	File configFile = new File("hibernate.cfg.xml");
            Configuration configuration = new Configuration();
            configuration.configure(configFile);
            serviceRegistry = new StandardServiceRegistryBuilder().applySettings(
                    configuration.getProperties()).build();
            
            MetadataSources sources = new MetadataSources( serviceRegistry);
            sources.addAnnotatedClass(IasDao.class);
            sources.addAnnotatedClass(PropertyDao.class);
            sources.addAnnotatedClass(IasioDao.class);
            sources.addAnnotatedClass(AsceDao.class);
            sources.addAnnotatedClass(DasuDao.class);
            sources.addAnnotatedClass(SupervisorDao.class);
            Metadata data=sources.buildMetadata();
            sessionFactory=data.buildSessionFactory();
            return sessionFactory;
        }
        catch (Throwable ex) {
            // Make sure you log the exception, as it might be swallowed
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }
    public static SessionFactory getSessionFactory() {
          if (sessionFactory==null) {
        	  sessionFactory=IasCdbRdb.createSessionFactory(); 
          }
    	  return sessionFactory;
    }
    
    private void storeIas() {
    	logger.info("Creating IAS objects to persist");
    	IasDao ias = new IasDao();
    	ias.setLogLevel(LogLevelDao.INFO);
    	
    	PropertyDao p1 = new PropertyDao();
    	p1.setName("Name1");
    	p1.setValue("Value1");
    	PropertyDao p2 = new PropertyDao();
    	p2.setName("Name2");
    	p2.setValue("Value2");
    	
    	IasDao ias2 = new IasDao();
    	ias2.setLogLevel(LogLevelDao.WARN);
    	
    	PropertyDao p3 = new PropertyDao();
    	p3.setName("Name-3");
    	p3.setValue("Value-3");
    	PropertyDao p4 = new PropertyDao();
    	p4.setName("Name-4");
    	p4.setValue("Value-4");
    	
    	ias2.getProps().add(p3);
    	ias2.getProps().add(p4);
    	
    	Session s = getSessionFactory().openSession();

    	logger.info("Persisting a IAS and 2 unrelated properties");
    	Transaction t = s.beginTransaction();
    	s.persist(ias);
    	s.persist(p1);
    	s.persist(p2);
    	t.commit();
    	s.flush();
    	
    	logger.info("Beginning transaction for a IAS with 2 properties");
    	Transaction t2 = s.beginTransaction();
    	s.persist(ias);
    	s.persist(p1);
    	s.persist(p2);
    	s.persist(ias2);
    	t2.commit();
    	s.flush();
    	
		logger.info("Persisting IASIOs");
		IasioDao i1 = new IasioDao();
		i1.setId("CONTROL/DA01/WVR/AMB_TEMP");
		i1.setRefreshRate(1000);
		i1.setShortDesc("The short description of this MP");
		i1.setIasType(IasTypeDao.ALARM);
		
		IasioDao i2 = new IasioDao();
		i2.setId("WTANK3/LEVEL");
		i2.setRefreshRate(1500);
		i2.setShortDesc("Remaining liters in water tank 3");
		i2.setIasType(IasTypeDao.INT);
		
		IasioDao i3 = new IasioDao();
		i3.setId("WTANK2/LEVEL");
		i3.setRefreshRate(1500);
		i3.setIasType(IasTypeDao.INT);
		
		logger.info("Beginning transaction for IASIOs");
    	Transaction t3 = s.beginTransaction();
    	s.persist(i1);
    	s.persist(i2);
    	s.persist(i3);
    	t3.commit();
    	s.flush();
    	
    	
    	
    	PropertyDao asce_p1 = new PropertyDao();
    	asce_p1.setName("ASCE prop1 Name");
    	asce_p1.setValue("ASCE prop1 Value");
    	PropertyDao asce_p2 = new PropertyDao();
    	asce_p2.setName("ASCE prop2 Name");
    	asce_p2.setValue("ASCE prop2 Value");
    	
    	AsceDao asce = new AsceDao();
    	asce.setId("ASCE-ID");
    	asce.setOutput(i2);
    	asce.setTfClass("alma.acs.eso.org.tf.Multiplicity");
    	asce.getProps().add(asce_p1);
    	asce.getProps().add(asce_p2);
    	asce.getInputs().add(i1);
    	asce.getInputs().add(i3);
    	
    	DasuDao dasu = new DasuDao();
    	dasu.setId("TheIdOfTheDASU");
    	dasu.setLogLevel(LogLevelDao.FATAL);
    	dasu.addAsce(asce);
    	
    	SupervisorDao superv = new SupervisorDao();
    	superv.setId("SupervID");
    	superv.setHostName("iasdevel.hq.eso.org");
    	superv.addDasu(dasu);
    	superv.setLogLevel(LogLevelDao.ALL);
    	
    	logger.info("Beginning transaction for Supervisor  and its DASU and ASCE");
    	Transaction t4 = s.beginTransaction();
    	s.persist(superv);
    	t4.commit();
    	s.flush();
    	
    	superv.setLogLevel(LogLevelDao.INFO);
    	Transaction t5 = s.beginTransaction();
    	s.persist(superv);
    	t5.commit();
    	s.flush();
    	
    	s.close();
    	
    }

	public static void main(String[] args) {
		System.out.println("IasCdbRdb started.. ");
		
		for (String arg: args) {
			System.out.println("==> "+arg);
		}
		
		IasCdbRdb ias = new IasCdbRdb();
		ias.storeIas();
		
		System.out.println("Sleeping...");
		try {
			Thread.sleep(5000);
		} catch (Exception e) {}
		getSessionFactory().close();
		
		
		System.out.println("IasCdbRdb done.\n");

	}

}
