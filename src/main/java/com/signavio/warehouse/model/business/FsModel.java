/**
 * Copyright (c) 2009, Signavio GmbH
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.signavio.warehouse.model.business;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.oryxeditor.server.diagram.Diagram;
import org.oryxeditor.server.diagram.DiagramBuilder;

import com.signavio.platform.security.business.FsSecureBusinessObject;
import com.signavio.platform.util.fsbackend.FileSystemUtil;
import com.signavio.warehouse.business.FsEntityManager;
import com.signavio.warehouse.directory.business.FsDirectory;
import com.signavio.warehouse.directory.business.FsRootDirectory;
import com.signavio.warehouse.revision.business.FsModelRepresentationInfo;
import com.signavio.warehouse.revision.business.FsModelRevision;
import com.signavio.warehouse.revision.business.RepresentationType;

/**
 * Implementation of a model in the file accessing Oryx backend.
 * 
 * @author Stefan Krumnow
 *
 */
public class FsModel extends FsSecureBusinessObject {
	
	private String pathPrefix;
	private String name;
	private String fileExtension;
	private String fileFullName;
	
	/**
	 * Constructor
	 * 
	 * Constructs a new model object for an EXISTING filesystem model.
	 *  
	 * @param parentDirectory
	 * @param name
	 * @param uuid
	 */
	public FsModel(String pathPrefix, String name, String fileExtension){
		
		this.pathPrefix = pathPrefix;
		this.name = name;
		this.fileExtension = fileExtension;
		
		String path = getPath();
		fileFullName = path;
		if (FileSystemUtil.isFileDirectory(path)){
			throw new IllegalStateException("Path does not point to a file.");
		} else if ( ! FileSystemUtil.isFileExistent(path) || ! FileSystemUtil.isFileAccessible(path )){
			throw new IllegalStateException("Model can not be accessed");
		}
		
	}
	
	public FsModel(File modelFile){
		this(modelFile.getPath());
	}
	
	public FsModel(String fullName){
		
		if (FileSystemUtil.isFileDirectory(fullName)){
			throw new IllegalStateException("Path does not point to a file.");
		} else if (fullName.contains(File.separator)){
			int i = fullName.lastIndexOf(File.separator);
			this.pathPrefix = fullName.substring(0, i);
			String remainder = fullName.substring(i+1);
			String[] splittedName = ModelTypeManager.splitNameAndExtension(remainder);
			this.name = splittedName[0];//从文件中解析
			this.fileExtension = splittedName[1];
		} else {
			throw new IllegalStateException("Path does not point to a model.");
		}
		
//		String path = getPath();
		fileFullName = fullName;
		if ( ! FileSystemUtil.isFileExistent(fileFullName) || ! FileSystemUtil.isFileAccessible(fileFullName )){
			throw new IllegalStateException("Model can not be accessed.");
		}
		
	}
	
	public void setName(String name) throws UnsupportedEncodingException, JSONException {
		name = FileSystemUtil.getCleanFileName(name);
		if (name.equals(this.name)) {
			return ;
		}
//		不能直接修改文件名，因为新版本设计器文件名不与流程名不同
//		FsModelRevision rev = getHeadRevision();
//		Diagram diagram = DiagramBuilder.parseJson(new String(rev.getRepresentation(RepresentationType.JSON).getContent(), "utf8"));
//		String namespace = diagram.getStencilset().getNamespace();
//		
//		if (ModelTypeManager.getInstance().getModelType(namespace).renameFile(getParentDirectory().getPath(), this.name, name)){
//			this.name = name;
//		} else {
//			throw new IllegalArgumentException("Cannot rename model");
//		}
		ModelTypeManager.getInstance().getModelType(this.fileExtension).storeNameStringToModelFile(name, getFileFullName());
	}

	public String getName() {
		//获得模型名称
		return ModelTypeManager.getInstance().getModelType(this.fileExtension).getNameFromModelFile(getFileFullName());
//		return name;
	}
	
	public void setDescription(String description) {
		ModelTypeManager.getInstance().getModelType(this.fileExtension).storeDescriptionToModelFile(description, getFileFullName());
	}
	
	public String getDescription() {
		return ModelTypeManager.getInstance().getModelType(this.fileExtension).getDescriptionFromModelFile(getFileFullName());
	}
	
	public void setType(String type) {
		ModelTypeManager.getInstance().getModelType(this.fileExtension).storeTypeStringToModelFile(type, getFileFullName());
	}
	
	public String getType() {
		return ModelTypeManager.getInstance().getModelType(this.fileExtension).getTypeStringFromModelFile(getFileFullName());
	}
	
	public Date getCreationDate() {
		return new Date();
	}

	public FsModelRevision getHeadRevision() {
		return new FsModelRevision(this);
	}

	public FsModelRevision getRevision(int revisionNr) {
		FsModelRevision onlyRevision = getHeadRevision();
		assert (onlyRevision.getRevisionNumber() == revisionNr);
		return onlyRevision;
	}

	public FsDirectory getParentDirectory() {
		File rootDir = new File(FsRootDirectory.getSingleton().getPath());
		File parentDir = new File(pathPrefix);
		try {
			if (rootDir.getCanonicalPath().equals(parentDir.getCanonicalPath())){
				return FsRootDirectory.getSingleton();
			}
		} catch (IOException e) {
			throw new IllegalArgumentException("Cannot determine canonical path.", e);
		}
		return new FsDirectory(pathPrefix);
	}
	
	/**
	 * 更新模型文件
	 * @param jsonRep
	 * @param svgRep
	 * @param comment
	 */
	public void createRevision(String jsonRep, String svgRep, String comment) {
		doRevision(jsonRep, svgRep, comment, false);
	}
	
	/**
	 * 发布模型文件
	 * @param jsonRep
	 * @param svgRep
	 * @param comment
	 */
	public void publishRevision(String jsonRep, String svgRep, String comment) {
		doRevision(jsonRep, svgRep, comment, true);
	}
	
	private void doRevision(String jsonRep, String svgRep, String comment,boolean publishing){
		Diagram diagram;
		try {
			diagram = DiagramBuilder.parseJson(jsonRep);
		} catch (JSONException e) {
			throw new IllegalArgumentException("JSON representation of diagram is not valid.", e);
		}
		String namespace = diagram.getStencilset().getNamespace();
		//将改变保存到模型文件中
		ModelTypeManager.getInstance().getModelType(namespace).storeRevisionToModelFile(jsonRep, svgRep, getFileFullName());
		//生成新的版本
		if(publishing){
			FsDirectory parent = this.getParentDirectory();
			Properties index = new Properties();
			String indexPath = parent.getPath()+"/.index";
			try {
				FileInputStream fis = new FileInputStream(indexPath);
				index.load(fis);
				fis.close();
			} catch (FileNotFoundException e) {
				File file = new File(indexPath);
				try {
					file.createNewFile();
					FileInputStream fis = new FileInputStream(file);
					index.load(fis);
					fis.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			String fileFullName=getFileFullName();
			String currentVersion = fileFullName.substring(fileFullName.lastIndexOf("\\")+1, fileFullName.lastIndexOf(".signavio.xml"));
			File srcBPMN20File = new File(parent.getPath()+"/"+currentVersion+".bpmn20.xml");
			File srcSignavioFile = new File(parent.getPath()+"/"+currentVersion+".signavio.xml");
			File destBPMN20File;
			File destSignavioFile;
			if(currentVersion.equals("draft")){
				String latestVersion = index.getProperty("latest");
				int latest=0;
				if(StringUtils.isEmpty(latestVersion))
					latest = 1;
				else
					latest = Integer.parseInt(latestVersion)+1;
				currentVersion=String.valueOf(latest);
				index.setProperty("latest", String.valueOf(latest));
				//生成发布版本文件
				destBPMN20File = new File(parent.getPath()+"/"+latest+".bpmn20.xml");
				destSignavioFile = new File(parent.getPath()+"/"+latest+".signavio.xml");
				try {
					FileUtils.copyFile(srcBPMN20File, destBPMN20File);
					FileUtils.copyFile(srcSignavioFile, destSignavioFile);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}else{
				destBPMN20File=srcBPMN20File;
				destSignavioFile=srcSignavioFile;
			}
			
			//生成发布模型的流程图
			byte[] imageByte=generateImage(getHeadRevision(),RepresentationType.PNG,new PNGTranscoder());
			File pngFile = null;
			if(imageByte != null){
				pngFile = new File(parent.getPath()+"/"+currentVersion+".png");
				try {
					FileOutputStream fos = new FileOutputStream(pngFile);
					fos.write(imageByte);
					fos.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			//发布当前版本至Activiti引擎
			ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
			RepositoryService repositoryService = processEngine.getRepositoryService();
			DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
			FileInputStream bpmnFIS=null;
			FileInputStream pngFIS=null;
			try {
				bpmnFIS = new FileInputStream(destBPMN20File);
				deploymentBuilder.addInputStream(currentVersion+".bpmn20.xml", bpmnFIS);
				pngFIS = new FileInputStream(pngFile);
				if(pngFile!=null)
					deploymentBuilder.addInputStream(currentVersion+".png", pngFIS);
				JSONObject deployInfo = new JSONObject();
				Deployment deployment=deploymentBuilder.deploy();
				deployInfo.accumulate("id", deployment.getId());
				deployInfo.accumulate("name", deployment.getName());
				deployInfo.accumulate("time", deployment.getDeploymentTime());
				index.setProperty("deployed-"+currentVersion, deployInfo.toString());
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			} finally {
				try {
					if (bpmnFIS != null)
						bpmnFIS.close();
					if (pngFIS != null)
						pngFIS.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				FileOutputStream fos = new FileOutputStream(indexPath);
				index.store(fos, comment);
				fos.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private byte[] generateImage(FsModelRevision rev, RepresentationType type,
			PNGTranscoder transcoder) {
		FsModelRepresentationInfo rep = rev.getRepresentation(type);
		byte[] result = new byte[] {};
		// png does not exist, create it
		if (rep == null) {
			// get svg representation
			FsModelRepresentationInfo svg = rev
					.getRepresentation(RepresentationType.SVG);

			InputStream in = new ByteArrayInputStream(svg.getContent());

			// PNGTranscoder transcoder = new PNGTranscoder();
			try {
				TranscoderInput input = new TranscoderInput(in);

				// Setup output
				ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
				try {
					TranscoderOutput output = new TranscoderOutput(outBytes);
					// Do the transformation
					transcoder.transcode(input, output);
					result = outBytes.toByteArray();
					// save representation
					rev.createRepresentation(type, result);

					outBytes.close();
				} catch (TranscoderException e) {

				} catch (IOException e) {

				} finally {
					try {
						outBytes.close();
					} catch (IOException e) {

					}
				}
			} finally {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		} else {
			result = rep.getContent();
		}
		return result;
	}

	public FsModelRepresentationInfo getRepresentation(RepresentationType type) {
		
		byte [] resultingInfo = ModelTypeManager.getInstance().getModelType(this.fileExtension).getRepresentationInfoFromModelFile(type, getFileFullName());
		if (resultingInfo != null) {
			return new FsModelRepresentationInfo(resultingInfo);
		}
		return null;
		
	}

	public FsModelRepresentationInfo createRepresentation(RepresentationType type, byte[] content) {
		ModelTypeManager.getInstance().getModelType(this.fileExtension).storeRepresentationInfoToModelFile(type, content, getFileFullName());
		return getRepresentation(type);
	}
	
	public void delete() {
		FsModelRevision rev = getHeadRevision();
		Diagram diagram;
		try {
			diagram = DiagramBuilder.parseJson(new String(rev.getRepresentation(RepresentationType.JSON).getContent(), "utf8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Deleting model failed.", e);
		} catch (JSONException e) {
			throw new RuntimeException("Deleting model failed.", e);
		}
		String namespace = diagram.getStencilset().getNamespace();
		
		ModelTypeManager.getInstance().getModelType(namespace).deleteFile(getParentDirectory().getPath(), this.name);
	}
	
	/*
	 * 
	 * Private Functions
	 * 
	 */
	
	private String getPath(){
		return pathPrefix + File.separator + name + fileExtension;
	}
	
	public String getFileFullName(){
		return fileFullName;
	}
	
	public void moveTo(FsDirectory newParent) throws UnsupportedEncodingException, JSONException {
		
		FsDirectory parent = getParentDirectory();
		
		if (newParent.equals(parent)) {
			return ;
		}

		FsModelRevision rev = getHeadRevision();
		Diagram diagram = DiagramBuilder.parseJson(new String(rev.getRepresentation(RepresentationType.JSON).getContent(), "utf8"));
		String namespace = diagram.getStencilset().getNamespace();
		
		if (!ModelTypeManager.getInstance().getModelType(namespace).renameFile("", parent.getPath() + File.separator + this.name, newParent.getPath() + File.separator + this.name)){
			throw new IllegalArgumentException("Cannot move model");
		}
	}
	
	
	/*
	 * 
	 * INTERFACE COMPLIANCE METHODS 
	 * 
	 */
	
	@Override
	public boolean equals(Object o){
		if (o instanceof FsModel){
			FsModel m = (FsModel)o;
			return getFileFullName().equals(m.getFileFullName());
		}
		return false;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T extends FsSecureBusinessObject> Set<T> getChildren(Class<T> type) {
		if (FsModelRevision.class.isAssignableFrom(type)){
			return (Set<T>) getRevisions();
		} else {
			return super.getChildren(type);
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T extends FsSecureBusinessObject> Set<T> getParents(Class<T> businessObjectClass) {
		if (FsDirectory.class.isAssignableFrom(businessObjectClass)){
			Set<T> parents = new HashSet<T>(1);
			FsDirectory parentDirectory = getParentDirectory();
			if (parentDirectory != null) {
				parents.add((T)parentDirectory);
			}
			return parents;
		} else if (FsEntityManager.class.isAssignableFrom(businessObjectClass)){
			return (Set<T>)FsEntityManager.getSingletonSet();
		} else {
			return super.getParents(businessObjectClass);
		}
	}
	
	public Set<FsModelRevision> getRevisions() {
		Set<FsModelRevision> result = new HashSet<FsModelRevision>(1);
		result.add(getHeadRevision());
		return result;
	}

	@Override
	public String getId() {
		String path = getFileFullName();
		String rootPath = FsRootDirectory.getSingleton().getPath() + File.separator;
		if (path.startsWith(rootPath)){
			path = FsRootDirectory.ID_OF_SINGLETON + File.separator + path.substring(rootPath.length());
		}
		path = path.replace(File.separator, ";");
		return path;
	}
	
	public List<FsDirectory> getParentDirectories() {
		List<FsDirectory> parents = new ArrayList<FsDirectory>();
		FsDirectory parent = getParentDirectory();
		if(parent != null) {
			parents.add(parent);
			parents.addAll(parent.getParentDirectories());
		}
		return parents;
	}


}
