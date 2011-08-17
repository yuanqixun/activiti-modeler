import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.yahoo.platform.yui.compressor.JavaScriptCompressor;


public class Test {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws  
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		System.out.println("--------------");
		ResourcePatternResolver rps = new PathMatchingResourcePatternResolver();
		try {
//			Resource[] result = rps.getResources("classpath*:/**/*.cfg.xml");
			Resource[] result = rps.getResources("classpath*:/**/MANIFEST.MF");
			for (int i = 0; i < result.length; i++) {
				System.out.println(result[i]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("--------------");
		
		
//		StringWriter sw = new StringWriter();
		
		
//        sw.append("/* ").append("aaaa").append(" */\n");
//		String path = "src/main/webapp/editor/scripts/Core/SVG/label.js";
//		InputStream input = new FileInputStream(new File(path));
//		JavaScriptCompressor compressor = new JavaScriptCompressor(
//				new InputStreamReader(input), null);
//		System.out.println(compressor.hashCode());
//		compressor.compress(sw, -1, false, false, false, false);
//		input.close();
//		System.out.println(sw.toString());
		
	}

}
