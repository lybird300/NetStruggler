package NetStruggler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;
import repast.simphony.context.Context;
import repast.simphony.context.space.graph.ContextJungNetwork;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunState;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.DistributionsAdapter;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.EdgeCreator;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.util.SimUtilities;

/**
 * This class constructs the context that includes all agents, their social network, and all model parameters (manipulative or not).
 * It also schedules the actions of individual agents (i.e., the execution of their step function) and finally, outputs simulation data.
 * @author Yuan Lin
 * @version OrgKTNet 1.0
 */
public class OrgBuilder implements ContextBuilder<Object> {
	private static ArrayList<OrgMember> memberList;
	public static HashMap<Integer, Integer> intervalDist = null;
	public static ArrayList<Double> avgScoreList = null;
	//public static ArrayList<Integer> uniSlnList = null;
	public static ArrayList<Integer> tickNetData = null; 
	private static char[] outputMark = null;
	private static int endRun = 1000;	
	private DataIO di;
	private int orgSize;
	private int space_n;
	private int space_k;
	private int problemID;
	private int designPt;
	private int nkID;
	//private int ptID;
	private int seed;
	private Param pm = null;
	private int actType;
	
	@SuppressWarnings("unchecked")
	@ScheduledMethod(start = 1, interval = 1, priority = 1)
	public void activateAgents() {
		Context<Object> context = RunState.getInstance().getMasterContext();
		ContextJungNetwork<OrgMember> orgSocialNetwork = (ContextJungNetwork<OrgMember>) context.getProjection("socialNetwork");
		//ContextJungNetwork<OrgMember> ktNetwork = (ContextJungNetwork<OrgMember>) context.getProjection("ktNetwork");
				
		OrgMember.updateMaxUnitWgt(Param.decayRate);//update currentTick as wells
		int currentTick = OrgMember.getCurrentTime();
		
		//int finalReplicate = 300;
		//int finalnkID = 199;
		
				
		//the following part handles incomplete files
		if(currentTick == 1){
			try{
			String act = "B";
			if(actType == 2)
				act = "C";//all active
			else if(actType == 3)
				act = "D";//normal distribution
			else if(actType == 0)
				act = "A";//none active
			String existingFile = "C:/MData/DataSet" + problemID + "/BatchAvgScore_act" + act + "_" + new String(outputMark) + ".csv";
			File result = new File(existingFile);
			boolean newFile = result.createNewFile();
			if(!newFile){
		    	boolean existing = false;
			   	String[] row = null;
			   	CSVReader reader = new CSVReader(new FileReader(existingFile), ',', '\'', 1);//skip the first line, which is the header 
			   	while((row = reader.readNext()) != null){
			   		if(Integer.parseInt(row[0]) == seed){
			   			existing = true;
			    		break;
			    	}
			   	}
			   	reader.close();
			   	if(existing) RunEnvironment.getInstance().endRun();
			}
		    else{//the file does not exist
			    	//if(newFile){
		    	FileOutputStream file = new FileOutputStream(result, true);//the second parameter is true means appending
		    	BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(file));
		    	//writer.append("nkSpace");
		    	//writer.append(',');
		    	writer.append("Run");
		    	writer.append(',');
		    	for(int i = 1; i<= endRun; i++){
		    		writer.append(Integer.toString(i));
		    		writer.append(',');
		    	}
		    	writer.newLine();
		    	writer.flush();//this line and the next is indispensable
		    	writer.close();
		    }
			} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
			
		
		
		//Even if other stochastic elements/processes in your program rely on an identical random seed,
		//the execution order of individual agents may still need to be "truly" randomized. This is one of the theoretical benefits of ABM.
		//block the preassigned random seed
		//if(Constants.switch_AssignRS) RandomHelper.setSeed((int)System.currentTimeMillis());
		SimUtilities.shuffle(memberList, RandomHelper.getUniform());
		//reactivate the preassigned random seed
		//if(Constants.switch_AssignRS) RandomHelper.setSeed(seed);
		for (OrgMember member : memberList){
			if(!member.isIdle())
				continue;
			if(RandomHelper.nextDouble() > member.getActivityRate()) member.setIntrType(0);
			else if(RandomHelper.nextDouble() < Param.randLink) member.setIntrType(2);
			else member.setIntrType(1);
			member.step();
		}
		
		//if(Constants.switch_AVG){
			di.calculateScores(memberList, orgSize);
			//di.countUniqSoln(memberList, orgSize);
			//AvgScore
			if(avgScoreList != null){
				if(currentTick == 1)
					avgScoreList.add(0, (Double)di.getAvgScore());
				else 
					avgScoreList.add(currentTick - 1, (Double)di.getAvgScore());
			}
			else System.out.println("Error: OrgBuilderL80\n");
			//MaxScore
			/*if(maxScoreList != null){
				if(currentTick == 1)
					maxScoreList.add(0, (Double)di.getMaxScore());
				else{
					int lastID = maxScoreList.size() - 1;
					if(maxScoreList.get(lastID) < 1)
						maxScoreList.add(lastID + 1, (Double)di.getMaxScore());
				}
			}
			else System.out.println("Error: OrgBuilderL88\n");*/
			//NumUniqSoln
			/*if(uniSlnList != null){
				if(currentTick == 1)
					uniSlnList.add(0, (Integer)di.getNumUniq());
				else 
					uniSlnList.add(currentTick - 1, (Integer)di.getNumUniq());
			}
			else System.out.println("Error: OrgBuilderL96\n");*/
		//}
		
		for (OrgMember member : memberList)
			member.setIdle(true);
		
		//social network and interaction histories are updated at every step
		sNetUpdate(orgSocialNetwork, currentTick);
		
		//for R analysis
		//di.outputAgentAttributes(Integer.toString(conditionID), memberList, currentTick, space_n);
		
		String filename = new String (outputMark);	
		//if(!Constants.switch_AVG)
			//di.outputProcessData(problemID, currentTick, true, memberList, orgSize, space_n, filename);
			
		//for test
		/*if(currentTick == 5){
			di.outputNodeAttributes(problemID, memberList, currentTick, space_n, filename);
			di.outputTieAttributes(problemID, currentTick, true, filename);//output sNet
		}*/
		
		//output net data for R analysis
		if(Param.actDist > 0 && tickNetData.contains((Integer)currentTick)){
			try {
				String act = "B";
				if(actType == 2)
					act = "C";//all active
				File folder = new File("C:/MData/DataSet" + problemID + "/net_act" + act + "_" + filename);
				if (!folder.exists())
					folder.mkdirs();
				//if(seed == finalReplicate && nkID == finalnkID)
					di.outputPajek("C:/MData/DataSet" + problemID + "/net_act" + act + "_" + filename, currentTick, seed, true);//output sNet
					//di.outputPajek(conditionID, currentTick, false);//output kNet (instant sNet)
			} catch (Exception e) {
				System.out.println("File output error.");
				e.printStackTrace();
			}
			//for Gephi analysis
			di.outputNodeAttributes(problemID, memberList, currentTick, space_n, filename);
			di.outputTieAttributes(problemID, currentTick, true, filename);//output sNet
		}
		
		if((1.-di.getAvgScore()) < Constants.smallValue || currentTick == endRun){
			//System.out.println("Simulation ends.");
			//if(!Constants.switch_AVG){
				//for Gephi analysis
				//di.outputNodeAttributes(problemID, memberList, currentTick, space_n, filename);
				//di.outputTieAttributes(problemID, currentTick, true, filename);//output sNet
				//di.outputTieAttributes(conditionID, currentTick, false, outputMark);//output kNet (instant sNet)
			//}
			//else{//filename is the design point ID
				//if(Param.actDist > 0){
					/*for(RepastEdge<OrgMember> e: orgSocialNetwork.getEdges())
						collectIntervalData((CustomTie)e);*/
					//di.outputIntervalFreq(problemID, seed, Param.actDist, intervalDist.entrySet(), filename);
					//di.outputProcessData(problemID, seed, Param.actDist, orgSize, filename);
					di.outputBatchAvgScore(problemID, nkID, avgScoreList, filename, seed, actType);
					//di.outputBatchMaxScore(problemID, maxScoreList, (int)(endRun/6), filename);
					//di.outputBatchUniSln(problemID, uniSlnList, endRun, filename, seed, Param.actDist);
					/*try {
						String actType = "B";
						if(Param.actDist > 9998)
							actType = "C";//all active
						File folder = new File("C:/MData/DataSet" + problemID + "/net_act" + actType + "_" + filename);
						if (!folder.exists()) folder.mkdirs();
						if(seed == finalReplicate && nkID == finalnkID)
							di.outputPajek("C:/MData/DataSet" + problemID + "/net_act" + actType + "_" + filename, currentTick, seed, true);//output sNet
						//di.outputPajek(conditionID, currentTick, false);//output kNet (instant sNet)
					} catch (Exception e) {
						System.out.println("File output error.");
						e.printStackTrace();
					}*/
				/*}
				else{
					di.outputBatchAvgScore(problemID, nkID, avgScoreList, String.valueOf(space_k), seed, actType);
					//di.outputBatchMaxScore(problemID, maxScoreList, (int)(endRun/6), filename);
					//di.outputBatchUniSln(problemID, uniSlnList, endRun, String.valueOf(space_k), seed, Param.actDist);
				}*/
			//}
			RunEnvironment.getInstance().endRun();
			//System.exit(0);//in non-batch mode this command helps to close the graphic interface.
		}
		
		//ktNetwork.removeEdges();
	}

	/**
	 * Construct the global context, set up global variables, and add agents to the context.
	 * initialize the tasks, expertise, and environments of individual agents. Build organizational expert index.
	 * Generate the initial topology of organizational social network.
	 * Repast S provides two network generators that create random and small-world networks separately.
	 */
	@Override
	public Context<Object> build(Context<Object> context) {
		context.setId("NetStruggler");
		Parameters params = RunEnvironment.getInstance().getParameters();
		
		seed = (Integer)params.getValue("randomSeed");
		//If the seeds are user specifiable, it's important to create the distributions in the begin() method rather than the method that includes step() method, 
		//which in your case is the activateAgent() method. Technically random seed only needs to set up once at the beginning.
		//Defining it in the parameters.xml, however, cannot affect function build(), which executes only at tick=0.
		//In order to use a preassigned random seed. You need to manually define it in build(). 
		if(Constants.switch_AssignRS) RandomHelper.setSeed(seed);
		
		/**
		 * the number of organizational members; size of the social network
		 */
		orgSize = (Integer) params.getValue("orgSize");
		space_n = (Integer) params.getValue("space_n");
		space_k = (Integer) params.getValue("space_k");
		/**
		 * n=20, k=5: problemID = 1 (DataSet1)
		 * n=20, k=1: problemID = 2 (DataSet2)
		 * n=10, k=5: problemID = 3 (DataSet3)
		 * n=10, k=1: problemID = 4 (DataSet4)
		 */
		problemID = (Integer) params.getValue("problemID");
		designPt = (Integer) params.getValue("designPt");
		//nkID = 116;
		nkID = (Integer) params.getValue("nkID");
		//ptID = (Integer) params.getValue("ptID");
		//nkID = RandomHelper.nextIntFromTo(0, 199);
		//ptID = RandomHelper.nextIntFromTo(0, 49);
		actType = (Integer)params.getValue("actType");
		
		switch(Constants.initNK){
			case 0: {//create new NK space and new points
				NK_gen space_gen = new NK_gen(space_n, space_k);
				space_gen.build_space();
				double maxScore = space_gen.write_to_file("C:/MData/DataSet" + problemID + "/nk_n" + space_n + "_k" + space_k);
				new NKSpace(space_n, space_k, maxScore, space_gen.getNK(), orgSize, problemID);
				space_gen = null;
				break;
			}
			case 1: {//load the NK space and create new points
				int temp_problemID = 100;
				new NKSpace("C:/MData/DataSet" + temp_problemID + "/nk_n" + space_n + "_k" + space_k + "_s116.nk", orgSize, temp_problemID);
				//break;
				System.out.println("Simulation ends.");
				RunEnvironment.getInstance().endRun();
				System.exit(0);
			}
			case 2: {//load the NK space and the points 
				//new NKSpace("C:/MData/DataSet" + problemID + "/nk_n" + space_n + "_k" + space_k + "_s" + nkID + ".nk",
						//"C:/MData/DataSet" + problemID + "/pt_n" + space_n + "_k" + space_k + "_s" + nkID + "_p" + ptID + ".pts");
				new NKSpace("N:/Temporary Folders/MData/nkSpace/nk_n" + space_n + "_k" + space_k + "_s" + nkID + ".nk",
						"N:/Temporary Folders/MData/nkSpace/pt_n" + space_n + "_k" + space_k + "_orgSize" + orgSize + ".pts");
				break;
			}
			case 3:{//generate a series of NK spaces and point sets
				NK_gen space_gen = null;
				for(int i = 0; i<200; i++){
					space_gen = new NK_gen(space_n, space_k);
					space_gen.build_space();
					space_gen.write_to_file("C:/MData/DataSet" + problemID + "/nk_n" + space_n + "_k" + space_k + "_s" + i);
					new NKSpace(space_n, space_k, orgSize, 50, problemID, i);
				}
				System.out.println("Simulation ends.");
				RunEnvironment.getInstance().endRun();
				System.exit(0);
			}
		}
		
		di = new DataIO(nkID);
		try {
			pm = di.importLHS("C:/MData/extreme_actC.csv", designPt);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/*if(!Constants.switch_AVG){
			outputMark = new char[4];
			if(Param.randLink < 0.1) outputMark[0] = '0';
			else if(Param.randLink < 0.5) outputMark[0] = '1';
			else outputMark[0] = '2';
			if(Param.decayRate > 0.9) outputMark[1] = '1';
			else outputMark[1] = '0';
			if(Param.wtGain > 1.9) outputMark[2] = '1';
			else outputMark[2] = '0';
			if(Param.actDist > 8) outputMark[3] = '9';
			else if(Param.actDist > 2) outputMark[3] = '3';
			else if(Param.actDist > 0) outputMark[3] = '1';
			else if(Param.actDist > -1) outputMark[3] = '0'; 
			else outputMark[3] = '8';//actDist = -1
		}
		else{*/
			int kdp = space_k*1000 + designPt;
			outputMark = ("" + kdp).toCharArray();
		//}
		
		//intervalDist = new HashMap<Integer, Integer>();
		//avgScoreList = new ArrayList<Double>();
		//maxScoreList = new ArrayList<Double>();
		//uniSlnList = new ArrayList<Integer>();
		if(Param.actDist > 0){
			tickNetData = new ArrayList<Integer>();//indicate the specific ticks when we need to collect network data (including tick = 10, and ticks from 51 to 350). The final collection happens when the execution ends.
			//int startTick = 1;
			//tickNetData.add(startTick);
			for(int i = 1; i < 30; i++)
				tickNetData.add(i);
			for(int i = 30; i < 60; i+=5)
				tickNetData.add(i);
			for(int i = 60; i < 100 ; i+=10)
				tickNetData.add(i);
			for(int i= 100; i < 400 ; i+=50)
				tickNetData.add(i);
			for(int i= 400; i < endRun; i+=100)
				tickNetData.add(i);
		}
				
		//build model with predefined points
		memberList = new ArrayList<OrgMember>();
		DistributionsAdapter da = new DistributionsAdapter(RandomHelper.getGenerator());
		//makeDefaultGenerator() constructs and returns a new uniform random number generation engine seeded with the current time. Currently this is MersenneTwister.
		//the random doubles being generated falls within the range of (0, 1)
		double activityRate = Param.actDist;//when actDist == -1, none active; when actDist == 9999, all active
		for(int i = 1; i <= orgSize; i++) {
				if(actType == 1)
					//activityRate = Math.pow((1-da.nextPowLaw(Param.actDist, Constants.activityDistRescale))/(Param.actDist + 1), 1/Param.actDist);
					activityRate = da.nextPowLaw(Param.actDist, Constants.activityDistRescale);
				//nextPowLaw(alpha, cut) returns cut*Math.pow(randomGenerator.raw(), 1.0/(alpha+1.0)), cut is set to be 1; when actDist = 0, it is a uniform distribution
				else if(actType == 3){
					do{
						activityRate = RandomHelper.createNormal(0.5, 0.5/3).nextDouble();
					}
					while(activityRate < 0 || activityRate > 1);
				}
				OrgMember om = new OrgMember(i, NKSpace.getPoints(i-1), activityRate, intervalDist);
				context.add(om);
				memberList.add(om);
		}
		OrgMember.initiateTime();
		OrgMember.TieWtSumArray = new double[orgSize];
		
		//for test
		//di.outputNodeAttributes(problemID, memberList, 0, space_n, new String(outputMark));
				
		//We use customized ties whose strength is a composition of interaction intensity and capacity.
		EdgeCreator<CustomTie, OrgMember> edgeCreator = new CustomTieCreator();
		NetworkBuilder<Object> sNetBuilder = new NetworkBuilder<Object>("socialNetwork", context, false);
		sNetBuilder.setEdgeCreator(edgeCreator);
		sNetBuilder.buildNetwork();
		OrgMember.sNet = (ContextJungNetwork<OrgMember>) context.getProjection("socialNetwork");
		//NetworkBuilder<Object> kNetBuilder = new NetworkBuilder<Object>("ktNetwork", context, false);
		//kNetBuilder.setEdgeCreator(edgeCreator); The ktNetwork will use the regular RepastEdge<OrgMember>
		//kNetBuilder.buildNetwork();
				
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		ScheduleParameters sParams = ScheduleParameters.createRepeating(1, 1, 1);
		schedule.schedule(sParams, this, "activateAgents");
				
		return context;
	}
	
    public void sNetUpdate(ContextJungNetwork<OrgMember> sNet, int currentTick){
    	//tieDecayRemove(sNet, kNet, currentTick);
    	ArrayList<RepastEdge<OrgMember>> tieCollection = new ArrayList<RepastEdge<OrgMember>>();
    	for(RepastEdge<OrgMember> currentTie : sNet.getEdges())
    		if(((CustomTie)currentTie).getLastUpdate()!=currentTick){
    		   	int s_index = Integer.parseInt(currentTie.getSource().getID()) - 1;
    	    	int i_index = Integer.parseInt(currentTie.getTarget().getID()) - 1;
    	    	double old_w = currentTie.getWeight();
    			double new_w = ((CustomTie) currentTie).wtUpdate(currentTick, Param.decayRate, Param.wtGain);
    			if(new_w < 0.1*Param.wtGain){
    				OrgMember.TieWtSumArray[s_index] -= old_w;
    				OrgMember.TieWtSumArray[i_index] -= old_w;
					tieCollection.add(currentTie);
    			}
    			else{
    				OrgMember.TieWtSumArray[s_index] += new_w - old_w;
    				OrgMember.TieWtSumArray[i_index] += new_w - old_w;
    			}
    		}
    		//else, the tie weight has been updated before knowledge exchange
    	for(RepastEdge<OrgMember> tie: tieCollection){
    		//if(!Constants.switch_AVG)
    		/*if(Param.actDist > 0)
    			collectIntervalData((CustomTie)tie);*/
    		sNet.removeEdge(tie);
    	}				
    }
    
   /* public void collectIntervalData(CustomTie e){
		int oldFreq;
    	List<List<Integer>> eventList = e.getEvents();
		for(int i = 0; i < eventList.size(); i++){
			int start = eventList.get(i).get(0).intValue();
			int end = eventList.get(i).get(1).intValue();
			e.submitSeqIntervalData(start, end, intervalDist);
			
			if(i + 1 == eventList.size()) break;
			//else
			int interval = eventList.get(i+1).get(0).intValue() - end;
			if(intervalDist.containsKey(interval))
				oldFreq = intervalDist.get(interval);
			else oldFreq = 0;
			intervalDist.put(interval, oldFreq + 1);
		}
    }*/ 
}
