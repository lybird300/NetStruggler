package NetStruggler;

import repast.simphony.space.graph.EdgeCreator;

public class CustomTieCreator implements EdgeCreator<CustomTie, OrgMember> {
	public CustomTieCreator() {
		
	}
	
	public Class<CustomTie> getEdgeType() {
		return CustomTie.class;
	}

	@Override
	public CustomTie createEdge(OrgMember source, OrgMember target,
			boolean isDirected, double weight) {
		// TODO Auto-generated method stub
		return new CustomTie(source, target, true, weight);
	}

}
