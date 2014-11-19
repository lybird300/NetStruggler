package NetStruggler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.Map.Entry;

import au.com.bytecode.opencsv.CSVReader;
import repast.simphony.context.Context;
import repast.simphony.context.space.graph.ContextJungNetwork;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunState;
import repast.simphony.space.graph.RepastEdge;

public class DataIO {
	private double avgScore = 0.;
	private int numUniq = 0;
	private double maxScore = 0.;
	private int nkID;
	private int ptID;
		
	public DataIO(int nkID, int ptID) {
		this.nkID = nkID;
		this.ptID = ptID;
	}
	
	public DataIO(int nkID) {
		this.nkID = nkID;
	}

	/**
	 * Output the generated organizational social network into a Pajek (.net) file
	 * Naming: net_P(id of current problem)_T(current step).net
	 * Notes: (a) Pajek uses "\r" instead of "\n" as the line breaker.
	 * (b) If the network is empty (with no ties), the corresponding Pajek file only has a "Vertices" section, no "Edges" section.
	 * (c) If the network only has one edge, output "Edge" instead of "Edges" to the .net files.
   	 * (d) If the network is non-directed, use "Edges"; otherwise, use "Arcs" instead. 
	 * @param currentProblem
	 * 		the ID of the problem under processing (start from 1)
	 * @param orgSize
	 * 		the number of agents, i.e., nodes; that is, the size of the organizational network
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void outputPajek(String folderName, int currentTick, int Run, boolean isSNet){
		Context<Object> context = RunState.getInstance().getMasterContext();
		ContextJungNetwork<OrgMember> targetNet;
		String fileName;
		if(isSNet){
			targetNet = (ContextJungNetwork<OrgMember>) context.getProjection("socialNetwork");
			fileName = folderName + "/Snet_T" + currentTick + ".net";
		}
		else{
			targetNet = (ContextJungNetwork<OrgMember>) context.getProjection("ktNetwork");
			fileName = folderName + "/Knet_T" + currentTick + ".net";
		}
		try{
			FileOutputStream output = new FileOutputStream(fileName, false);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));
			writer.write("*Vertices " + targetNet.size());
			writer.newLine();
			for (int i = 1; i <= targetNet.size(); i++) {
				writer.write(i + " \"" + String.valueOf(i) + "\""); //caution: although the name of a node can start from "0", its index (first number in the .pajek file) should always start with 1
				writer.newLine();
			}
			if (targetNet.numEdges() == 0)
				System.out.println(fileName + " is an empty network.");
			else if (targetNet.numEdges() == 1) {
				System.out.println(fileName + " only has one edge.");
				writer.write("*Edge");
			} else	
				writer.write("*Edges");
			writer.newLine();
			for (RepastEdge<OrgMember> e : targetNet.getEdges()) {
				String n1_id = e.getSource().getID();
				String n2_id = e.getTarget().getID();
				writer.write(n1_id + " " + n2_id + " " + e.getWeight());
				writer.newLine();
			}
			writer.flush();
			writer.close();
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	/**
     * Create an undirected network out of a Pajek .net file.
     * Below is an example format for an undirected graph with edge weights and edges specified in non-list form: <br>
     * <pre>
     * *vertices <# of vertices> 
     * 1 "a" 
     * 2 "b" 
     * 3 "c" 
     * *edges 
     * 1 2 0.1 
     * 1 3 0.9 
     * 2 3 1.0 
     * </pre>
     * @param fileName
     * 		the Pajek file
	 * @param net
	 * 		the network to be specified
	 */
	public void inputPajek(String fileName, ContextJungNetwork<OrgMember> net){
		try {
			FileInputStream input = new FileInputStream(fileName);
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));
			String currentLine = null;
			StringTokenizer tokenizer = null;
			currentLine = reader.readLine();
            tokenizer = new StringTokenizer(currentLine);
            if (!tokenizer.nextToken().toLowerCase().startsWith("*vertices"))
            	System.out.println("Pajek file parse error: '*vertices' not first token");
            int numVertices = Integer.parseInt(tokenizer.nextToken());
            if(numVertices != net.size())
            	System.out.println("Pajek file parse error: the number of vertices does not match");
            
            //scan the .net file until reach the line of "*edge(s)"
            do{
            	currentLine = reader.readLine().trim();
            } while (!currentLine.startsWith("*"));
            
			int currentStartId = -1;
            int currentEndId = -1;
            //the starting line in the following "while" statement is the line right below "edges"
            //since the previous "while" statement ends with reading the line with "edges" 
            while ((currentLine = reader.readLine()) != null){
                currentLine = currentLine.trim();
                if (currentLine.length() == 0) {
                    break;
                }     
                if (currentLine.startsWith("*" ))
                    continue;
                tokenizer = new StringTokenizer(currentLine);
                currentStartId = Integer.parseInt(tokenizer.nextToken());              
                currentEndId = Integer.parseInt(tokenizer.nextToken());
                double weight = Double.parseDouble(tokenizer.nextToken());
                if (currentStartId == currentEndId) {
                    System.out.println("Same source and target nodes");
                    break;
                }
                OrgMember start = null;
                OrgMember end = null;
                for(OrgMember om : net.getNodes()){
                	int id = Integer.parseInt(om.getID());
                	if(id == currentStartId) start = om;
                	if(id == currentEndId) end = om;
                	if(start!=null && end!=null) break;
                }
                if(!net.isAdjacent(start, end))
                	net.addEdge(start, end, weight);
                else
                	net.getEdge(start, end).setWeight(weight);
            }
            reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Import a set of LHS samples from the whole LHS design matrix stored in a CSV file 
	 * @param from
	 * 	the starting sample (inclusive)
	 * @param to
	 * 	the ending sample (inclusive)
	 */
	/*public void importLHS(String fileName, int from, int to){
		FileInputStream input = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(reader);
		for (CSVRecord record : records) {
		  String lastName = record.get("Last Name");
		  String firstName = record.get("First Name");
		}
		
	}*/
	
	/**
	 * Import a specific sample from the whole LHS design matrix stored in a CSV file 
	 * @param target
	 * 	the target sample
	 * Note: assume the first line of the csv file is colnames/header
	 */
	public Param importLHS(String fileName, int target) throws IOException {
		CSVReader reader = new CSVReader(new FileReader(fileName), ',', '\'', target); 
		String[] record = reader.readNext();
        Param pm = new Param(record, target); 
        reader.close();
        return pm;
    }
	
	/**
	 * Output each organizational member's attributes at a specific click to complement the network data
	 * @param fileName
	 * @param orgArea
	 * 		the total number of expertise areas in the organization
	 * @param memberArea
	 * 		the number of expertise areas of each member
	 * @param list
	 * 		the list of organizational members
	 * @param currentTick
	 */
	//keep this function for R analysis
	public  void outputAgentAttributes(String conditionID, ArrayList<OrgMember> list, int currentTick, int n, String outputMark){
		//String fileName = "C:/MData/DataSet" + conditionID + "/Agent_nk" + nkID + "_pt" + ptID + "_" + outputMark + "_t" + currentTick + ".csv";
		String fileName = "C:/MData/DataSet" + conditionID + "/Agent_nk" + nkID + "_" + outputMark + "_t" + currentTick + ".csv";
		try{
			File assignment = new File(fileName);
			boolean newFile = assignment.createNewFile();
			FileOutputStream file = new FileOutputStream(assignment, true);//the second parameter is true means appending
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(file));	
	
			if(newFile){//if the file does not exist
				//create the head row of the .csv file
			    writer.append("Tick");
			    writer.append(',');
			    writer.append("Member");
			    writer.append(',');
			    for(int k = 1; k<=n; k++){
			    	writer.append("Dim" + Integer.toString(k));
			    	writer.append(',');
			    }
			    writer.append("Score");
			    writer.append(',');
			    writer.append("Strategy");
			    writer.newLine();
			}
			for(OrgMember om : list){
				int[] solution = om.getSolution().clone();
				writer.append(Integer.toString(currentTick));
				writer.append(',');
				writer.append(om.getID());
				writer.append(',');
				for(int k=1; k<=n; k++){
			    	writer.append(Integer.toString(solution[k-1]));
			    	writer.append(',');
				}
				writer.append(String.valueOf(om.getScore()));
				writer.append(',');
				writer.append(String.valueOf(om.getIntrType()));
				writer.newLine();
			}
			writer.flush();
			writer.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void outputNodeAttributes(int problemID, ArrayList<OrgMember> list, int currentTick, int n, String outputMark){
		//String fileName = "C:/MData/DataSet" + problemID + "/Node_nk" + nkID + "_pt" + ptID + "_" + outputMark + "_t" + currentTick + ".csv";
		String fileName = "C:/MData/DataSet" + problemID + "/Node_nk" + nkID + "_" + outputMark + "_t" + currentTick + ".csv";
		try{
			File assignment = new File(fileName);
			boolean newFile = assignment.createNewFile();
			FileOutputStream file = new FileOutputStream(assignment, true);//the second parameter is true means appending
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(file));	
	
			if(newFile){//if the file does not exist
				//create the head row of the .csv file
			    writer.append("Id");
			    writer.append(',');
			    //writer.append("Label");
			    //writer.append(',');
			    /*for(int k = 1; k<=n; k++){
			    	writer.append("Dim" + Integer.toString(k));
			    	writer.append(',');
			    }
			    writer.append("Score");
			    writer.append(',');
			    writer.append("Strategy");
			  	writer.append(',');
			    writer.append("ActRate");
			    writer.append(',');*/
			    writer.append("RelativeKnowExcTime");
			    writer.append(',');
			    writer.append("RelativeEmbedKnowExcTime");
			    //writer.append(',');
			    //writer.append("GlobalInterTime");
			    writer.newLine();
			}
			for(OrgMember om : list){
				//int[] solution = om.getSolution().clone();
				writer.append(om.getID());
				writer.append(',');
				//writer.append(om.getID());
				//writer.append(',');
				/*for(int k=1; k<=n; k++){
			    	writer.append(Integer.toString(solution[k-1]));
			    	writer.append(',');
				}
				writer.append(String.valueOf(adjustScore(om.getScore())));
				writer.append(',');
				writer.append(String.valueOf(om.getIntrType()));
				writer.append(',');
				writer.append(String.valueOf(om.getActivityRate()));
				writer.append(',');*/
				writer.append(String.valueOf(om.getInteractionTime(2)));
				writer.append(',');
				writer.append(String.valueOf(om.getInteractionTime(1)));
				//writer.append(',');
				//writer.append(String.valueOf(om.getInteractionTime(2)));
				writer.newLine();
			}
			writer.flush();
			writer.close();
		}catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	@SuppressWarnings("unchecked")
	public  void outputTieAttributes(int problemID, int currentTick, boolean isSNet, String outputMark){
		Context<Object> context = RunState.getInstance().getMasterContext();
		ContextJungNetwork<OrgMember> targetNet;
		String fileName;
		if(isSNet){
			targetNet = (ContextJungNetwork<OrgMember>) context.getProjection("socialNetwork");
			//fileName = "C:/MData/DataSet" + problemID + "/STie_nk" + nkID + "_pt" + ptID + "_" + outputMark + "_t" + currentTick + ".csv";
			fileName = "C:/MData/DataSet" + problemID + "/STie_nk" + nkID + "_" + outputMark + "_t" + currentTick + ".csv";
		}
		else{
			targetNet = (ContextJungNetwork<OrgMember>) context.getProjection("ktNetwork");
			//fileName = "C:/MData/DataSet" + problemID + "/KTie_nk" + nkID + "_pt" + ptID + "_" + outputMark + "_t" + currentTick + ".csv";
			fileName = "C:/MData/DataSet" + problemID + "/KTie_nk" + nkID + "_" + outputMark + "_t" + currentTick + ".csv";
		}
		
		try{
			File assignment = new File(fileName);
			boolean newFile = assignment.createNewFile();
			FileOutputStream file = new FileOutputStream(assignment, true);//the second parameter is true means appending
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(file));	
	
			if(newFile){//if the file does not exist
				//create the head row of the .csv file
			    writer.append("Source");
			    writer.append(',');
			    writer.append("Target");
			    writer.append(',');
			    writer.append("Weight");
			    if(isSNet){
			    	writer.append(',');
			    	writer.append("Similarity");
			    	/*writer.append("Frequency");
			    	writer.append(',');
			    	writer.append("Age");*/
			    }
			    writer.newLine();
			}
			for (RepastEdge<OrgMember> e : targetNet.getEdges()) {
				writer.append(e.getSource().getID());
				writer.append(',');
				writer.append(e.getTarget().getID());
				writer.append(',');
				writer.append(String.valueOf(e.getWeight()));
				if(isSNet){
					writer.append(',');
					writer.append(String.valueOf(((CustomTie)e).getSimilarity()));
					/*writer.append(String.valueOf(((CustomTie)e).getUseFrequency()));
					writer.append(',');
					writer.append(String.valueOf(((CustomTie)e).getAge(currentTick)));*/
				}
				writer.newLine();
			}
			writer.flush();
			writer.close();
		} catch(IOException e){
			e.printStackTrace();
		}
		
	}
	
	public void outputIntervalFreq(int problemID, int replicateID, double actDist, Set<Entry<Integer,Integer>> intervalDist, String outputMark){
		//String fileName = "C:/MData/DataSet" + problemID + "/IntervalFreq_nk" + nkID + "_pt" + ptID + "_" + outputMark + "_t" + currentTick + ".csv";
		String actType = "B";
		if(actDist > 8)
			actType = "C";//all active
		String fileName = "C:/MData/DataSet" + problemID + "/IntervalFreq_act" + actType + "_rep" + replicateID + "_" + outputMark + ".csv";
		try{
			File assignment = new File(fileName);
			boolean newFile = assignment.createNewFile();
			FileOutputStream file = new FileOutputStream(assignment, true);//the second parameter is true means appending
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(file));	
	
			if(newFile){//if the file does not exist
				//create the head row of the .csv file
			    writer.append("Interval");
			    writer.append(',');
			    writer.append("Count");
			    writer.newLine();
			}
			for(Entry<Integer, Integer> interval : intervalDist){
				writer.append(Integer.toString(interval.getKey()));
				writer.append(',');
				writer.append(Integer.toString(interval.getValue()));
				writer.newLine();
			}
			writer.flush();
			writer.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void outputBatchAvgScore(int problemID, int nkID, ArrayList<Double> scoreList, String name, int seed, int actType){
		String act = "B";
		if(actType == 2)
			act = "C";//all active
		else if(actType == 3)
			act = "D";//normal distribution
		else if(actType == 0)
			act = "A";//none active
		String fileName = "C:/MData/DataSet" + problemID + "/BatchAvgScore_act" + act + "_" + name + ".csv";
		try{
			File tResult = new File(fileName);
			FileOutputStream file = new FileOutputStream(tResult, true);//the second parameter is true means appending
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(file));
			writer.append(String.valueOf(nkID));
			writer.append(',');
			writer.append(String.valueOf(seed));
			writer.append(',');
			for(Double score : scoreList){
				writer.append(Double.toString(score));
				writer.append(',');
			}
			writer.newLine();
			writer.flush();
			writer.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void outputBatchMaxScore(int problemID, ArrayList<Double> scoreList, int endTick, String name, int seed, double actDist) {
		String fileTitle = "C:/MData/DataSet" + problemID + "/BatchMaxScore_" + name + ".csv";
		try{
			File tResult = new File(fileTitle);
			boolean newFile = tResult.createNewFile();
			FileOutputStream file = new FileOutputStream(tResult, true);//the second parameter is true means appending
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(file));	
			if(newFile){//if the file does not exist
				writer.append("nkID");
			    writer.append(',');
			    //writer.append("ptID");
			    //writer.append(',');
			    writer.append("ActDist");
			    writer.append(',');
			    writer.append("Run");
			    writer.append(',');
			    for(int i = 1; i<= endTick; i++){
			    	writer.append(Integer.toString(i));
			    	writer.append(',');
			    }
			    writer.newLine();
			}
			writer.append(String.valueOf(nkID));
		    writer.append(',');
		    //writer.append(String.valueOf(ptID));
		    //writer.append(',');
		    if(actDist > 8) 
		    	writer.append("All");
		    else if(actDist < 0)
		    	writer.append("None");
		    else
		    	writer.append(String.valueOf((int)actDist));
		    writer.append(',');
		    writer.append(String.valueOf(seed));
		    writer.append(',');
			for(Double score : scoreList){
				writer.append(Double.toString(score));
				writer.append(',');
			}
			writer.newLine();
			writer.flush();
			writer.close();			
		}catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void outputBatchUniSln(int problemID, ArrayList<Integer> numSlnList, int endTick, String name, int seed, double actDist) {
		String actType = "B";
		if(actDist > 8)
			actType = "C";//all active
		if(actDist < 0)
			actType = "A";//none active
		String fileName = "C:/MData/DataSet" + problemID + "/BatchUniSln_act" + actType + "_" + name + ".csv";
		try{
			File tResult = new File(fileName);
			boolean newFile = tResult.createNewFile();
			FileOutputStream file = new FileOutputStream(tResult, true);//the second parameter is true means appending
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(file));	
			if(newFile){//if the file does not exist
				/*writer.append("nkID");
			    writer.append(',');
			    writer.append("ptID");
			    writer.append(',');
			    writer.append("ActDist");
			    writer.append(',');*/
			    writer.append("Run");
			    writer.append(',');
			    for(int i = 1; i<= endTick; i++){
			    	writer.append(Integer.toString(i));
			    	writer.append(',');
			    }
			    writer.newLine();
			}
			/*writer.append(String.valueOf(nkID));
		    writer.append(',');
		    writer.append(String.valueOf(ptID));
		    writer.append(',');
		    if(actDist > 8) 
		    	writer.append("All");
		    else if(actDist < 0)
		    	writer.append("None");
		    else
		    	writer.append(String.valueOf((int)actDist));
		    writer.append(',');*/
		    writer.append(String.valueOf(seed));
		    writer.append(',');
			for(Integer score : numSlnList){
				writer.append(Double.toString(score));
				writer.append(',');
			}
			writer.newLine();
			writer.flush();
			writer.close();			
		}catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	@SuppressWarnings("unchecked")
	//when correlation (Pearson's r) returns NAN, it is because the standard deviation is zero
	public void outputProcessData(int problemID, int currentTick, Boolean isSNet, ArrayList<OrgMember> memberList, int orgSize, int N, String outputMark){
		Context<Object> context = RunState.getInstance().getMasterContext();
		ContextJungNetwork<OrgMember> net;
		String fileName;
		if(isSNet){
			net = (ContextJungNetwork<OrgMember>) context.getProjection("socialNetwork");
			//fileName = "C:/MData/DataSet" + problemID + "/SNet_nk" + nkID + "_pt" + ptID + "_" + outputMark + ".csv";
			fileName = "C:/MData/DataSet" + problemID + "/SNet_nk" + nkID + "_" + outputMark + ".csv";
		}
		else{
			net = (ContextJungNetwork<OrgMember>) context.getProjection("ktNetwork");
			//fileName = "C:/MData/DataSet" + problemID + "/kNet_nk" + nkID + "_pt" + ptID + "_" + outputMark +  ".csv";
			fileName = "C:/MData/DataSet" + problemID + "/kNet_nk" + nkID + "_" + outputMark +  ".csv";
		}
		NetOperation no = new NetOperation(net, orgSize);
		try{
			File tResult = new File(fileName);
			boolean newFile = tResult.createNewFile();
			FileOutputStream file = new FileOutputStream(tResult, true);//the second parameter is true means appending
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(file));	
			if(newFile){//if the file does not exist
				writer.append("Tick");
			    writer.append(',');
				if(isSNet){
				    writer.append("MaxScore");
				    writer.append(',');					
				    writer.append("AvgScore");
				    writer.append(',');
				    writer.append("NumUniqSoln");
				    writer.append(',');
				    writer.append("KnowDiff");
				    writer.append(',');
				}
			    //the average tie strength of the current social network
			    writer.append("Density");
			    writer.append(',');
			    writer.append("LCC");
			    writer.append(',');
			    writer.append("WLCC");
			    writer.append(',');
			    writer.append("WLCC2");
			    //writer.append(',');
			    //writer.append("SPL");
			    //writer.append(',');
			    //writer.append("NumOfCompo");
			    //writer.append(',');
			    //writer.append("MaxCompoRatio");
			    writer.append(',');
			    writer.append("Assortativity");
			    writer.append(',');
			    writer.append("OlpWgtCorr");
			    //writer.append(',');
			    //writer.append("KdiffOlpCorr");
			    //writer.append(',');
			    //writer.append("KdiffWgtCorr");
			    writer.append(',');
			    writer.append("DegSthCorr");
			    //writer.append(',');
			    //writer.append("DegScoreCorr");
			    //writer.append(',');
			    //writer.append("SthScoreCorr");
			    //writer.append(',');
			    //writer.append("DptScoreCorr");
			    writer.append(',');
			    writer.append("LCCRatio");
			    writer.newLine();
			}
			writer.append(Integer.toString(currentTick));
			writer.append(',');
			if(isSNet){
				calculateScores(memberList, orgSize);
				countUniqSoln(memberList, orgSize);
				writer.append(Double.toString(getMaxScore()));
				writer.append(',');
				writer.append(Double.toString(getAvgScore()));
				writer.append(',');
				writer.append(Integer.toString(getNumUniq()));
				writer.append(',');
				writer.append(Double.toString(getAvgKnowDiff(memberList, orgSize, N)));
				writer.append(',');
			}
			writer.append(String.valueOf(no.getNetworkDensity()));
			writer.append(',');
			writer.append(String.valueOf(no.calAvgClusterCoeff()));
			writer.append(',');
			writer.append(String.valueOf(no.calWeightedCCStrength()));
			writer.append(',');
			writer.append(String.valueOf(no.calWeightedCCStrength2()));			
			//no.findComponentsCalShortPath();//we need to get avgMaxCompoDeg and maxCompoSize for small world Q
			//writer.append(',');
			//writer.append(String.valueOf(no.getAvgShortestPath()));			
			//writer.append(',');
			//writer.append(String.valueOf(no.getCompoNum()));
			//writer.append(',');
			//writer.append(String.valueOf(no.getMaxCompoRatio()));
			writer.append(',');
			writer.append(String.valueOf(no.getAssortativity()));
			writer.append(',');
			no.calTiePropCorr();
			writer.append(String.valueOf(no.getOlpWgtCorr()));
			//writer.append(',');
			//writer.append(String.valueOf(no.getKdiffOlpCorr()));
			//writer.append(',');
			//writer.append(String.valueOf(no.getKdiffWgtCorr()));
			writer.append(',');
			no.calNodePropCorr();
			writer.append(String.valueOf(no.getDegStgCorr()));
			//writer.append(',');
			//writer.append(String.valueOf(no.getDegScoreCorr()));
			//writer.append(',');
			//writer.append(String.valueOf(no.getStgScoreCorr()));
			//writer.append(',');
			//writer.append(String.valueOf(no.getDptScoreCorr()));
			writer.append(',');
			writer.append(String.valueOf(no.maxCompSmallWorldQ()));
			writer.newLine();
			writer.flush();
			writer.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//A short version of outputProcessData to get an overview of the network topology at the end run
	public void outputProcessData(int problemID, int replicateID, double actDist, int orgSize, String outputMark){
		Context<Object> context = RunState.getInstance().getMasterContext();
		ContextJungNetwork<OrgMember> net;
		net = (ContextJungNetwork<OrgMember>) context.getProjection("socialNetwork");
		NetOperation no = new NetOperation(net, orgSize);
		String actType = "B";
		if(actDist > 8)
			actType = "C";//all active
		String	fileName = "C:/MData/DataSet" + problemID + "/SNet_act" + actType + "_" + outputMark + ".csv";
		try{
			File tResult = new File(fileName);
			boolean newFile = tResult.createNewFile();
			FileOutputStream file = new FileOutputStream(tResult, true);//the second parameter is true means appending
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(file));	
			if(newFile){//if the file does not exist
				writer.append("Run");
			    writer.append(',');
			    //the average tie strength of the current social network
			    writer.append("Density");
			    writer.append(',');
			    //writer.append("NumOfCompo");
			    //writer.append(',');
			    writer.append("MaxCompoRatio");
			    writer.append(',');
			    writer.append("LCC");
			    writer.append(',');
			    writer.append("WLCC");
			    writer.append(',');
			    writer.append("Assortativity");
			    writer.append(',');
			    writer.append("DegStgCorr");
			    writer.append(',');
			    writer.append("OlpWgtCorr");
			    writer.append(',');
			    writer.append("MaxCompSmallWorldQ");//must after the calculation of LCC
			    writer.newLine();
			}
			writer.append(Integer.toString(replicateID));
			writer.append(',');
			writer.append(String.valueOf(no.getNetworkDensity()));
			writer.append(',');
			boolean maxCompLargerThanTwo = no.findComponentsCalShortPath();//we need to get avgMaxCompoDeg and maxCompoSize for small world Q
			//writer.append(String.valueOf(no.getCompoNum()));
			//writer.append(',');
			writer.append(String.valueOf(no.getMaxCompoRatio()));
			writer.append(',');
			writer.append(String.valueOf(no.calAvgClusterCoeff()));
			writer.append(',');
			writer.append(String.valueOf(no.calWeightedCCStrength()));
			writer.append(',');
			writer.append(String.valueOf(no.getAssortativity()));
			writer.append(',');
			no.calNodePropCorr();
			writer.append(String.valueOf(no.getDegStgCorr()));
			writer.append(',');
			no.calTiePropCorr();
			writer.append(String.valueOf(no.getOlpWgtCorr()));
			writer.append(',');
			if(maxCompLargerThanTwo)
				writer.append(String.valueOf(no.maxCompSmallWorldQ()));
			else
				writer.append(String.valueOf(-1));
			writer.newLine();
			writer.flush();
			writer.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}

	private  double getAvgKnowDiff(ArrayList<OrgMember> memberList, int orgSize, int N) {
		double kdiff = 0.;
		for(int i = 0; i<orgSize; i++){
			int[] soln1 = memberList.get(i).getSolution().clone();
			for(int j= i+1; j<orgSize; j++){
				int[] soln2 = memberList.get(j).getSolution().clone();
				for(int k = 0; k < N; k++)
					if(soln1[k] != soln2[k]) kdiff++;
				//kdiff /= N;
			}
		}
		kdiff /= orgSize*(orgSize - 1)/2;
		return kdiff;
	}

	//map each agent's score through rank-preserving function, average them together	
	public void calculateScores(ArrayList<OrgMember> memberList, int pop) {
		double sum = 0.;
		double max = 0.;
        for(int i = 0; i < pop; i++) {
        	OrgMember a = (OrgMember) memberList.get(i);        	
        	double adjScore = adjustScore(a.getScore()); //calculate adjusted agent score, it's in [0,1]
        	if(adjScore > max) max = adjScore;
            sum += adjScore;	
        }
        avgScore = sum/pop;
        maxScore = max;
	}
	
	private  double adjustScore(double originScore){
		return Math.pow(originScore/NKSpace.getMax(), 8);
	}
	
	public void countUniqSoln(ArrayList<OrgMember> memberList, int orgSize) {
		ArrayList<Integer> uniqueList = new ArrayList<Integer>();
		for(int i = 0; i < orgSize; i++) {
			OrgMember a = (OrgMember) memberList.get(i);
			Integer soln = new Integer(Constants.binToInt(a.getSolution()));  //use Integer for object properties
	        if(!uniqueList.contains(soln)) uniqueList.add(soln);
		}
		if(uniqueList.size()==1){
			System.out.println("Only left one solution score.\n");
		}
		numUniq = uniqueList.size();
	}

	public  double getAvgScore() {
		return avgScore;
	}

	public  int getNumUniq() {
		return numUniq;
	}
	
	public  double getMaxScore(){
		return maxScore;
	}
}




