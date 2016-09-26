package apoc.algo.algorithms.walktrap;

public class Community {

    public Neighbor first_neighbor;	// first item of the list of adjacent communities
    public Neighbor last_neighbor;	// last item of the list of adjacent communities

    public int this_community;		// number of this community
    public int first_member;		// number of the first vertex of the community
    public int last_member;		// number of the last vertex of the community
    public int size;			// number of members of the community

    public Probabilities P;		// the probability vector, 0 if not stored.


    public float sigma;			// sigma(C) of the community
    public float internal_weight;	// sum of the weight of the internal edges
    public float total_weight;		// sum of the weight of all the edges of the community (an edge between two communities is a half-edge for each community)

    public int sub_communities[];	// the two sub sommunities, -1 if no sub communities;
    public int sub_community_of;		// number of the community in which this community has been merged
    // 0 if the community is active
    // -1 if the community is not used

    public void merge(Community C1, Community C2) {    // create a new community by merging C1 an C2


    }

    public void add_neighbor(Neighbor N) {
        if (last_neighbor != null) {
            if(last_neighbor.community1 == this_community)
                last_neighbor.next_community1 = N;
            else
                last_neighbor.next_community2 = N;

            if(N.community1 == this_community)
                N.previous_community1 = last_neighbor;
            else
                N.previous_community2 = last_neighbor;
        }
        else {
            first_neighbor = N;
            if(N.community1 == this_community)
                N.previous_community1 = null;
            else
                N.previous_community2 = null;
        }
        last_neighbor = N;
    }

    public void remove_neighbor(Neighbor N) {
        if (N.community1 == this_community) {
            if(N.next_community1 != null) {
//      if (N->next_community1->community1 == this_community)
                N.next_community1.previous_community1 = N.previous_community1;
//      else
//	N->next_community1->previous_community2 = N->previous_community1;
            }
            else last_neighbor = N.previous_community1;
            if(N.previous_community1 != null) {
                if (N.previous_community1.community1 == this_community)
                    N.previous_community1.next_community1 = N.next_community1;
                else
                    N.previous_community1.next_community2 = N.next_community1;
            }
            else first_neighbor = N.next_community1;
        }
        else {
            if(N.next_community2 != null) {
                if (N.next_community2.community1 == this_community)
                    N.next_community2.previous_community1 = N.previous_community2;
                else
                    N.next_community2.previous_community2 = N.previous_community2;
            }
            else last_neighbor = N.previous_community2;
            if(N.previous_community2 != null) {
//      if (N->previous_community2->community1 == this_community)
//	N->previous_community2->next_community1 = N->next_community2;
//      else
                N.previous_community2.next_community2 = N.next_community2;
            }
            else first_neighbor = N.next_community2;
        }
    }

    public float min_delta_sigma() {            // compute the minimal delta sigma among all the neighbors of this community
        float r = 1.f;
        for(Neighbor N = first_neighbor; N != null;) {
            if(N.delta_sigma < r) r = N.delta_sigma;
            if(N.community1 == this_community)
                N = N.next_community1;
            else
                N = N.next_community2;
        }
        return r;
    }

    public Community() {            // create an empty community
        P = null;
        first_neighbor = null;
        last_neighbor = null;
        sub_community_of = -1;
        sub_communities = new int[2];
        sub_communities[0] = -1;
        sub_communities[1] = -1;
        sigma = 0.f;
        internal_weight = 0.f;
        total_weight = 0.f;
    }
}
