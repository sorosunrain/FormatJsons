package com.sunrain.formatjson;

import java.io.File;

public class Utils {	
	
	
	public static final String getSaveImagePath(String basePath){		
		String path = basePath + "/images/";
		fixDirectory(path);
		return path;
	}
	
	public static final String getSaveJsonPath(String basePath){		
		String path = basePath + "/jsons/";
		fixDirectory(path);
		return path;
	}
	
	
	public static final void fixDirectory(String path){
		
		File file = new File(path);
		if(!file.exists()){
			file.mkdirs();
		}
		
	}
	
}
