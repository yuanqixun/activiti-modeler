package com.signavio.platform.listeners;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.activiti.engine.ProcessEngines;
import org.apache.log4j.Logger;

/**
 * 流程引擎上下文监听
 * @author yuanqixun
 * @since 2011-08-16
 */
public class ProcessEnginesServletContextListener implements
		ServletContextListener {
	private final Logger log = Logger.getLogger(ProcessEnginesServletContextListener.class);
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		log.info("Destroying ProcessEngines...");
		ProcessEngines.destroy();
		log.info("Done destroying ProcessEngines...");
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		log.info("Initializing ProcessEngines...");
		ProcessEngines.init();
		log.info("Done initializing ProcessEngines...");
	}

}
