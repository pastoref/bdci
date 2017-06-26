package theoremProver.z3;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Z3Properties {
	private static final long DEFAULT_MAX_EXECUTION_TIME = 600;//seconds
	private String pathFile = "z3.properties";
	private Properties properties;
	
	public Z3Properties(){
		
	}
	
	public static Z3Properties getInstance() {
		return new Z3Properties();
	}
	
	private String getProperty(String key) {
		if ( properties == null ){
			loadFile(pathFile);
		}
		String ret = properties.getProperty(key);
		
		return ret;
	}
	
	public void loadFile(String pathProperties) {
		FileInputStream inStream = null;
		try {
			inStream = new FileInputStream(new File(pathProperties));
			try {
				properties = new Properties();
				properties.load(inStream);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				inStream.close();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			
		}
	}
	
	public String getZ3path() {
		return getProperty("z3path");
	}
	
	public String getInvariantsDir() {
		return getProperty("invariantsDir");
	}

	
	public Long getMaxExecutionTime(){
		String maxString = getProperty("maxExecutionTime");
		if ( maxString == null ){
			return DEFAULT_MAX_EXECUTION_TIME;
		}
		return Long.valueOf(maxString);
	}
	
}
