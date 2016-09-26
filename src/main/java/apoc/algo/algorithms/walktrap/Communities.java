package apoc.algo.algorithms.walktrap;

public class Communities {
    private boolean silent;		// whether the progression is displayed
    private int details;		// between 0 and 3, how much details are printed
    private long max_memory;	// size in Byte of maximal memory usage, -1 for no limit

    public long memory_used;				    // in bytes
    public Min_delta_sigma_heap min_delta_sigma;    	    // the min delta_sigma of the community with a saved probability vector (for memory management)

    public Graph G;		    // the graph
    public int members[];		    // the members of each community represented as a chained list.
    // a community points to the first_member the array which contains
    // the next member (-1 = end of the community)
    public Neighbor_heap H;	    // the distances between adjacent communities.

    public Community communities[];	// array of the communities

    public int nb_communities;		// number of valid communities
    public int nb_active_communities;	// number of active communities

    // Results arrays:
    public float modularity[];
    public int matrix[][];
    private int mergeIdx;
    public float find_best_modularity(int community, boolean max_modularity[]) {
        float Q = (communities[community].internal_weight - communities[community].total_weight*communities[community].total_weight/G.total_weight)/G.total_weight;
        if(communities[community].sub_communities[0] == -1) {
            max_modularity[community] = true;
            return Q;
        }
        float Q2 = find_best_modularity(communities[community].sub_communities[0], max_modularity) + find_best_modularity(communities[community].sub_communities[1], max_modularity);
        if(Q2 > Q) {
            max_modularity[community] = false;
            return Q2;
        }
        else {
            max_modularity[community] = true;
            return Q;
        }

    }
    public void print_best_modularity_partition() {
        boolean max_modularity[]  = new boolean[nb_communities];
        float Q = find_best_modularity(nb_communities-1, max_modularity);
        System.out.println("Maximal modularity Q = " + Q + " for partition :");
        print_best_modularity_partition(nb_communities - 1, max_modularity);
        max_modularity = null;
    }

    void print_best_modularity_partition(int community, boolean max_modularity[]) {
        if(max_modularity[community])
            print_community(community);
        else {
            print_best_modularity_partition(communities[community].sub_communities[0], max_modularity);
            print_best_modularity_partition(communities[community].sub_communities[1], max_modularity);
        }
    }

//   Communities(Graph* G, int random_walks_length = 3, bool silent = false, int details = 1, long max_memory = -1);    // Constructor

    Communities(Graph graph, int random_walks_length, boolean s, int d, long m) {   // Constructor
        silent = s;
        details = d;
        max_memory = m;
        memory_used = 0;
        G = graph;
        mergeIdx = 0;

        Probabilities.C = this;
        Probabilities.length = random_walks_length;
        Probabilities.tmp_vector1 = new float[G.nb_vertices];
        Probabilities.tmp_vector2 = new float[G.nb_vertices];
        Probabilities.id = new int[G.nb_vertices];
        for(int i = 0; i < G.nb_vertices; i++) Probabilities.id[i] = 0;
        Probabilities.vertices1 = new int[G.nb_vertices];
        Probabilities.vertices2 = new int[G.nb_vertices];
        Probabilities.current_id = 0;

        modularity = new float[G.nb_vertices];
        matrix = new int[G.nb_vertices][2];

        members = new int[G.nb_vertices];
        for(int i = 0; i < G.nb_vertices; i++)
            members[i] = -1;


        H = new Neighbor_heap(G.nb_edges);

        communities = new Community[2*G.nb_vertices];

        for (int i = 0; i < communities.length; i++) {
            communities[i] = new Community();
        }

// init the n single vertex communities

        if(max_memory != -1)
            min_delta_sigma = new Min_delta_sigma_heap(G.nb_vertices*2);
        else min_delta_sigma = null;


        for(int i = 0; i < G.nb_vertices; i++) {
            communities[i].this_community = i;
            communities[i].first_member = i;
            communities[i].last_member = i;
            communities[i].size = 1;
            communities[i].sub_community_of = 0;
        }


        nb_communities = G.nb_vertices;
        nb_active_communities = G.nb_vertices;

        if(!silent) System.err.printf("computing random walks and the first distances:");
        for(int i = 0; i < G.nb_vertices; i++)
            for(int j = 0; j < G.vertices[i].degree; j++)
                if (i < G.vertices[i].edges[j].neighbor) {
                    communities[i].total_weight += G.vertices[i].edges[j].weight/2.;
                    communities[G.vertices[i].edges[j].neighbor].total_weight += G.vertices[i].edges[j].weight/2.;
                    Neighbor N = new Neighbor();
                    N.community1 = i;
                    N.community2 = G.vertices[i].edges[j].neighbor;
                    N.delta_sigma = (float) (-1./(double)(Math.min(G.vertices[i].degree,  G.vertices[G.vertices[i].edges[j].neighbor].degree)));
                    N.weight = G.vertices[i].edges[j].weight;
                    N.exact = false;
                    add_neighbor(N);
                }
        int c = 0;
        Neighbor N = H.get_first();
        if (N == null) {
            return;
        }

        while(!N.exact) {
            update_neighbor(N, (float)compute_delta_sigma(N.community1, N.community2));
            N.exact = true;
            N = H.get_first();
            if(max_memory != -1) manage_memory();
            if(!silent) {
                c++;
                for(int k = (500*(c-1))/G.nb_edges + 1; k <= (500*c)/G.nb_edges; k++) {
                    if(k % 50 == 1) {
                        System.err.println( k/ 5 + "% ");
                    }
                    System.out.println(".");
                }
            }
        }
        if (details >= 2) System.out.println("Partition 0 (" + G.nb_vertices  + " communities)\n");
        if (details >= 2) for(int i = 0; i < G.nb_vertices; i++) print_community(i);
        if (details >= 2) System.out.println("\n");
    }


    void merge_communities(Neighbor merge_N) {            // create a community by merging two existing communities
        int c1 = merge_N.community1;
        int c2 = merge_N.community2;

        communities[nb_communities].first_member = communities[c1].first_member;	// merge the
        communities[nb_communities].last_member = communities[c2].last_member;	// two lists
        members[communities[c1].last_member] = communities[c2].first_member;		// of members

        communities[nb_communities].size = communities[c1].size + communities[c2].size;
        communities[nb_communities].this_community = nb_communities;
        communities[nb_communities].sub_community_of = 0;
        communities[nb_communities].sub_communities[0] = c1;
        communities[nb_communities].sub_communities[1] = c2;
        communities[nb_communities].total_weight = communities[c1].total_weight + communities[c2].total_weight;
        communities[nb_communities].internal_weight = communities[c1].internal_weight + communities[c2].internal_weight + merge_N.weight;
        communities[nb_communities].sigma = communities[c1].sigma + communities[c2].sigma + merge_N.delta_sigma;

        communities[c1].sub_community_of = nb_communities;
        communities[c2].sub_community_of = nb_communities;

// update the new probability vector...

        if(communities[c1].P != null && communities[c2].P != null) communities[nb_communities].P = new Probabilities(c1, c2);

        if(communities[c1].P != null) {
            communities[c1].P = null;
            if(max_memory != -1) min_delta_sigma.remove_community(c1);
        }
        if(communities[c2].P != null) {
            communities[c2].P = null;
            if(max_memory != -1) min_delta_sigma.remove_community(c2);
        }

        if(max_memory != -1) {
            min_delta_sigma.delta_sigma[c1] = -1.f;		    // to avoid to update the min_delta_sigma for these communities
            min_delta_sigma.delta_sigma[c2] = -1.f;		    //
            min_delta_sigma.delta_sigma[nb_communities] = -1.f;
        }

// update the new neighbors
// by enumerating all the neighbors of c1 and c2

        Neighbor N1 = communities[c1].first_neighbor;
        Neighbor N2 = communities[c2].first_neighbor;

        while(N1 != null && N2 != null) {
            int neighbor_community1;
            int neighbor_community2;

            if (N1.community1 == c1) neighbor_community1 = N1.community2;
            else neighbor_community1 = N1.community1;
            if (N2.community1 == c2) neighbor_community2 = N2.community2;
            else neighbor_community2 = N2.community1;

            if (neighbor_community1 < neighbor_community2) {
                Neighbor tmp = N1;
                if (N1.community1 == c1) N1 = N1.next_community1;
                else N1 = N1.next_community2;
                remove_neighbor(tmp);
                Neighbor N = new Neighbor();
                N.weight = tmp.weight;
                N.community1 = neighbor_community1;
                N.community2 = nb_communities;
                N.delta_sigma = (float) (((double)(communities[c1].size+communities[neighbor_community1].size)*tmp.delta_sigma +
                                        (double)(communities[c2].size)*merge_N.delta_sigma)/((double)(communities[c1].size+communities[c2].size+communities[neighbor_community1].size)));//compute_delta_sigma(neighbor_community1, nb_communities);
                N.exact = false;
                tmp = null;
                add_neighbor(N);
            }

            if (neighbor_community2 < neighbor_community1) {
                Neighbor tmp = N2;
                if (N2.community1 == c2) N2 = N2.next_community1;
                else N2 = N2.next_community2;
                remove_neighbor(tmp);
                Neighbor N = new Neighbor();
                N.weight = tmp.weight;
                N.community1 = neighbor_community2;
                N.community2 = nb_communities;
                N.delta_sigma = (float) (((double)(communities[c1].size)*merge_N.delta_sigma +
                                        (double)(communities[c2].size+communities[neighbor_community2].size)*tmp.delta_sigma)/((double)(communities[c1].size+communities[c2].size+communities[neighbor_community2].size)));//compute_delta_sigma(neighbor_community2, nb_communities);
                N.exact = false;
                tmp = null;
                add_neighbor(N);
            }

            if (neighbor_community1 == neighbor_community2) {
                Neighbor tmp1 = N1;
                Neighbor tmp2 = N2;
                boolean exact = N1.exact && N2.exact;
                if (N1.community1 == c1) N1 = N1.next_community1;
                else N1 = N1.next_community2;
                if (N2.community1 == c2) N2 = N2.next_community1;
                else N2 = N2.next_community2;
                remove_neighbor(tmp1);
                remove_neighbor(tmp2);
                Neighbor N = new Neighbor();
                N.weight = tmp1.weight + tmp2.weight;
                N.community1 = neighbor_community1;
                N.community2 = nb_communities;
                N.delta_sigma = (float) (((double)(communities[c1].size+communities[neighbor_community1].size)*tmp1.delta_sigma +
                                        (double)(communities[c2].size+communities[neighbor_community1].size)*tmp2.delta_sigma
                                        - (double)(communities[neighbor_community1].size)*merge_N.delta_sigma)/((double)(communities[c1].size+communities[c2].size+communities[neighbor_community1].size)));
                N.exact = exact;
                tmp1 = null;
                tmp2 = null;
                add_neighbor(N);
            }
        }


        if(N1 == null) {
            while(N2 != null) {
            //      double delta_sigma2 = N2.delta_sigma;
                int neighbor_community;
                if (N2.community1 == c2) neighbor_community = N2.community2;
                else neighbor_community = N2.community1;
                Neighbor tmp = N2;
                if (N2.community1 == c2) N2 = N2.next_community1;
                else N2 = N2.next_community2;
                remove_neighbor(tmp);
                Neighbor N = new Neighbor();
                N.weight = tmp.weight;
                N.community1 = neighbor_community;
                N.community2 = nb_communities;
                N.delta_sigma = (float) (((double)(communities[c1].size)*merge_N.delta_sigma
                                        + (double)(communities[c2].size+communities[neighbor_community].size)*tmp.delta_sigma)/((double)(communities[c1].size+communities[c2].size+communities[neighbor_community].size)));//compute_delta_sigma(neighbor_community, nb_communities);
                N.exact = false;
                tmp = null;
                add_neighbor(N);
            }
        }
        if(N2 == null) {
            while(N1 != null) {
//      double delta_sigma1 = N1.delta_sigma;
                int neighbor_community;
                if (N1.community1 == c1) neighbor_community = N1.community2;
                else neighbor_community = N1.community1;
                Neighbor tmp = N1;
                if (N1.community1 == c1) N1 = N1.next_community1;
                else N1 = N1.next_community2;
                remove_neighbor(tmp);
                Neighbor N = new Neighbor();
                N.weight = tmp.weight;
                N.community1 = neighbor_community;
                N.community2 = nb_communities;
                N.delta_sigma = (float) (((double)(communities[c1].size+communities[neighbor_community].size)*tmp.delta_sigma
                        + (double)(communities[c2].size)*merge_N.delta_sigma)/((double)(communities[c1].size+communities[c2].size+communities[neighbor_community].size)));//compute_delta_sigma(neighbor_community, nb_communities);
                N.exact = false;
                tmp = null;
                add_neighbor(N);
            }
        }

        if(max_memory != -1) {
            min_delta_sigma.delta_sigma[nb_communities] = communities[nb_communities].min_delta_sigma();
            min_delta_sigma.update(nb_communities);
        }

        nb_communities++;
        nb_active_communities--;

    }

    double merge_nearest_communities() {
        Neighbor N = H.get_first();
        while(!N.exact) {
            update_neighbor(N, (float)compute_delta_sigma(N.community1, N.community2));
            N.exact = true;
            N = H.get_first();
            if(max_memory != -1) manage_memory();
        }

        double d = N.delta_sigma;
        remove_neighbor(N);

        merge_communities(N);
        if(max_memory != -1) manage_memory();

        matrix[mergeIdx][0] = N.community1;
        matrix[mergeIdx][1] = N.community2;

        mergeIdx++;
        if(details >= 2)
            System.out.println("Partition " + (nb_communities - G.nb_vertices) + " (" +
                    (2*G.nb_vertices - nb_communities) + " communities)");
        if(details >= 2)
            System.out.println("community " + N.community1 + " + community " + N.community2 +
                    " --> community " + (nb_communities - 1));


        float Q = 0.f;
        for(int i = 0; i < nb_communities; i++)
            if(communities[i].sub_community_of == 0)
                Q += (communities[i].internal_weight - communities[i].total_weight*communities[i].total_weight/G.total_weight)/G.total_weight;
        if(details >= 3)
            System.out.println("Q = " + Q + "  #  " + "delta_sigma = " +d);;
        modularity[mergeIdx] = Q;

        if(details == 4) print_community(nb_communities-1);
        if(details >= 5) print_partition(2*G.nb_vertices - nb_communities);
        if(details >= 2) System.out.println();
        N = null;

        if(!silent) {
            for(int k = (500*(G.nb_vertices - nb_active_communities - 1))/(G.nb_vertices-1) + 1; k <= (500*(G.nb_vertices - nb_active_communities))/(G.nb_vertices-1); k++) {
                if(k % 50 == 1) {
                    System.out.println(k/ 5 + "% ");}
                System.out.println(".");
            }
        }
        return d;

    }


    double compute_delta_sigma(int community1, int community2) {        // compute delta_sigma(c1,c2)
        if(communities[community1].P == null) {
            communities[community1].P = new Probabilities(community1);
            if(max_memory != -1) min_delta_sigma.update(community1);
        }
        if(communities[community2].P == null) {
            communities[community2].P = new Probabilities(community2);
            if(max_memory != -1) min_delta_sigma.update(community2);
        }

        return communities[community1].P.compute_distance(communities[community2].P)
                *(double)(communities[community1].size)*(double)(communities[community2].size)/(double)(communities[community1].size + communities[community2].size);

    }

    void remove_neighbor(Neighbor N) {
        communities[N.community1].remove_neighbor(N);
        communities[N.community2].remove_neighbor(N);
        H.remove(N);

        if(max_memory !=-1) {
            if(N.delta_sigma == min_delta_sigma.delta_sigma[N.community1]) {
                min_delta_sigma.delta_sigma[N.community1] = communities[N.community1].min_delta_sigma();
                if(communities[N.community1].P != null) min_delta_sigma.update(N.community1);
            }

            if(N.delta_sigma == min_delta_sigma.delta_sigma[N.community2]) {
                min_delta_sigma.delta_sigma[N.community2] = communities[N.community2].min_delta_sigma();
                if(communities[N.community2].P != null) min_delta_sigma.update(N.community2);
            }
        }

    }
    void add_neighbor(Neighbor N) {
        communities[N.community1].add_neighbor(N);
        communities[N.community2].add_neighbor(N);
        H.add(N);

        if(max_memory !=-1) {
            if(N.delta_sigma < min_delta_sigma.delta_sigma[N.community1]) {
                min_delta_sigma.delta_sigma[N.community1] = N.delta_sigma;
                if(communities[N.community1].P != null) min_delta_sigma.update(N.community1);
            }

            if(N.delta_sigma < min_delta_sigma.delta_sigma[N.community2]) {
                min_delta_sigma.delta_sigma[N.community2] = N.delta_sigma;
                if(communities[N.community2].P != null) min_delta_sigma.update(N.community2);
            }
        }
    }
    void update_neighbor(Neighbor N, float new_delta_sigma) {
        if(max_memory !=-1) {
            if(new_delta_sigma < min_delta_sigma.delta_sigma[N.community1]) {
                min_delta_sigma.delta_sigma[N.community1] = new_delta_sigma;
                if(communities[N.community1].P != null) min_delta_sigma.update(N.community1);
            }

            if(new_delta_sigma < min_delta_sigma.delta_sigma[N.community2]) {
                min_delta_sigma.delta_sigma[N.community2] = new_delta_sigma;
                if(communities[N.community2].P != null) min_delta_sigma.update(N.community2);
            }

            float old_delta_sigma = N.delta_sigma;
            N.delta_sigma = new_delta_sigma;
            H.update(N);

            if(old_delta_sigma == min_delta_sigma.delta_sigma[N.community1]) {
                min_delta_sigma.delta_sigma[N.community1] = communities[N.community1].min_delta_sigma();
                if(communities[N.community1].P != null) min_delta_sigma.update(N.community1);
            }

            if(old_delta_sigma == min_delta_sigma.delta_sigma[N.community2]) {
                min_delta_sigma.delta_sigma[N.community2] = communities[N.community2].min_delta_sigma();
                if(communities[N.community2].P != null) min_delta_sigma.update(N.community2);
            }
        }
        else {
            N.delta_sigma = new_delta_sigma;
            H.update(N);
        }

    }

    void manage_memory() {
        while((memory_used > max_memory) && !min_delta_sigma.is_empty()) {
            int c = min_delta_sigma.get_max_community();
            communities[c].P = null;
            min_delta_sigma.remove_community(c);
        }
    }

    void print_state() {
        System.out.println("number of communities : " + nb_active_communities);
        for(int c = 0; c < nb_communities; c++)
            if(communities[c].sub_community_of == 0)
                print_community(c);
    }

    void print_partition(int nb_remaining_commities) {    // print the partition for a given number of communities
        int last_community = 2*G.nb_vertices - nb_remaining_commities - 1;
        System.out.println("Partition " + (G.nb_vertices - nb_remaining_commities)
                 + " (" + nb_remaining_commities + " communities)");
        for(int c = 0; c <= last_community; c++)
            if((communities[c].sub_community_of == 0) || (communities[c].sub_community_of > last_community))
                print_community(c);


    }

    void print_community(int c) {                // print a community
        System.out.println("community " + c + " = {");
        for (int m = communities[c].first_member; m != members[communities[c].last_member]; m = members[m]) {
            if (G.index != null) System.out.print(G.index[m]);
            else System.out.print(m);
            if (members[m] != members[communities[c].last_member]) System.out.print(", ");
        }
        System.out.println("}");
    }

}

