package NetStruggler;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import repast.simphony.context.Context;
import repast.simphony.context.space.graph.ContextJungNetwork;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunState;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.util.SimUtilities;


/**
 * Each instance of this class is an agent who represents an organizational member.
 * The class constructs, assigns tasks to, and initialize the expertise of an agent.
 * It handles the process in which one single agent pursues the max level of knowledge in every task-required area by acquiring knowledge from others.
 * It also reduces unused knowledge (knowledge obsolescence).
 * 
 * @author Yuan Lin
 * @version OrgKTNet 1.0
 */
public class OrgMember implements Comparable<Object> {
	public static ContextJungNetwork<OrgMember> sNet;
	public static int currentTick;
	public static double maxUnitTieWeight;
	public static Map<Integer, Integer> intervalDist = null;
	public static double[] TieWtSumArray = null;
	/**
	 * start from "1"
	 */
	protected String myID = null;
	/**
	 * Only temporarily used when calculate local interaction priority; in order to avoid repeated calculation
	 */
	public double localConstraint = 0;
	/**
	 * the current solution occupied by the agent in the problem space.
	 * soln is kept as a binary string except when a lookup is required.
	 */
	int[] soln;
	/**
	 * the score of the current solution, corresponding to the agents' soln.
	 */
	private double cur_score;
	/**
	 * placeholder used to handle the iterative nature of the simulation.
	 */
	int[] new_soln;
	/**
	 * placeholder used to handle the iterative nature of the simulation.
	 */
	private double new_score;
	/**
	 * the probability that this member is active for interaction per time step
	 */
	private double activityRate;
	/**
	 * Depend on the member's activity rate and other members' preference (i.e., whether they are likely to meet the focal member)
	 */
	int localInterTime;
	int totalInterTime;
	/**
	 * indicate whether the agent is available for KT interaction
	 */
	public boolean idle = true;
	/**
	 * indicate whether the agent is self-exploring (0) or local linking (1) or random linking (2)
	 */
	public int interaction_type;
	
	public static void initiateTime(){
		currentTick = 0;
	}
	
	public static void updateMaxUnitWgt (double decayPower){
		currentTick++;
		double newMaxWgt = 0;
		for(int i = 1; i <= currentTick; i++)
			newMaxWgt += Math.pow(currentTick + 1 - i, -1*decayPower);
		maxUnitTieWeight = newMaxWgt;
	}
	
	public static int getCurrentTime(){
		return currentTick;
	}
	
	public OrgMember(int memberID, int[] pts, double actRate, Map<Integer, Integer> dist) {
		myID = Integer.toString(memberID);
        soln = pts;
        new_soln = soln.clone();
        cur_score = NKSpace.getScore(binToInt(pts)); //set the initial score
        new_score = cur_score;
        localInterTime = 0;
        totalInterTime = 0;
        //globalInterTime = 0;
		//Individual activity rate follows a power-law distribution
        //Ref: (a) http://stackoverflow.com/questions/918736/random-number-generator-that-produces-a-power-law-distribution (b) http://mathworld.wolfram.com/RandomNumber.html
        //when activityDistPower = 0, activityRate = activityDistRescale*((1-activity_lb)*Y + activity_lb) where y is a uniformly distributed variable in [0,1]
        /*activityRate = Constants.activityDistRescale * Math.pow((1 - Math.pow(Constants.activity_lb, activityDistPower + 1))*
				RandomHelper.nextDouble() + Math.pow(Constants.activity_lb, activityDistPower + 1), 1/(activityDistPower + 1));*/
        activityRate = actRate;
        /*if(activityRate > 1)
        	System.out.println("OrgMember.java/Line90: Activity rate.\n");*/
        OrgMember.intervalDist = dist;
	}
	public void setIntrType(int intrType){
		interaction_type = intrType;
	}
	public double getActivityRate(){
		return activityRate;
	}
	/**
	 * @return this agent's ID
	 */
	public String getID() {
		return myID;
	}
	
    public void setScore(double score) {
    	cur_score = score;
    }
    
    public double getScore() {
    	return cur_score;
    }
    
    public int[] getSolution() {
    	return soln;
    }
    
    
    /**
     * set up fixed starting points
     * @param pts
     */
    /*public void setupStartingPt(int[] pts) {
        soln = pts;
        new_soln = soln.clone();
    }*/
    
    /**
     * Random Starting points
     * @param the length of solution string
     */
    /*public void setupStartingPt(int n) {
        soln = new int[n];
        randomSoln();
        new_soln = soln.clone();
    }*/

    /**
     * fills the soln with a random binary string
     */
    /*public void randomSoln() {
        for(int i = 0; i < soln.length; i++) {
            soln[i] = (int) (RandomHelper.nextDouble() * 2);
        }
    }*/

    /**
     * Allows other agents to access the soln of this agent.
     * @return the solution
     */
    public int getPoint() {
    	return binToInt(soln);
    }
    
    /**
     * converts the binary-string solution to a long for easy score lookup.
     * @param the binary-string solution
     * @return
     */
    public int binToInt(int[] bin) {
        int t = bin.length;		//should be k+1
        int num = 0;
        int coef = 0;
        for(int i = 0; i < t; i++ ) {
            coef = (int)Math.pow(2, (t-i-1));
            num += bin[i] * coef;
        }
        return num;
    }
 
    /**
     * After every agent has acted, the effects of these actions are made permanent.
     * update() applies to the agent's solution and score ONLY if the new soln is an improvement
     */
    public void solnUpdate() {
 		double testScore = NKSpace.getScore(binToInt(new_soln)); //find experimental score
 		if(testScore > cur_score){
 			new_score = testScore; 
 			soln = new_soln.clone();
 			cur_score = new_score;
 		}
 		else{//go back to prior solution
    		new_soln = soln.clone();
    		new_score = cur_score;
 		}
    }
    
    public boolean isIdle(){
    	return idle;
    }
    
    public void setIdle(boolean state){
    	idle = state;
    }
	public int getIntrType() {
		return this.interaction_type;
	}
    
	
    /**
     * Both learn from the difference, even if one's current score is higher than the other's
     * If the error is greater than zero, use it as the probability of dissimilar bits being miscopied.
     * NOTE - error is only introduced for dissimilar bits. So the more similar two solution are, the more accurate the imitation will be (resemble the advantage of strong ties) 
     * @param target
     * @param space
     * @param time
     */
    public void knowledgeExchange(OrgMember supPerformer, OrgMember infPerformer){
    	//Parameters params = RunEnvironment.getInstance().getParameters();
    	/**
    	 * This parameter predefines the probability of error when the recipient estimates each dimension of the source's solution.
    	 */
    	double err = Param.learnErr;
    	double reinforce = Param.wtGain;
    	double decayPower = Param.decayRate;
    	
		supPerformer.setIdle(false);
		infPerformer.setIdle(false);
		int solnLength = this.soln.length;
    	double bandWidth = 0.;
    	
    	CustomTie sTie = (CustomTie) sNet.getEdge(supPerformer, infPerformer);
    	int s_index = Integer.parseInt(supPerformer.getID()) - 1;
    	int i_index = Integer.parseInt(infPerformer.getID()) - 1;
    	double wtAdd = reinforce;//the new weight should be at least 1*reinforceValue
    	if(sTie == null){
    		sTie = (CustomTie)sNet.addEdge(supPerformer, infPerformer, reinforce);
    		sTie.addEvent(currentTick);
    	}
   		else{
   			double oldWeight = sTie.getWeight();
   			sTie.addEvent(currentTick);
   			wtAdd = sTie.wtUpdate(currentTick, decayPower, reinforce) - oldWeight;
   		}
    	TieWtSumArray[s_index] += wtAdd;
    	TieWtSumArray[i_index] += wtAdd;
    	//kNet.addEdge(supPerformer, infPerformer, sTie.getWeight());
    	//I use the following formula to calculate bandWidth instead of sTie.getFrequency()/currentTick because the following formula
    	//gives more weight to more recent interaction history
    	bandWidth = sTie.getWeight()/(OrgMember.maxUnitTieWeight*reinforce);
    	if(bandWidth > 1){
    		System.out.println("Bandwidth = " + bandWidth + "\n");
    		bandWidth = 1;
    	}
    	int numTransferArea = (int)Math.ceil(solnLength*bandWidth);
    	if(numTransferArea <= 0)
    		return;
    	
    	ArrayList<Integer> list = new ArrayList<Integer>();
    	for(int j = 0; j < solnLength; j++)
    		list.add(j);
    	SimUtilities.shuffle(list, RandomHelper.getUniform());
    	List<Integer> randList = list.subList(0, numTransferArea);
    	
    	//first let superior performer explore and see if it can find better solution based on the interaction with inferior performer
    	//then inferior performer learn from the update solution of superior performer
    	for(int j = 0; j < solnLength; j++){
    		if(!randList.contains(j)) supPerformer.new_soln[j] = supPerformer.soln[j];
    		else if(supPerformer.soln[j] != infPerformer.soln[j]){
    			if(RandomHelper.nextDouble() > err) supPerformer.new_soln[j] = infPerformer.soln[j];
    		}
    	}
    	supPerformer.solnUpdate(); 
    	
    	for(int j = 0; j < solnLength; j++){
    		if(!randList.contains(j)) infPerformer.new_soln[j] = infPerformer.soln[j];
    		else if(supPerformer.soln[j] != infPerformer.soln[j]){
    			if(RandomHelper.nextDouble() > err) infPerformer.new_soln[j] = supPerformer.soln[j];
    		}
    	}
    	infPerformer.solnUpdate();
    	
    }
	
    public void selfLearn() {
    	//Parameters params = RunEnvironment.getInstance().getParameters();
    	double innov = Param.innoRange;
    	//System.out.println("Innovation: currentTick=" + currentTick + "\n");
    	new_soln = soln.clone(); //make a clean copy
    	for(int i = 0; i < Math.ceil(innov); i++) {
    		int bit = (int) (RandomHelper.nextDouble() * soln.length); //select a bit for experiment
    		new_soln[bit] = (soln[bit] +1) % 2; //experiment by shifting the bit
    	}
       	solnUpdate();
    }
    
	
	public boolean globalInteract() {
		//Context<Object> context = RunState.getInstance().getMasterContext();
		//ContextJungNetwork<OrgMember> sNet = (ContextJungNetwork<OrgMember>) context.getProjection("socialNetwork");
		//ContextJungNetwork<OrgMember> kNet = (ContextJungNetwork<OrgMember>) context.getProjection("ktNetwork");
		
		int srcID = RandomHelper.nextIntFromTo(1, sNet.size());		
		while(srcID == Integer.parseInt(this.getID())){//avoid self-connection
			srcID = RandomHelper.nextIntFromTo(1, sNet.size());
		}
		
		OrgMember src = null;
		Iterable<OrgMember> members = RunState.getInstance().getMasterContext().getObjects(OrgMember.class);
		for(OrgMember member: members){
			if(Integer.parseInt(member.getID()) == srcID)
				src = member;
		}
		//if the other agent is not available (at a non-interaction mode or interact with another agent), return false
		if(!src.isIdle() || src.getIntrType()==0) return false;
		if(this.getScore() >= src.getScore()) knowledgeExchange(this, src);
		else knowledgeExchange(src, this);
		this.addInteractionTime();
		src.addInteractionTime();
		return true;
	}
	
	private void addInteractionTime() {
		if(this.interaction_type == 1) this.localInterTime++;
		//if(this.interaction_type == 2) this.globalInterTime++;
		this.totalInterTime++;
	}

	public boolean localInteract() {
		//Context<Object> context = RunState.getInstance().getMasterContext();
		//ContextJungNetwork<OrgMember> sNet = (ContextJungNetwork<OrgMember>) context.getProjection("socialNetwork");
		//ContextJungNetwork<OrgMember> kNet = (ContextJungNetwork<OrgMember>) context.getProjection("ktNetwork");
		NetOperation no = new NetOperation(sNet, sNet.size());
		
		TreeSet<OrgMember> neighbors = new TreeSet<OrgMember>();//Set does not add duplicate items.
		for(OrgMember directN : sNet.getAdjacent(this)){
			directN.setLC(no.localConstraint(this, directN));
			neighbors.add(directN);
			for(OrgMember indirectN : sNet.getAdjacent(directN)){
				if(!indirectN.getID().equals(this.getID()) && !sNet.isAdjacent(this, indirectN)){
					if(!neighbors.contains(indirectN)){
						indirectN.setLC(no.localConstraint(this, indirectN));
						neighbors.add(indirectN);
					}
				}
			}
		}
		//The next step is very important. Otherwise the different orders of the "neighbors" hashset will influence the order and the folloiwng shuffle of the "potentSrc" arraylist 
		//List<OrgMember> potentSrc = new ArrayList<OrgMember>(neighbors);
		/*System.out.println("A new start:\n");
		if(potentSrc.size() >= 3){
			for(OrgMember om : potentSrc)
				System.out.println("ID=" + om.getID() + ", lc=" + om.getLC() + "\n");
		}*/
		//Collections.sort(potentSrc, new CustomComparator());
		/*System.out.println("Now it becomes:\n");
		if(potentSrc.size() >= 3){
			for(OrgMember om : potentSrc)
				System.out.println("ID=" + om.getID() + ", lc=" + om.getLC() + "\n");
		}*/
		//OrgMember target = potentSrc.get(potentSrc.size()-1);
		OrgMember target = neighbors.last();
		if(target==null){
			System.out.println("OrgMemberL271: target null!\n");
			return false;
		}
		//if the other agent is not available (at a non-interaction mode or interact with another agent), return false
		if(!target.isIdle() || target.getIntrType()==0) return false;
		if(this.getScore() >= target.getScore()) knowledgeExchange(this, target);
		else knowledgeExchange(target, this);
		this.addInteractionTime();
		target.addInteractionTime();
		return true;	
	}
	
	
	/**
	 * At every step an agent will follow the same procedure described in this method. 
	 * First determine whether the agent will copy or explore. If copying, the agent will always copy its best neighbor,
	 * providing that there is a network tie to another agent with a better solution.
	 * @param currentTick 
	 * @param intrType 
	 */
	@SuppressWarnings("unchecked")
	public void step() {
		Context<Object> context = RunState.getInstance().getMasterContext();
		ContextJungNetwork<OrgMember> sNet = (ContextJungNetwork<OrgMember>) context.getProjection("socialNetwork");
		//Only when the current agent is idle will it executes local or global interaction.
		//A side effect is that there won't be intentional reciprocity at the same time step, although technically the interaction is reciprocal.
		if(!this.isIdle()) return;
		//boolean success = false;
		switch(this.getIntrType()){
			case 0: selfLearn();
					break;
			case 1: if(sNet.getDegree(this) > 0){
						localInteract();
						//success = localInteract();
						break;
					}
					//else it continues to the next case
			case 2: globalInteract();
					//success = globalInteract();
					break;
		}
		//if(success) interactionTime ++;
		//else if (this.getIntrType() != 0) selfLearn();
	}
	
	public double getInteractionTime(int type){
		if(type == 1){
			if(totalInterTime == 0) return 0.;
			else return (double)localInterTime/(double)totalInterTime;
		}
		else if(type == 2) return totalInterTime/currentTick;
		else return -1;
	}
	
	public double getLC(){
		return localConstraint;
	}
	
	public void setLC(double lc){
		localConstraint = lc;
	}

	@Override
	public int compareTo(Object obj) {
		OrgMember om = (OrgMember) obj;
		double lc1 = this.getLC();
	    double lc2 = om.getLC();
	    if(lc1 < lc2) return -1;
	    else if (lc1 > lc2) return 1;
	    else return 0;
	}
}
