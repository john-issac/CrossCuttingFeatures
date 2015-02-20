/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.servicewrapper;

import com.logging.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author john
 */
public class ServiceRunner {
    /**
     * Holds all passed arguments external caller. The first must be a class to instantiate as a service.
     * Subsequent parameters could be another class or an argument to be passed to the previous class in which case
     * it has to have the character "-". Ex: Class1 Class2 -arg1. This will start 2 services, one for Class1 
     * (which takes no arguments) and one for Class2 (which takes arg1 as a parameter)
     */
    private Map<String, List> _allParams;
    /**
     * Holds the instantiated services from the classes in allParams
     */
    private Map<Integer, String> _services;
    
    private static List<IService> _servicesInstants;
    
    private static ILogger logger = LogProvider.getLogger();

    public ServiceRunner(Map<String, List> allParams) {
        _allParams = allParams;
    }
    
    public ServiceRunner(String[] servicesClasses) {
        if (servicesClasses.length < 1)
            logger.error("Missing arguments.", new RuntimeException());
        int servicesNum = -1;
        for(int i=0; i<servicesClasses.length; i++) {
            char firstChar = servicesClasses[i].charAt(0);
            // ensure the first argument doesn't start with a -
            if(firstChar == '-' && _services.isEmpty()) {
                logger.error("Missing service class name as first argument.", new RuntimeException());
            } else if(firstChar == '-') {
                if(servicesClasses[i].indexOf("=") > -1 && !Character.isLetter(servicesClasses[i].charAt(1)))
                    logger.error("Invalid Service Parameter.", new RuntimeException());
                if(_allParams.containsKey(_services.get(servicesNum)))
                    _allParams.get(_services.get(servicesNum)).add(servicesClasses[i]);
                else {
                    _allParams.put(_services.get(servicesNum), new ArrayList());
                    _allParams.get(_services.get(servicesNum)).add(servicesClasses[i]);
                }
            } else {
                _services.put(++servicesNum, servicesClasses[i]);
            }
        }
    }
    
    public void startAll() {
        String serviceClassName = null;
        try {
            for(int i=0; i<_services.size(); i++) {
                serviceClassName = _services.get(i);
                logger.debug("Creating " + serviceClassName);
                Class<?> serviceClass = Class.forName(serviceClassName);
                List params = _allParams.get(serviceClassName);
                Constructor serviceClassCnstr;
                if(params == null) // parameterless constructor
                    serviceClassCnstr = serviceClass.getDeclaredConstructor();
                else
                    serviceClassCnstr = serviceClass.getDeclaredConstructor(List.class);
                serviceClassCnstr.setAccessible(true);
                if (!IService.class.isAssignableFrom(serviceClass)) {
                    logger.error("Service class " + serviceClassName + " did not implement " +
                            IService.class.getName(),new RuntimeException());
                }
                Object serviceObject;
                
                if(params == null)
                    serviceObject = serviceClassCnstr.newInstance();
                else
                    serviceObject = serviceClassCnstr.newInstance(params);
                IService service = (IService)serviceObject;
                
                _servicesInstants.add(service);

                registerShutdownHook(service);

                logger.debug("Starting service " + service);
                service.init();
                service.start();
                logger.info(service + " started.");
            }

        } catch (ClassNotFoundException | RuntimeException | InstantiationException | 
                IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            logger.error("Failed to create and run ", e);
        }
        
    }
    
    private void registerShutdownHook(final IService service) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.debug("Stopping service " + service);
                service.stop();
                service.destroy();
                logger.info(service + " stopped.");
            }
        });
    }
    
    public static void stop() {
        try {
            for(int i=0; i<_servicesInstants.size(); i++) {
                _servicesInstants.get(i).stop();
                _servicesInstants.get(i).destroy();
            }
            System.exit(0);
        } catch(Exception e) {
            logger.error("Failed to stop naturally", e);
            System.exit(1);
        }
    }
}
