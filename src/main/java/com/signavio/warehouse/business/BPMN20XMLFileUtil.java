package com.signavio.warehouse.business;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.json.JSONException;
import org.xml.sax.SAXException;

import com.signavio.platform.core.Platform;
import com.signavio.platform.core.PlatformProperties;
import com.signavio.platform.util.fsbackend.FileSystemUtil;

import de.hpi.bpmn2_0.exceptions.BpmnConverterException;
import de.hpi.bpmn2_0.transformation.Json2XmlConverter;

public class BPMN20XMLFileUtil {

	public static String storeBPMN20XMLFile(String path, String jsonRep) throws IOException, JSONException, BpmnConverterException, JAXBException, SAXException, ParserConfigurationException, TransformerException {
		Json2XmlConverter converter = new Json2XmlConverter(jsonRep, Platform.getInstance().getFile("/WEB-INF/xsd/BPMN20.xsd").getAbsolutePath());
		StringWriter xml = converter.getXml();
		FileSystemUtil.deleteFileOrDirectory(path);
		FileSystemUtil.createFile(path, xml.toString());
		return path;
	}
	
	public static String storeBPMN20XMLFile(String jsonRep) throws IOException, JSONException, BpmnConverterException, JAXBException, SAXException, ParserConfigurationException, TransformerException {
		PlatformProperties props = Platform.getInstance().getPlatformProperties();
		Json2XmlConverter converter = new Json2XmlConverter(jsonRep, Platform.getInstance().getFile("/WEB-INF/xsd/BPMN20.xsd").getAbsolutePath());
		StringWriter xml = converter.getXml();
		String path="";
		try {
			Document document = DocumentHelper.parseText(xml.toString());
			Element root = document.getRootElement();
			String id=root.attributeValue("id");
			id=id.substring(4);
			//创建目录
			String dir = props.getRootDirectoryPath();
			dir = dir+File.separator+id;
			FileSystemUtil.createDirectory(dir);
			path=dir+File.separator+"draft.bpmn20.xml";
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		FileSystemUtil.createFile(path, xml.toString());
		return path;
	}
}
