package NetStruggler;


import java.util.*;

import org.apache.commons.collections15.Transformer;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.algorithms.filters.FilterUtils;
import edu.uci.ics.jung.algorithms.importance.BetweennessCentrality;
import edu.uci.ics.jung.algorithms.metrics.*;
import edu.uci.ics.jung.algorithms.shortestpath.DistanceStatistics;
import edu.uci.ics.jung.graph.Graph;
import repast.simphony.context.space.graph.ContextJungNetwork;
import repast.simphony.space.graph.RepastEdge;

/**
 * This class provides some methods for analyzing the organizational social network.
 * It extends and utilizes JUNG class/functions, whose .jar files has already been included in the library of "Repast Simphony Development" (see left menu).
 * You may think the organizational social network should be an attribute of this class. However, this is a static class whereas the network keeps evolving.
 * Thus, we'd better input the latest network as a parameter when invoking any function inside this class.
 * @author Yuan Lin
 */
@SuppressWarnings("unused")
public class NetOperation extends Metrics {
	private ContextJungNetwork<OrgMember> targetNet;
	//private int compoNum = 0;
	private int maxCompoSize = 0;
	private double avgMaxCompoDeg = 0.;
    private int netSize = 0;
    private double avgShortPathMaxCompo = 0;
    private double avgClustCoeff = 0.;
    private double olp_wgt_corr = 0;
    //private double kdiff_wgt_corr = 0;
	//private double kdiff_olp_corr = 0;
	private double deg_stg_corr = 0;
    //private double deg_score_corr = 0;
    //private double stg_score_corr = 0;
	//private double dpt_score_corr = 0;

    //private double modularity;
	//public Collection<Set<OrgMember>> communities;
    //private int nonovlpCommNum;
	
	public NetOperation(ContextJungNetwork<OrgMember> net, int orgSize){
		targetNet = net;
		netSize = orgSize;
	}	
	
	public void calTiePropCorr() {
		int tieNum = targetNet.numEdges();
		if(tieNum < 2) return;
		double[] overlap = new double[tieNum];
		double[] weight = new double[tieNum];
		//double[] knowDiff = new double[tieNum];
		int i = 0;
		for (RepastEdge<OrgMember> e: targetNet.getEdges()) {
			overlap[i] = calOverlap(e);
			weight[i] = e.getWeight();
			//knowDiff[i] = (double)calKnowDiff(e);
			i++;
		}
		PearsonsCorrelation corr = new PearsonsCorrelation();
		olp_wgt_corr = corr.correlation(overlap, weight);
		//kdiff_wgt_corr = corr.correlation(weight, knowDiff);
		//kdiff_olp_corr = corr.correlation(overlap, knowDiff);
	}

	private int calKnowDiff(RepastEdge<OrgMember> e) {
		int[] soln1 = e.getSource().getSolution().clone();
		int[] soln2 = e.getTarget().getSolution().clone();
		int diff = 0;
		for(int i = 0; i < soln1.length; i++)
			if(soln1[i] != soln2[i]) diff++;
		return diff;
	}

	public void calNodePropCorr() {
		double[] degree = new double[netSize];
		double[] strength = new double[netSize];
		//double[] score = new double[netSize];
		//double[] disparity = new double[netSize];
		int i = 0;
		//double[] result = new double[2];
		for (OrgMember node : targetNet.getNodes()) {
			degree[i] = targetNet.getDegree(node);
			strength[i] = getNodeStrength(node);
			//result = getStrengthDisparity(node);
			//strength[i] = result[0];
			//score[i] = node.getScore();
			//disparity[i] = result[1];
			i++;
		}
		PearsonsCorrelation corr = new PearsonsCorrelation();
		deg_stg_corr = corr.correlation(degree, strength);	
		//deg_score_corr = corr.correlation(degree, score);
		//stg_score_corr = corr.correlation(score, strength);
		//dpt_score_corr = corr.correlation(score, disparity);
	}

	/**
	 * It is calculated from an individual organizational members' perspective.
	 * @param om
	 * @return
	 */
	private double getNodeStrength(OrgMember om) {
		int om_d = targetNet.getDegree(om);
	   	if (om_d == 0) return 0;
	   	else{
	   		int om_index = Integer.parseInt(om.getID()) - 1;
	   		return OrgMember.TieWtSumArray[om_index]/om_d;
	   	}
	}
	/*private double getNodeStrength(OrgMember om) {
	   	if (targetNet.getDegree(om)==0) return 0;
    	double strength = 0.;
    	for (OrgMember neighbor : targetNet.getAdjacent(om)){
    		RepastEdge<OrgMember> e = targetNet.getEdge(om, neighbor);
        	strength += e.getWeight();
        }
		return strength;
	}*/

	public boolean findComponentsCalShortPath(){
		Graph<OrgMember, RepastEdge<OrgMember>> g = targetNet.getGraph();
		WeakComponentClusterer<OrgMember, RepastEdge<OrgMember>> wcc = new WeakComponentClusterer<OrgMember, RepastEdge<OrgMember>>();
		Set<Set<OrgMember>> components = wcc.transform(g);
		Set<OrgMember> maxCompoSet = null;
		//compoNum = components.size();
		for(Set<OrgMember> c: components){
			if(maxCompoSize < c.size()){
				maxCompoSize  = c.size();
				maxCompoSet = c;
			}
		}
		if(maxCompoSize>2){ //otherwise (max component size = 1 or 2), keep the default value of avgShortPathMaxCompo, which is 0 
			Graph<OrgMember, RepastEdge<OrgMember>> subg = FilterUtils.createInducedSubgraph(maxCompoSet, g); //get the max component
			avgMaxCompoDeg = subg.getEdgeCount()*2.0/maxCompoSize;
			if(avgMaxCompoDeg == 0)
				System.out.println("NetOperationL141: Weird...\n");
			avgShortPathMaxCompo = calAvgShortestPath(subg);
			return true;
		}
		else return false;
	}
    
	/*public int getCompoNum() {
		return compoNum;
	}*/

	public double getMaxCompoRatio() {
		return (double) maxCompoSize/netSize;
	}
	
	
	/**
	 * Calculates the average node degree of the network
	 * @param numOfNodes
	 * 		the size of the network

	public double getAvgDC() {
		int sumDegree = 0;
		for (OrgMember node : targetNet.getNodes())
			sumDegree += targetNet.getDegree(node);
		return (double) sumDegree / (double) netSize;
	}	 */

	/**
	 * Calculate the density of the entire network
	 * @param orgSize
	 * 		the size of the network
	 */
	public double getNetworkDensity() {
		int maxNumOfTies = netSize * (netSize - 1) / 2;
		return (double) targetNet.numEdges() / (double) maxNumOfTies;
	}
	
	/**
	 * Calculate the mean value of network nodes' clustering coefficient
	 * @param orgSocialNetwork
	 * @see clusteringCoefficients
	 */
	public double calAvgClusterCoeff(){
		Graph<OrgMember, RepastEdge<OrgMember>> g = targetNet.getGraph();
		Map<OrgMember, Double> cc = clusteringCoefficients(g);
		double ccSum = 0.;
		for (Map.Entry<OrgMember, Double> single_cc : cc.entrySet())
			ccSum += single_cc.getValue();
		avgClustCoeff = ccSum/(double)netSize;
		return avgClustCoeff;
	}
	
	
	/**
	 * For each vertex v in graph, calculates the average shortest path length from v to all other vertices in graph, ignoring edge weights.
	 * Returns the results in a Map from vertices to Double values. If there exists an ordered pair <u,v> for which d.getDistance(u,v) returns null, then the average distance value for u will be stored as Double.POSITIVE_INFINITY).
	 * @param orgSocialNetwork
	 * @return
	 */
	private double calAvgShortestPath(Graph<OrgMember, RepastEdge<OrgMember>> g){
		Transformer<OrgMember, Double> avg_d = DistanceStatistics.averageDistances(g);
		double spSum = 0.0;
		for(OrgMember om: g.getVertices()){
			spSum += avg_d.transform(om);
		}
		//if(g.getVertexCount() != maxCompoSize)
			//System.out.println("NetOperation/L186: number of nodes do not match\n");
		return spSum/(double)maxCompoSize;
	}
	
	public double getAvgSPLMaxComp(){
		return avgShortPathMaxCompo;
	}
	

	/**
	 * Calculate the network's betweenness centralization based on Equation 5.13
	 * in Wasserman, S., & Faust, K. (1994). Social network analysis methods and applications. p.191  
	 * The betweenness centrality of each node is calculated using JUNG
	 * <a href="http://jung.sourceforge.net/doc/api/edu/uci/ics/jung/algorithms/importance/BetweennessCentrality.html>BetweennessCentrality class</a>
	 * @param orgSocialNetwork
	 * @return
	 */
	public double getBetweenCentralization(){
		Graph<OrgMember, RepastEdge<OrgMember>> g = targetNet.getGraph();
        BetweennessCentrality<OrgMember, RepastEdge<OrgMember>> ranker = 
                new BetweennessCentrality<OrgMember, RepastEdge<OrgMember>>(g);
        ranker.setRemoveRankScoresOnFinalize(false); 
        ranker.evaluate();
        double maxBC = 0.;
        double sumBC = 0.;
        for (OrgMember om: targetNet.getNodes())
        	if (ranker.getVertexRankScore(om) > maxBC )
        		maxBC = ranker.getVertexRankScore(om);
        for (OrgMember om: targetNet.getNodes())
        	sumBC += maxBC - ranker.getVertexRankScore(om);
        return 2*sumBC/(Math.pow((netSize - 1), 2.)*(netSize - 2));
	}
	
	/**
	 * This function and related sub-functions are modified from the JUNG StructuralHoles class(edu.uci.ics.jung.algorithms.metrics.StructuralHoles),
	 * which calculates some of the measures from Burt's text "Structural Holes: The Social Structure of Competition".
	 * <p>The original codes are donated by Jasper Voskuilen and Diederik van Liere of the Department of Information and Decision Sciences at Erasmus University,
	 * and are converted to jung2 by Tom Nelson.</p>
	 * 
     * Burt's constraint measure (equation 2.4, page 55 of Burt, 1992). Essentially a
     * measure of the extent to which <code>i</code> is invested in people who are invested in
     * other of <code>i</code>'s alters (neighbors). The "constraint" is characterized
     * by a lack of primary holes around each neighbor. Formally:
     * <pre>
     * constraint(i) = sum_{j in MP(i), j != i} localConstraint(i,j)
     * </pre>
     * where MP(i) is the set of i's neighbors(in a non-directed network context).
     * @return
	 * 		the constraint measure
     * @see #localConstraint(Object, Object)
     */
    public double constraint(OrgMember i) {
        if(targetNet.getDegree(i) == 0) return 0;
        double result = 0;
        for(OrgMember j : targetNet.getAdjacent(i))
        	if(!i.getID().equals(j.getID()))
        		result += localConstraint(i, j);
        return result;
    }
    /**
     * Return the local constraint on <code>i</code> from a lack of primary holes around its neighbor <code>j</code>.
     * Based on Burt's equation 2.4. Formally:
     * <pre>
     * localConstraint(i, j) = (w(i,j) + (sum_{q in N(i)} w(i,q) * w(q, j)))^2
     * </pre>
     * where 
     * <ul>
     * <li/><code>N(i) is the set of i's neighbors(in a non-directed network context)</code>
     * <li/><code>w(i,q) =</code> normalized tie weight of i and q
     * </ul>
     * @see #normalizedTieWeight(Object, Object)
     */
    public double localConstraint(OrgMember i, OrgMember j) 
    {	
        double nmtw_ij = normalizedTieWeight(i, j);
        double inner_result = 0;
        for (OrgMember q : targetNet.getAdjacent(i))
            inner_result += normalizedTieWeight(i, q) * normalizedTieWeight(q, j);
        return Math.pow((nmtw_ij + inner_result), 2);
    }
    /**
     * Return the proportion of <code>v1</code>'s network time and energy invested
     * in the relationship with <code>v2</code>. Formally:
     * <pre>
     * normalizedMutualEdgeWeight(v1,v2) = mutual_weight(v1,v2) / (sum_g mutual_weight(v1,g))
     * </pre>
     * Returns 0 if either numerator or denominator = 0, or if <code>v1 == v2</code>.
     */
    private double normalizedTieWeight(OrgMember v1, OrgMember v2)
    {
        if (v1.getID().equals(v2.getID()))
            return 0;
        double numerator = 0.;
        RepastEdge<OrgMember> e = targetNet.getEdge(v1, v2);
        if(e == null) return 0.;
        else if (targetNet.getDegree(v1)==1) return 1.;
        else{
        	numerator = e.getWeight();
	        double denominator = getNodeStrength(v1);
	        if (denominator == 0) return 0.; 
	        else return numerator / denominator;
	    }   
    }
    
    private double[] getStrengthDisparity(OrgMember om){
    	double[] results = {0, 0};
    	if (targetNet.getDegree(om)==0) return results;
    	double strength = 0.;
    	double wSumOfSqr = 0.;
    	for (OrgMember neighbor : targetNet.getAdjacent(om)){
    		RepastEdge<OrgMember> e = targetNet.getEdge(om, neighbor);
    		double w = e.getWeight();
        	strength += w;
        	wSumOfSqr += w*w;
        }
    	results[0] = strength;
    	results[1] = wSumOfSqr/(strength*strength);
    	return results;
    }
    
    
    /*public double getDegDistPowLaw(){
    	DegreeDistribution dd = new DegreeDistribution(targetNet);
    	return dd.getPowerLaw();
    }*/
	
    /**
     * The weighted clustering coefficient is calculated based on the definition in Barrat et al. Proc. Natl. Acad. Sci. 2004
     */
    public double calWeightedCCStrength() {
        double totalCC = 0;
        for (OrgMember node : targetNet.getNodes()) {
            double nodeCC = 0;
            double cc = 0;
            int degree = targetNet.getDegree(node);
            double strength = 0;
            
        	//Calculate the node strength
            if(degree > 0)
     	   		strength = getNodeStrength(node);
     	   	
            //if a node have degree < 2 means that this node can't form a triangle.
            if (degree < 2) cc = 0;
            else {
                //Search Triangle
                //browse the pairs of neighbors
                for (OrgMember neightbor1 : targetNet.getAdjacent(node)){
                    for (OrgMember neightbor2 : targetNet.getAdjacent(node)){
                        if (neightbor1.getID().equals(neightbor2.getID())) continue;
                        //find a triangle
                        if (targetNet.isAdjacent(neightbor1, neightbor2))
                     	   nodeCC += (targetNet.getEdge(node, neightbor1).getWeight() + targetNet.getEdge(node, neightbor2).getWeight()) / 2;
                    }
                }
                nodeCC /= 2.0;//because a triangle is seen 2 times
                //Calculate local weighted cluster coefficient
                cc = (2 / (strength * (degree - 1))) * nodeCC;
            }
            totalCC += cc;
        }
        return totalCC / netSize;
    }
    
    /**
     * The weighted clustering coefficient is calculated based on the definition in Bolanos et al. Journal of Neuroscience Methods. 2013
     */
    public double calWeightedCCStrength2() {
        double totalCC = 0;
        double maxWeight = 0.;
        for (RepastEdge<OrgMember> e: targetNet.getEdges()) {
        	if(maxWeight < e.getWeight()) maxWeight = e.getWeight();
        }
        for (OrgMember node : targetNet.getNodes()) {
            double nodeCC2 = 0;//sum of the product of two weights
        	double nodeCC3 = 0;//sum of the product of three weights
            double cc = 0;
            int degree = targetNet.getDegree(node);
     	   	
            //if a node have degree < 2 means that this node can't form a triangle.
            if (degree < 2) cc = 0;
            else {
                //Search Triangle
                //browse the pairs of neighbors
                for (OrgMember neightbor1 : targetNet.getAdjacent(node)){
                	if(targetNet.getDegree(neightbor1) == 1) continue;
                    for (OrgMember neightbor2 : targetNet.getAdjacent(neightbor1)){
                        if (node.getID().equals(neightbor2.getID())) continue;
                        if (targetNet.isAdjacent(node, neightbor2))//find a triangle
                     	   nodeCC3 += targetNet.getEdge(node, neightbor1).getWeight()*targetNet.getEdge(node, neightbor2).getWeight()*
                     	   		targetNet.getEdge(neightbor1, neightbor2).getWeight();
                        nodeCC2 += targetNet.getEdge(node, neightbor1).getWeight()*targetNet.getEdge(neightbor1, neightbor2).getWeight();                     	
                    }
                }
                double denominator = nodeCC2 + nodeCC3;
                //Calculate local weighted cluster coefficient
                if(denominator == 0) cc = 0;
                else cc = 3*nodeCC3/denominator;
            }
            totalCC += cc;
        }
        return totalCC / netSize;
    }
    
    private double calOverlap(RepastEdge<OrgMember> e){              
    	// get nodes on both sides of the edge
    	HashSet<OrgMember> sourceSet = new HashSet<OrgMember>();
    	HashSet<OrgMember> targetSet = new HashSet<OrgMember>();
    	for(OrgMember snb : targetNet.getAdjacent(e.getSource()))
    		sourceSet.add(snb);
    	for(OrgMember tnb : targetNet.getAdjacent(e.getTarget()))
    		targetSet.add(tnb);
        Set<OrgMember> numerator = new HashSet<OrgMember>(sourceSet);
        Set<OrgMember> denominator = new HashSet<OrgMember>(sourceSet);
        double overlap = 0;
        // find intersection and union
        numerator.retainAll(targetSet);
        denominator.addAll(targetSet);                
        if (denominator.size() > 0)
        	overlap = ((double) numerator.size() / (double) denominator.size());
        return overlap;
    } 
    
    /*public double getAvgOverlap() {
        return avgOverlap;
    }*/
    
    /**
     * Following Newman's definition (unweighted network)
     */
    public double getAssortativity(){     
        double edges_count = targetNet.numEdges(); 
        double num1  = 0 , num2 = 0 , den = 0; 
        for (RepastEdge<OrgMember> e : targetNet.getEdges()){
        	OrgMember v1 = e.getSource();
        	OrgMember v2 = e.getTarget();
        	int d_v1 = targetNet.getDegree(v1);
        	int d_v2 = targetNet.getDegree(v2);
            num1 += d_v1*d_v2;
            num2 += (d_v1+d_v2);
            den += (d_v1*d_v1+d_v2*d_v2);
        }
        num1 /= edges_count;
        num2 = (num2/(2 * edges_count)) * (num2/(2 * edges_count));
        den /= (2 * edges_count);
        
       return (double)(num1-num2)/(double)(den-num2);
    }
    
    public double maxCompSmallWorldQ(){
    	double lcc = getAvgClusterCoeff();
    	double lccR = avgMaxCompoDeg/maxCompoSize;
    	double spl = getAvgSPLMaxComp();
    	double splR = Math.log(maxCompoSize)/Math.log(avgMaxCompoDeg);
    	return (lcc/lccR)/(spl/splR);
    }

	private double getAvgClusterCoeff() {
		return avgClustCoeff;
	}

	public double getOlpWgtCorr() {
		return olp_wgt_corr;
	}
	
	/*public double getKdiffWgtCorr() {
		return kdiff_wgt_corr;
	}


	public double getKdiffOlpCorr() {
		return kdiff_olp_corr;
	}

	public double getDegScoreCorr() {
		return deg_score_corr;
	}

	public double getStgScoreCorr() {
		return stg_score_corr;
	}

	public double getDptScoreCorr() {
		return dpt_score_corr ;
	}*/
	
	public double getDegStgCorr() {
		return deg_stg_corr;
	}
}

