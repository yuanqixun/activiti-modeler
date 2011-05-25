import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import com.yahoo.platform.yui.compressor.JavaScriptCompressor;


public class Test {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws  
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		StringWriter sw = new StringWriter();
		
		
        sw.append("/* ").append("aaaa").append(" */\n");
		String path = "D:\\AppServer\\apache-tomcat-7.0.14\\wtpwebapps\\activiti-modeler\\editor\\scripts\\utils.js";
		InputStream input = new FileInputStream(new File(path));
		JavaScriptCompressor compressor = new JavaScriptCompressor(
				new InputStreamReader(input), null);
		System.out.println(compressor.hashCode());
		compressor.compress(sw, 1, false, false, true, false);
		input.close();
		System.out.println(sw.toString());
		
	}

}
