package org.kaldax.cli.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractCommand {
	
	abstract int configure(String [] args, int argsPos) throws IllegalArgumentException, IllegalAccessException ;
	protected abstract void execute() throws Exception;
	protected abstract void cleanup() throws Exception;
	
	protected static final HashMap<String,Object> mCommandContext = new HashMap<String,Object>(); 
	protected AtomicLong mStreamLastActivityTime = new AtomicLong(Long.MIN_VALUE);
	
	
	protected long getLastStreamActivityTime() {
		return mStreamLastActivityTime.get();
	}
	
	protected int mapArgumentsToFields(String [] args, int argsPos) throws IllegalArgumentException, IllegalAccessException {
		int pos = argsPos;

		HashSet<String> mandatoryFields = new HashSet<String>();
		
		for (Field field : this.getClass().getDeclaredFields()) {
			if(field.isAnnotationPresent(StringArg.class)) {
				StringArg string = field.getAnnotation(StringArg.class);
				if(string.mandatory()) {
					mandatoryFields.add(string.name());
				}
			} else if(field.isAnnotationPresent(BooleanSwitch.class)) {
				BooleanSwitch bool = field.getAnnotation(BooleanSwitch.class);
				if(bool.mandatory()) {
					mandatoryFields.add(bool.name());
				}
				
			}
		}
		
		while((pos!=args.length) && (args[pos].startsWith("--"))) {
			String arg = args[pos].substring(2);
			int eqPos = arg.indexOf("=");
			String key = null; 
			String val = null;
			if(eqPos!=-1) {
				key = arg.substring(0,eqPos);
				val = arg.substring(eqPos+1);
			} else {
				key=arg;
			}
			boolean set = false;
			for (Field field : this.getClass().getDeclaredFields()) {
				if(field.isAnnotationPresent(StringArg.class)) {
					StringArg string = field.getAnnotation(StringArg.class);
					if(string.name().equals(key)) {
						try {
							field.set(this, val);
						} catch (java.lang.IllegalArgumentException ia) {
							List<String> list = (List<String>) field.get(this);
							if(list==null) {
								list = new ArrayList<String>();
								field.set(this, list);
							}
							list.add(val);
						}
						mandatoryFields.remove(key);
						set = true;
						break;
					}
				} else if(field.isAnnotationPresent(BooleanSwitch.class)) {
					BooleanSwitch bool = field.getAnnotation(BooleanSwitch.class);
					if(bool.name().equals(key)) {
						if(val==null) {
							field.set(this, true);
						} else {
							field.set(this, Boolean.parseBoolean(val));
						}
						mandatoryFields.remove(key);
						set = true;
						break;
					}
				}
			}
			if(!set) {
				throw new IllegalArgumentException("Unknown parameter :"+key);
			}
			pos++;
		}
		if(mandatoryFields.size()>0) {
			StringBuilder missing = new StringBuilder();
			missing.append("Mandatory parameter(s) missing: ");
			String sep=null;
			for(String item : mandatoryFields) {
				if(sep!=null) {
					missing.append(sep);
				} else {
					sep=",";
				}
				missing.append(item);
			}
			throw new IllegalArgumentException(missing.toString());
		}
		return pos;
	}
	
	protected String stringPrompt(String msg) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		while(in.ready()) {
			in.read();
		}
		System.out.print(msg);
		return in.readLine();
	}
	
	protected File dirPrompt(String msg) throws IOException {
		String targetDirName = stringPrompt(msg);
		File targetDir = new File(targetDirName);
		if(!targetDir.exists()) {
			targetDir.mkdirs();
		}
		if(!targetDir.exists()) {
			throw new IOException("Unable to create directory "+targetDirName);
		} else if(!targetDir.isDirectory()) {
			throw new IOException(targetDirName+ " is not a directory.");
		}
		return targetDir;
	}
	
	protected File assureDir(String dir) throws IOException {
		File targetDir = new File(dir);
		if(!targetDir.exists()) {
			targetDir.mkdirs();
		}
		if(!targetDir.exists()) {
			throw new IOException("Unable to create directory "+dir);
		} else if(!targetDir.isDirectory()) {
			throw new IOException(dir+ " is not a directory.");
		}
		return targetDir;
	}
	
	/**
	 * Convert windows path format to unix format.
	 * @param path
	 * @return
	 */
	public static String unixifyPath(String path) {
		return path.replace("\\", "/");
	}
	
	protected void assureDir(File targetDirectory) throws IOException {
		if(!targetDirectory.exists()) {
			targetDirectory.mkdirs();
			if(!targetDirectory.exists()) {
				throw new IOException("Unable to create directory "+targetDirectory.getPath());
			}
		}
		
	}
	
	
	public void listJobs(String base,final File folder, List<String> result,FileFilter filter) throws IOException {
    	
        for (final File f : folder.listFiles()) {
            if (f.isDirectory()) {
            	listJobs(base,f, result,filter);
            }
            if (f.isFile()) {
            	if ((filter == null) || filter.accept(f)) {
	            	String cp = unixifyPath( f.getCanonicalPath() );
	            	if(cp.endsWith(".xml")) {
		            	cp = cp.substring(base.length()+1);
		                result.add(cp);
	            	}
            	}
            }
        }
    }


	
	

}
