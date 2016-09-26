package apoc.algo.algorithms.walktrap;

public class Neighbor {
    int community1;     // Two adjacent communities
    int community2;     // community1 < community2

    float delta_sigma;      // the delta sigma between two communities.
    float weight;           // total weight of edges between two communities.
    boolean exact;          // true if delta_sigma is exact, false if it is only a lower bound.

    Neighbor next_community1;	    // pointers of two double
    Neighbor previous_community1;    // chained lists containing
    Neighbor next_community2;	    // all the neighbors of
    Neighbor previous_community2;    // each communities.

    int heap_index;	//

    Neighbor() {};
}
