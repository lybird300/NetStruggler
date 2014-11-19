package NetStruggler;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import repast.simphony.random.RandomHelper;

/**
 * The agent object controls agent local agent behavior in the simulation.
 * Each agent maintains a list of its network ties and its current location in the problem space.
 * A set of flags govern its behavior. The agent refers to the global NKSpace object to determine score.
 * 
 * The model calls all agents to act in the simulation iteratively, but with the appearance of a 
 * simultaneous decision. Thus, agents act put the consequences of these actions are enacted only
 * with the update() function, which applies the new solutions, scores, etc.
 * 
 * The meat of the agent's behavior occurs in the decision function, which determines which action to take.
 * 
 * This code was originally designed for
 * (Lazer, David and Allan Friedman. "The Network Structure of Exploration and Exploitation." Administrative Science Quarterly, 52:4. 2007)
 * Licensed under Creative Commons Attribution-Share Alike CC(2007)
 * http://creativecommons.org/licenses/by-nc-sa/3.0/us/
 */
public class NKSpace {
	private static int n = 0;
	private static int k = 0;
	private static double max = 0.;
	private static double[] scores = null;
	private static int[][] points = null;

	public NKSpace(int space_n, int space_k, double space_max, double[] space_scores, int orgSize, int conditionID){
		n = space_n;
		k = space_k;
		max = space_max;
		scores = space_scores;
		makePoints(orgSize, "C:/MData/DataSet" + String.valueOf(conditionID) + "/pt_n" + Integer.toString(n) + "_k" + Integer.toString(k));
	}
	
	public NKSpace(int space_n, int space_k, int orgSize, int numOfPointSet, int conditionID, int spaceID){
		n = space_n;
		k = space_k;
		for(int i = 0; i<numOfPointSet; i++)
			makePoints(orgSize, "C:/MData/DataSet" + conditionID + "/pt_n" + n + "_k" + k + "_s" + spaceID + "_p" + i);
	}
	
	//Loads the space from file
	//called with a filename and possible set of points to create an object to keep the space local
	public NKSpace(String file, String pts) {
		//load score space
		try {
			DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
			n = in.readInt(); //get n from the NK file  DEBUG - remove these three lines while copying old files
			k = in.readInt(); //get k from the NK file

			scores = new double[(int)Math.pow(2, n)];

			for(int i=0; i < (int)Math.pow(2, n); i++) {
				scores[i] = in.readDouble();  //import the entire file
			}
			
			max = in.readDouble();
			in.close();
		}
		catch (IOException e) { System.out.println (" IOexception =" + e );}
		loadPoints(pts);
		
	}
	
	//Loads the space from  file
	//called with a filename and possible set of points to create an object to keep the space local
	public NKSpace(String file, int orgSize, int conditionID) {
		//load score space
		try {
			DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
			n = in.readInt(); //get n from the NK file  DEBUG - remove these three lines while copying old files
			k = in.readInt(); //get k from the NK file

			scores = new double[(int)Math.pow(2, n)];

			for(int i=0; i < (int)Math.pow(2, n); i++) {
				scores[i] = in.readDouble();  //import the entire file
			}
			
			max = in.readDouble();
			in.close();
		}
		catch (IOException e) { System.out.println (" IOexception =" + e );}
		System.out.println("Using randomized starting points");
		makePoints(orgSize, "C:/MData/DataSet" + String.valueOf(conditionID) + "/pt_n" + Integer.toString(n) + "_k" + Integer.toString(k) + "_orgSize" + Integer.toString(orgSize));
	}
	
	public static int getN() {
		return n;
	}
	public static int getK(){
		return k;
	}
	
	//loads a pre-specified pts file into the NKSpace object
	private static void loadPoints(String pointsFile) {
		try {
			DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(pointsFile)));
			int pop = in.readInt(); //get pop from the points file
			int fileN = in.readInt(); //get n from the points file
			if(fileN != n) System.out.println("ERROR - bad points file for NK space");
			points = new int[pop][n];
			for(int i=0; i < pop; i++) {
				for(int j = 0; j < n; j++) {
					points[i][j] = in.readInt();  //import the entire file
				}
			}
			in.close();
		}
		catch (IOException e) { System.out.println (" IOexception =" + e );}
	}
	
    //prints a set of random n-dimensional starting points to a file
    /* FORMAT *.pts:
    * NOTE that it is not human-readable
    * Datum 1: pop  --should be read to determine how long the datastruct is to be
    * Datum 2: n  --double check the appropriate nk
    * Data 3-pop*n  -- the NK file
    */
	private static void makePoints(int pop, String title) {
		points = new int[pop][n];
    	title = (title+".pts");
    	try{
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(title)));
            out.writeInt(pop); //each file records the number of points and n of the space
            out.writeInt(n);
            for(int i=0; i < pop; i++)
            	for(int j=0; j<n; j++){ //how many total bits are needed
            		points[i][j] = (int) (RandomHelper.nextDouble() * 2);
            		out.writeInt(points[i][j]);
            	}
            out.close();
        }
        catch (IOException ioe){
            System.out.println("IO Exception");
        }
	}
	
	//returns a specific line from the pre-loaded points file for agent #a
	public static int[] getPoints(int a){
		return points[a];
	}
	
	//public lookup function to get the score for an agent
	public static double getScore(int point) {
		return scores[point];
	}
	
	//find the highest score in a space for scoring reasons
	public static double getMax() {
		return max;
    }
	
	
	
	/*********     HELPER TOOLS      ********************/
    //Takes a number, returns an array representing its binary value
    //NB - only works for n < 32

    public int[] intToBin(int num) {
        int[] bin = new int[n];
        for(int i = n-1; i >= 0; i--) {  //start with the highest bit
            if(((1 << i) & num) != 0) { //bitshift 1 over and compare with the power of 2
                bin[n-1-i] = 1;			//write things right to left
            }
            else {
                bin[n-1-i] = 0;
            }
        }
        return bin;
    }
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
}

