package NetStruggler;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;

public class Param {
	public static double randLink;
	public static double decayRate;
	public static double wtGain;
	public static double actDist;
	public static double learnErr;
	public static double innoRange;
	public Param(String[] record, int index){
		int dpt = Integer.parseInt(record[0]);
		if(dpt != index){
			System.out.println("Param.javaL11: Not the right design point!\n");
			System.exit(0);
		}
		Parameters params = RunEnvironment.getInstance().getParameters();
		int actType = (Integer)params.getValue("actType");
		if(actType==0)
			actDist = -1;
		if(actType==1)
			actDist = Double.parseDouble(record[1]);
		if(actType == 2 || actType == 3)
			actDist = 9999;
		randLink = Double.parseDouble(record[2]);
		wtGain = Double.parseDouble(record[3]);
		decayRate =  Double.parseDouble(record[4]);
		learnErr =  Double.parseDouble(record[5]);
		innoRange =  Double.parseDouble(record[6]);	
	}
}
