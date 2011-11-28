package softwareallies.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;

public class NamespacesFixer {

	/**
	 * NameSpace Fixer
	 * Copyright 2011 (c) by SoftwareAllies/Nearatec
	 * All Rights Reserved
	 * 
	 * The purpose of this class is to automate the adding and removing name spaces from visual force's
	 * pages, apex classes and triggers. 
	 * 
	 * @param args
	 * @author Carlos Castillo
	 * @copyright SoftwareAllies/Nearatec 2011
	 * 
	 * Revision History
	 * ----------------
	 * 2011-03-23 Carlos Castillo (SoftwareAllies) – File created, Methods fixFile,getFileLines,openFileToRead,openFileToWrite,addNameSpace and removeNamespace created
	 * 2011-03-24 Carlos Castillo (SoftwareAllies) – Methods getAllFiles,fixDirectory,getTokens created,main modified
	 * 2011-03-25 Carlos Castillo (SoftwareAllies) – Methods isClassName,isMethodName,isPage created
	 */
	
	/*
	 * 
	 *	Variables declarations 
	 * 
	 */
	private java.io.LineNumberReader reader;
	private java.io.PrintWriter writer;
	private java.util.TreeMap<String,java.util.LinkedList<String>> elements; 
	private java.util.TreeMap<String,String> paths;
	private String namespace;
	/*
	 *	Methods declarations 
	 *
	 */
	
	public static void main(String[] args) {
		java.io.File mainPath;
		if(args.length==3){
			mainPath=new java.io.File(args[0]+"\\src");
			if(mainPath.isDirectory()){
				if(Boolean.parseBoolean(args[2]))
					System.out.println(">>Fixing... <namespace:"+args[1]+"> <action:add>");//log comments//
				else
					System.out.println(">>Fixing... <namespace:"+args[1]+"> <action:remove>");//log comments//
				/**
				 * args[0] ----  path
				 * args[1] ----  name space
				 * args[2] ----  mode {true:add name space ; false:remove name space}
				 * */
				new NamespacesFixer().fixProject(mainPath,args[1],Boolean.parseBoolean(args[2]));
			}
			else{
				System.err.println(mainPath+ "is not a valid path!");
			}
		}
		else{
			System.err.println("Wrong arguments received! should be 3 (path namespace mode)");
			System.err.println("You sent: ");
			for(String arg:args)
				System.err.println("	>>"+arg);
		}
		System.exit(0);
	}
	private void fixProject(File mainPath,String namespace,boolean typeOfFix) {
		elements=new java.util.TreeMap<String,java.util.LinkedList<String>>();
		setNamespace(namespace);
		setElementsAndPaths(mainPath);
		for(String path:paths.keySet()){
			System.out.println("	Folder: "+mainPath+"\\"+path);
			fixDirectory(path, paths.get(path), typeOfFix);
		}
	}
	private void setElementsAndPaths(File mainPath){
		//this map have the name of the target folder and the type of file to get from
		java.util.Map<String,String> targetPaths=new java.util.TreeMap<String, String>();
		targetPaths.put("classes", ".cls");			// add (.)
		targetPaths.put("objects", ".object");		//
		targetPaths.put("pages", ".page");			// add (__)
		targetPaths.put("triggers", ".trigger");	// add (.)		
		
		java.util.TreeMap<String,String> path=new java.util.TreeMap<String,String>();
		java.util.LinkedList<String> elements;
		File targetPath=null;
		for(String target:targetPaths.keySet()){
			elements=new LinkedList<String>();
			if(mainPath.isDirectory()){
				for(File directory:mainPath.listFiles()){
					if(directory.toString().endsWith(target)){
						if(directory.isDirectory()){
							targetPath=directory;
							path.put(targetPath.getPath(),targetPaths.get(target));
							break;
						}
						
					}
				}
				for(File file:getAllFiles(targetPath.getPath(),targetPaths.get(target))){
					if(!target.equals("objects"))
						elements.add(file.getName().replace(targetPaths.get(target),""));
				}
				this.elements.put(target,elements); 
			}
			
		}
		setPaths(path);
		
	}
	
	private void fixDirectory(String path,String filter,boolean typeOfFix){
		for(java.io.File file:getAllFiles(path,filter)){
			System.out.print("		File: "+file);//log comments//
			fixFile(file,typeOfFix);
			System.out.println(" <fixed>");//log comments//
		}
	}
	
	// this method returns only the files that satisfy the filter 
	//			the filter must be the file's extension (e.g. cls,page,trigger)
	private java.util.List<java.io.File> getAllFiles(String path,String filter){
		java.io.File directory=new java.io.File(path);
		java.io.File[] allFiles = null;
		java.util.List<java.io.File> files=new java.util.LinkedList<java.io.File>();
		if(directory.isDirectory()){
			allFiles=directory.listFiles();
		}
		for(java.io.File file:allFiles){
			if(file.getName().endsWith(filter)){
				files.add(file);
			}
		}
		return files;
	}
	
	/*
	 * typeOfFix:
	 * 				true  --> add
	 * 				false --> remove
	 * */ 
	private void fixFile(java.io.File file,boolean typeOfFix){
		java.util.LinkedList<String> lines=getFileLines(file);
		try{
		writer=openFileToWrite(file);
			for(String line:lines){
				if(typeOfFix)
					writer.println(addNamespace(addNamespace(line),elements));
				else
					writer.println(removeNamespace(line,"__","."));
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		finally{
			writer.close();
		}
	}
	//This method adds the name space to the custom Objects' reference, should receive one line at the time
	private String addNamespace(String oldLine){
		String newLine="";
		java.util.LinkedList<String> tokens =getTokens(oldLine);
		int counter=0;
		while(counter++<tokens.size()){
			if(tokens.get(counter-1).endsWith("__c")||tokens.get(counter-1).endsWith("__r")){
				if(!tokens.get(counter-1).startsWith(getNamespace()+"__"))
					newLine+=getNamespace()+"__"+tokens.get(counter-1);
				else
					newLine+=tokens.get(counter-1);
			}
			else{
				newLine+=tokens.get(counter-1);
			}
		}
		return newLine;
	}
	
	private String addNamespace(String oldLine,java.util.TreeMap<String,java.util.LinkedList<String>> customClassesList){
		String newLine="",token="";
		java.util.Iterator<String> keys;
		LinkedList<String> tokens=getTokens(oldLine);
		boolean flag;
			for(int i=0;i<tokens.size();i++){
				token=tokens.get(i);
				flag=false;
				if(Character.isLetter(token.charAt(0))){
					keys=customClassesList.keySet().iterator();	
					while(keys.hasNext()&&!flag){	
						for(String element: customClassesList.get(keys.next())){
							if(element.equalsIgnoreCase(token)){
								
								if((isClass(element)&&!isClassName(getTokens(oldLine),element))||(isTrigger(element)&&!isTriggerName(getTokens(oldLine),element))){
									try{
									if(!namespace.equalsIgnoreCase(tokens.get(i-2)))
										newLine+=namespace+"."+element;
									else
										newLine+=element;
									}
									catch(IndexOutOfBoundsException e){
										newLine+=namespace+"."+element;
									}
									flag=true;
								}
								else if(isPage(token)){
									newLine+=namespace+"__"+element;
									flag=true;
								}
								break;
							}	
						}
					}
					if(!flag)
						newLine+=token;
			}
				else
					newLine+=token;
			}
		return newLine;
	}
	private boolean isPage(String token){
		boolean flag=false;
			for(String element:elements.get("pages")){
				if(token.equalsIgnoreCase(element)){
					flag=true;
					break;
				}
			}
		return flag;
	}
	private boolean isTrigger(String token){
		boolean flag=false;
		for(String element:elements.get("triggers")){
			if(token.equalsIgnoreCase(element)){
				flag=true;
				break;
			}
		}
	return flag;
	}
	private boolean isTriggerName(java.util.LinkedList<String> tokens,String name){
		boolean flag=false;
		byte state=0;
		for(String token:tokens){
			if(state==0){
				if(token.equalsIgnoreCase("trigger")){
					state=1;
				}
			}
			else if(state==1){
				if(token.equalsIgnoreCase(name)){
					flag=true;
					break;
				}
			}
		}
		return flag;
	}
	private boolean isClass(String token){
		boolean flag=false;
		for(String element:elements.get("classes")){
			if(token.equalsIgnoreCase(element)){
				flag=true;
				break;
			}
		}
	return flag;
	}
	private boolean isClassName(java.util.LinkedList<String> tokens,String name){
		boolean flag=false;
		byte state=0;
		for(String token:tokens){
			if(state==0){
				if(token.equalsIgnoreCase("class")){
					state=1;
				}
			}
			else if(state==1){
				if(token.equalsIgnoreCase(name)){
					flag=true;
					break;
				}
			}
		}
		return flag;
	}
	//This method removes the name space from the custom Objects' reference, with no case sensitive and maintain intact the rest of the string
	// should receives one line at the time
 	private String removeNamespace(String line,String... extension/* '__'  or  '.'  */){
		for(String namespaceExtension:extension)
			line=line.replaceAll("(?i)"+getNamespace()+namespaceExtension, "");
		return line;
	}
 	
 	/** Auxiliary methods to open the files -->*/
 	private java.io.LineNumberReader openFileToRead(java.io.File file)throws FileNotFoundException,IOException{
		if(!file.exists()){
			file.createNewFile();
		}
 		return new java.io.LineNumberReader(new java.io.FileReader(file));
	}
 	private java.io.PrintWriter openFileToWrite(java.io.File file) throws IOException {
		if(!file.exists()){
			file.createNewFile();
		}
		return new java.io.PrintWriter(new java.io.FileWriter(file));
	}
 	// Reads a file and store each line in a list
 	private java.util.LinkedList<String> getFileLines(java.io.File file){
		java.util.LinkedList<String> linesList=new java.util.LinkedList<String>();
		String aux="";
		try{
			setReader(openFileToRead(file));
			while((aux=reader.readLine())!=null){
				linesList.add(aux);
				
			}
			reader.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return linesList;
	}
	
 	//This method splits out the received line into Tokens avoiding loosing the characters that standard tokenizer does.
	private static java.util.LinkedList<String> getTokens(String inputString)
	{
		java.util.LinkedList<String> tokens=new java.util.LinkedList<String>();
		String token="";
		char currentChar=0;
		int i=0;
		//loop over the String
		while(i<inputString.length())
		{
			
			currentChar=inputString.charAt(i);
			
			// * Split the string if the current character is one of the followers *
			if(currentChar=='\t'||currentChar=='|'||currentChar=='&'||currentChar=='!'||currentChar==' '||currentChar=='"'||currentChar=='\''||currentChar=='<'||currentChar=='*'||currentChar=='-'||currentChar=='+'||currentChar=='/'||currentChar=='>'||currentChar=='='||currentChar=='%'||currentChar==','||currentChar=='.'||currentChar==';'||currentChar==':'||currentChar=='('||currentChar==')'||currentChar=='['||currentChar==']'||currentChar=='^')
			{
				// * save the token formed before the split*
				if(token.length()>0)
				{
					tokens.add(token.trim());
					token="";
				}
				// * save the splitter character as a token too *
				tokens.add(""+currentChar);
			}
			// * Any other character different of the splitters  is concatenated in the token to be saved later *
			else{
				token+=currentChar;
			}
			i++;
		}
		// * store the token formed at the end *
		if(token.length()>0)
		{
			tokens.add(token.trim());
			token="";
		}
		return tokens;
	}
	/** <-- Auxiliary methods to open the files */
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
	public String getNamespace() {
		return namespace;
	}
	public void setElements(String key,java.util.LinkedList<String> elements) {
		this.elements.put(key,elements);
	}
	public java.util.TreeMap<String,java.util.LinkedList<String>> getElements() {
		return elements;
	}
	public void setPaths(java.util.TreeMap<String,String> paths) {
		this.paths = paths;
	}
	public java.util.TreeMap<String,String> getPaths() {
		return paths;
	}
	public void setReader(java.io.LineNumberReader reader){
		this.reader=reader;
	}
	public java.io.LineNumberReader getReader(){
		return this.reader;
	}
}
