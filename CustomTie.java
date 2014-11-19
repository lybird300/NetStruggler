package NetStruggler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import repast.simphony.space.graph.RepastEdge;

public class CustomTie extends RepastEdge<OrgMember> {
	private List<List<Integer>> events = new ArrayList<List<Integer>>();
	//each node of this list is a two-node list indicating a continuous time segment (the number at index 0/1 indicates the start/end step of this segment)  
	private int useFreq = 0;
	/**
	 * used in wtUpdate to decide when we can stop including more events into weight calculation
	 * the purpose is to save time
	 */
	private int stopPoint = 0;
	/**
	 * used for continuous tie weight update (given a tie is being consecutively used)
	 */
	private double latestSeg;
	
	public CustomTie(OrgMember source, OrgMember target, boolean directed, double weight) {
		super(source, target, directed, weight);
	}
	public CustomTie(OrgMember source, OrgMember target, boolean directed) {
		super(source, target, directed);
	}
	public int getLastUpdate(){
		if(events.size()==0) return 0;
		else return events.get(events.size()-1).get(1).intValue();
	}
	public int getAge(int currentTick){
		return currentTick + 1 - events.get(0).get(0).intValue();
	}
	public int getUseFrequency(){
		return useFreq;
	}
	/**
	 * @return the measure of knowledge similarity between the source and the recepient
	 */
	public double getSimilarity(){
		double count = 0.;
		int[] src_sln = this.source.getSolution().clone();
		int[] rep_sln = this.target.getSolution().clone();
		for(int i = 0; i< src_sln.length; i++){
			if(src_sln[i] != rep_sln[i]) count++;
		}
		return count/src_sln.length;
	}
	/*public void addEvent(int currentTick){
		useFreq++;
		if(currentTick > 1 && getLastUpdate()==currentTick-1){
			List<Integer> seg = events.get(events.size()-1);
			seg.set(1, new Integer(currentTick));
			latestSeg += 1.0/(currentTick + 1 - seg.get(0).intValue());
		}
		else{ //getLastUpdate()<currentTick; at least decayed once
			ArrayList<Integer> newEvents = new ArrayList<Integer>();
			for(int i = 1; i<=2; i++)
				newEvents.add(new Integer(currentTick));
			events.add(newEvents);
			latestSeg = 1;
		}	
	}*/
	public void addEvent(int currentTick){
		useFreq++;
		if(currentTick > 1 && getLastUpdate()==currentTick-1)
			events.get(events.size()-1).set(1, new Integer(currentTick));
		else{ //getLastUpdate()<currentTick; at least decayed once
			ArrayList<Integer> newEvents = new ArrayList<Integer>();
			for(int i = 1; i<=2; i++)
				newEvents.add(new Integer(currentTick));
			events.add(newEvents);
		}	
	}
	public double wtUpdate(int currentTick, double decayPower, double reinforceValue){
		//double harmonicEstimate = (decayPower < 1)? ((Math.pow(useFreq, 1-decayPower)-1)/(1-decayPower)+1):(Math.log(useFreq)+1);
		//if(harmonicEstimate < Constants.smallValue){
		double newWeight = 0;
		int visited = 0;
		int origin = currentTick + 1 - events.get(0).get(0).intValue();
		for(int k = events.size()-1; k >= stopPoint; k--){
			//the start of the current segment
			int start = currentTick + 1 - events.get(k).get(0).intValue();
			//the end of the current segment (using currentTick minus, so start is larger than end)
			int end = currentTick + 1 - events.get(k).get(1).intValue();
			int originEst = end + useFreq - visited - 1;
			double tailUpperEst = (decayPower < 1)? ((Math.pow(originEst, 1-decayPower) - Math.pow(end, 1-decayPower))/(1-decayPower)+
					Math.pow(origin, -1*decayPower) - 1):(Math.log(origin)+1.0/origin-Math.log(end)-1);
			//the total number of steps in the current segment
			visited += start - end + 1;
			if(tailUpperEst >= 0.1){//then tailUpperEst*reinforceValue >= 0.1*reinforceValue
				for(int t = start; t >= end; t--)
					newWeight += Math.pow(t, -1*decayPower);
				//double wtAdd_lower =  (decayPower < 1)? ((Math.pow(origin, 1-decayPower) - Math.pow(end, 1-decayPower))/(1-decayPower)+
						//1-Math.pow(end, -1*decayPower)):(Math.log(origin)+1-Math.log(end)-1/end);
				//newWeight += wtAdd_lower;		
			}
			else{
				stopPoint = k+1;
				break;
			}
		}
		newWeight *= reinforceValue;
		super.setWeight(newWeight);
		return newWeight;
	}
	
	//make sure there is always a addEvent() before this function
	public double wtContinuousUpdate(int currentTick, double decayPower, double reinforceValue){
		double newWeight = latestSeg;
		int visited = 1;
		if(latestSeg > 1)
			visited = events.get(events.size()-1).get(1).intValue() - events.get(events.size()-1).get(0).intValue() + 1;
		int origin = currentTick + 1 - events.get(0).get(0).intValue();
		for(int k = events.size()-2; k >= stopPoint; k--){
			//the start of the current segment
			int start = currentTick + 1 - events.get(k).get(0).intValue();
			//the end of the current segment (using currentTick minus, so start is larger than end)
			int end = currentTick + 1 - events.get(k).get(1).intValue();
			int originEst = end + useFreq - visited - 1;
			double tailUpperEst = (decayPower < 1)? ((Math.pow(originEst, 1-decayPower) - Math.pow(end, 1-decayPower))/(1-decayPower)+
					Math.pow(origin, -1*decayPower) - 1):(Math.log(origin)+1.0/origin-Math.log(end)-1);
			//the total number of steps in the current segment
			visited += start - end + 1;
			if(tailUpperEst >= 0.1){//then tailUpperEst*reinforceValue >= 0.1*reinforceValue
				for(int t = start; t >= end; t--)
					newWeight += Math.pow(t, -1*decayPower);
				//double wtAdd_lower =  (decayPower < 1)? ((Math.pow(origin, 1-decayPower) - Math.pow(end, 1-decayPower))/(1-decayPower)+
						//1-Math.pow(end, -1*decayPower)):(Math.log(origin)+1-Math.log(end)-1/end);
				//newWeight += wtAdd_lower;		
			}
			else{
				stopPoint = k+1;
				break;
			}
		}
		newWeight *= reinforceValue;
		super.setWeight(newWeight);
		return newWeight;
	}
	/*public double wtUpdate(int currentTick, double decayPower, double reinforceValue){
		//double harmonicEstimate = (decayPower < 1)? ((Math.pow(useFreq, 1-decayPower)-1)/(1-decayPower)+1):(Math.log(useFreq)+1);
		//if(harmonicEstimate < Constants.smallValue){
		double newWeight = 0;
		int visited = 0;
		for(int k = events.size()-1; k >= stopPoint; k--){
			int end_t = events.get(k).get(1).intValue();
			double tailUpperEst = 0;
			//The following for statement is too computational expensive
			for(int j = 0; j < useFreq - visited; j++)
				tailUpperEst += Math.pow(currentTick + 1 - end_t + j, -1*decayPower);
			if(tailUpperEst >= 0.1){//then tailUpperEst*reinforceValue >= 0.1*reinforceValue
				int start_t = events.get(k).get(0).intValue();			
				for(int i = start_t; i <= end_t; i++){
					newWeight += Math.pow(currentTick + 1 - i, -1*decayPower);
					visited++;
				}
			}
			else{
				stopPoint = k+1;
				break;
			}
		}
		//the following codes have some unknown problem, so I decided to abandon them
		//Basically leave sub-lists where they are for now and delete them when the tie needs to be removed
		/*if(events.size()==1 && k==1)
			System.out.println("stop");
		for(int j = 0; j <= k && events.size() > 0; j++){
			if(j==events.size())
				System.out.println("stop");
			int start = events.get(0).get(0).intValue();
			int end = events.get(0).get(1).intValue();
			submitSeqIntervalData(start, end, dist);
			events.remove(events.get(0));
		}*/
		/*newWeight *= reinforceValue;
		super.setWeight(newWeight);
		return newWeight;
	}*/
	public List<List<Integer>> getEvents(){
		/*ArrayList<Integer> records = new ArrayList<Integer>();
		for(List<Integer> elist: events)
			records.addAll(elist);
		return records;*/
		return events;
	}
	
	public void submitSeqIntervalData(int start, int end, Map<Integer, Integer> intervalDist) {
	    if(start == end) return;
	    else{
	    	int oldFreq;
			if(intervalDist.containsKey(1)) oldFreq = intervalDist.get(1).intValue();
			else oldFreq = 0;
			intervalDist.put(1, end - start + oldFreq);
		}
	}
}
