package com.intalio.web.plugin.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.log4j.Logger;

import com.intalio.web.plugin.IDiagramPlugin;
import com.intalio.web.plugin.IDiagramPluginService;

public class PluginServiceImpl implements IDiagramPluginService {

	private static Logger _logger = Logger.getLogger(PluginServiceImpl.class);

	private static PluginServiceImpl _instance = null;

	private Map<String, IDiagramPlugin> _registry = new HashMap<String, IDiagramPlugin>();

	/**
	 * Private constructor to make sure we respect the singleton pattern.
	 * 
	 * @param context
	 *            the servlet context
	 */
	private PluginServiceImpl(ServletContext context) {
		_registry.putAll(getLocalPluginsRegistry(context));
	}

	/**
	 * @param context
	 *            the context needed for initialization
	 * @return the singleton of PluginServiceImpl
	 */
	public static IDiagramPluginService getInstance(ServletContext context) {
		if (_instance == null) {
			_instance = new PluginServiceImpl(context);
		}
		return _instance;
	}

	/**
	 * The default local plugins, available to the webapp so that the default
	 * profile can provision its plugins. Consumers through OSGi should use the
	 * service tracker to get the plugins they need.
	 */
	private static Map<String, IDiagramPlugin> LOCAL = null;

	/**
	 * Initialize the local plugins registry
	 * 
	 * @param context
	 *            the servlet context necessary to grab the files inside the
	 *            servlet.
	 * @return the set of local plugins organized by name
	 */
	public static Map<String, IDiagramPlugin> getLocalPluginsRegistry(
			ServletContext context) {
		if (LOCAL == null) {
			LOCAL = initializeLocalPlugins(context);
		}
		return LOCAL;
	}

	private Map<String, IDiagramPlugin> assemblePlugins(
			ServletContext ctx) {
		Map<String, IDiagramPlugin> plugins = new HashMap<String, IDiagramPlugin>(
				_registry);
//		for (IDiagramPluginFactory factory : _factories) {
//			for (IDiagramPlugin p : factory.getPlugins(request)) {
//				plugins.put(p.getName(), p);
//			}
//		}
		return plugins;
	}

	public Collection<IDiagramPlugin> getRegisteredPlugins(
			ServletContext ctx) {
		return assemblePlugins(ctx).values();
	}

	public IDiagramPlugin findPlugin(ServletContext ctx, String name) {
		return assemblePlugins(ctx).get(name);
	}

	private static Map<String, IDiagramPlugin> initializeLocalPlugins(
			ServletContext context) {
		Map<String, IDiagramPlugin> local = new HashMap<String, IDiagramPlugin>();
		// we read the plugins.xml file and make sense of it.
		InputStream fileStream = null;
		try {
//			try {
//				fileStream = new FileInputStream(new StringBuilder(
//						context.getRealPath("/")).append("/editor").append("/scripts").append("/Plugins").append("/")
//						.append("plugins.xml").toString());
				fileStream = context.getResourceAsStream("/WEB-INF/xml/editor/plugins.xml");
//			} catch (FileNotFoundException e) {
//				throw new RuntimeException(e);
//			}
			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLStreamReader reader = factory.createXMLStreamReader(fileStream);
			while (reader.hasNext()) {
				if (reader.next() == XMLStreamReader.START_ELEMENT) {
					if ("plugin".equals(reader.getLocalName())) {
						String source = null, name = null;
						boolean core = false;
						for (int i = 0; i < reader.getAttributeCount(); i++) {
							if ("source"
									.equals(reader.getAttributeLocalName(i))) {
								source = reader.getAttributeValue(i);
							} else if ("name".equals(reader
									.getAttributeLocalName(i))) {
								name = reader.getAttributeValue(i);
							} else if ("core".equals(reader
									.getAttributeLocalName(i))) {
								core = Boolean.parseBoolean(reader
										.getAttributeValue(i));
							}
						}
						Map<String, Object> props = new HashMap<String, Object>();
						while (reader.hasNext()) {
							int ev = reader.next();
							if (ev == XMLStreamReader.START_ELEMENT) {
								if ("property".equals(reader.getLocalName())) {
									String key = null, value = null;
									for (int i = 0; i < reader
											.getAttributeCount(); i++) {
										if ("name".equals(reader
												.getAttributeLocalName(i))) {
											key = reader.getAttributeValue(i);
										} else if ("value".equals(reader
												.getAttributeLocalName(i))) {
											value = reader.getAttributeValue(i);
										}
									}
									if (key != null & value != null)
										props.put(key, value);
								}
							} else if (ev == XMLStreamReader.END_ELEMENT) {
								if ("plugin".equals(reader.getLocalName())) {
									break;
								}
							}
						}
						local.put(name, new LocalPluginImpl(name, source,
								context, core, props));
					}
				}
			}
		} catch (XMLStreamException e) {
			_logger.error(e.getMessage(), e);
			throw new RuntimeException(e); // stop initialization
		} finally {
			if (fileStream != null) {
				try {
					fileStream.close();
				} catch (IOException e) {
				}
			}
			;
		}
		return local;
	}

}
