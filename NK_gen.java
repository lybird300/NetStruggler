package NetStruggler;


import java.io.*;

import repast.simphony.random.RandomHelper;

/**
 * This code generates an NK space (Kauffman 1992) in the form of an 2^n string for
 * an n-dimensional NK space. This revised version can create NK spaces up to 20 bits long.
 * Includes a function to print score-space to a (non-ascii) file.
 * 
 * build_space() creates a space in the calling function's memory space, and write-to-file()
 * writes the generated space to disk. make-points() generates a prescribed set of initial points.
 
 * Given the size of large NK spaces, it is recommended that NK spaces be built before the 
 * running the main model.
 * 
 * An NK space works like this: each point in an n-length bit string has k dependency links. The 
 * score of each link depends on k other bits, so there are 2^K possible scores for each bit. 
 * The score of an NK string is the normalized sum of each bit contribution. More more information, 
 * see Appendix 1 of Lazer and Friedman, 2007.
 *
 * This code was originally designed for
 * (Lazer, David and Allan Friedman. "The Network Structure of Exploration and Exploitation." Administrative Science Quarterly, 52:4. 2007)
 * Licensed under Creative Commons Attribution-Share Alike CC(2007)
 * http://creativecommons.org/licenses/by-nc-sa/3.0/us/
 */
public class NK_gen {
    double[] nk;		//the score list, a very long array of size 2^n
    int n;      //model param - bit length
    int k;      //model param - epistatis or ruggedness of landscape. 0 = single peak

    public NK_gen(int n_, int k_) {
        n = n_;
        k = k_;
        nk = new double[(int) Math.pow(2, n)];  //2^n scores in an n space
    }
    
    
    // THE MAIN FUNCTION
    /* 1) Create the matrices of random links between each gene (genes correspond to your solution elements, i.e., knowledge areas; it is about the independence among dimensions and has nothing to do with the social network)
     * 2) Create a score for each possible combination of nodes 
     * 3) Walk the entire space, filling in the appropriate score
     */
    public void build_space(){
        int size = nk.length;
        int numscores = (int) Math.pow(2, k + 1);
        int[][] links = new int[n][k + 1]; //Links between the genes, including the gene itself
        double[][] scores = new double[n][numscores]; //each possible mask combo gets a score

        //fill the links
        for (int i = 0; i < n; i++) {
            links[i][0] = i; //the first one is the gene itself
            for (int j = 1; j < k + 1; j++) {
                links[i][j] = (int) (RandomHelper.nextDouble() * n); //fills rest of link lists with random links to rest of gene
            }
        }

        //fill the scores
        //NOTE that the scores can be randomly filled because they are accessed to mimic epistatic selection
        for(int i = 0; i < n; i++) {
            for(int j =0; j < numscores; j++) {
                //each different gene combination gets a different score
                scores[i][j] = RandomHelper.nextDouble();  //score drawn from uniform distribution 0-1
            }
        }

        int[] mask = new int[k+1];//a single reusable mask to look at the gene space
        //Walk through entire gene-space, filling in scores for each gene
        for(int s = 0; s < size; s++) {  //s is a point in the space, a specific vector with n dimensions (binary string); a genome in Kauffman's model, a solution in your model
        	int[] id = Constants.intToBin(s, n);  //create a binary string for this gene combo
        	
            double temp_score = 0;
            for(int g = 0; g < n; g++) {  //g is for gene, a knowledge area in your model 
                //fill the mask
                for(int m = 0; m <= k; m++) {
                    //fill the mask with the values of genes that is linked/dependent with the focal gene, g
                    mask[m] = id[links[g][m]];  //mask should be binary
                }
                //add the score of each gene in the genome
                temp_score += scores[g][Constants.binToInt(mask)];   //index score using the mask
            }
            //set the score of that genome/solution/point, which is the average across all the genes/areas/dimensions in the genome
            nk[s] = temp_score/n;
        }
    }


    public double[] getNK() {
        return nk;
    }



    //prints the NK to a file
    /* FORMAT *.nk:
    * NOTE that it is not human-readable
    * Datum 1: n  --should be read to determine how long the data struct is to be
    * Datum 2: k  --double check the appropriate k
    * Data 3-2^n+2 -- the NK file
    */
    public double write_to_file(String title) {
        title = (title+".nk");  //put the file suffix on to identify it
        int size = nk.length;
        double max = -1.;
        try{
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(title)));
            out.writeInt(n); //keep NK space data with file.
            out.writeInt(k); //keep NK space data with file.
            for(int i=0; i < size; i++) {
            	if(nk[i] > max) max = nk[i];
                out.writeDouble(nk[i]);
            }
            out.writeDouble(max);
            out.close();
        }
        catch (IOException ioe){
            System.out.println("IO Exception");
        }
        return max;
    }

    
    /**************  Additional tools *****************/
    
    //Computes the number of peaks (judged by local environments) in the space
    public int getNumPeaks() {
		int numMax = 0;

		for(int i = 0; i < nk.length; i++) { //for each point in the space
			int[] orig = Constants.intToBin(i, n);
			int max_flag = 0;  //reset flag
			//System.out.println("    " +NK.nk[i]);
			for(int j = 0; j < n; j++) { //for each of the n possible variations
				int[] neighbor = new int[n];
				neighbor = (int[]) orig.clone(); //copy the original
				
				//System.out.print("     ");  //DEBUG
				//NK.printArray(orig);		//DEBUG
				
				int bit  = (orig[j]+1) % 2;
				neighbor[j] = bit; //alter one bit
				if(nk[Constants.binToInt(neighbor)] >= nk[i]) { //compare scores
					max_flag = 1; //if neighbor is bigger, not a max
					//System.out.print("  FLAG");	
				}			
			}		
			if(max_flag==0) numMax++;
		}	
		
		return numMax;
	}

    /*********   DEBUGGING TOOLS     ********************/

    public void printArray(int[] arr) {
        System.out.print("[ ");
        for(int i = 0; i < arr.length; i++) {
            System.out.print(arr[i]+ " ");
        }
        System.out.print("] \n");
    }
    public void printDoubleArray(double[] arr) {
    System.out.print("[ ");
    for(int i = 0; i < arr.length; i++) {
        System.out.print(arr[i]+ " ");
    }
    System.out.print("] \n");
}


    public void printMatrix(int[][] m) {
        for(int i = 0; i < m.length; i++) {
            System.out.print("[ ");
            for(int j = 0; j < m[1].length; j++) {
                System.out.print(m[i][j]+ " ");
            }
            System.out.print("] \n");
        }
    }
    public void printDoubleMatrix(double[][] m) {
    for(int i = 0; i < m.length; i++) {
        System.out.print("[ ");
        for(int j = 0; j < m[1].length; j++) {
            System.out.print(m[i][j]+ " ");
        }
                    System.out.print("] \n");
    }
}
}
