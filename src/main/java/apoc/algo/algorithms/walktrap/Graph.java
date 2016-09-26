package apoc.algo.algorithms.walktrap;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;

public class Graph {
    public int nb_vertices;		// number of vertices
    public int nb_edges;			// number of edges
    public float total_weight;		// total weight of the edges
    public Vertex vertices[];		// array of the vertices
    private Edge_list EL;
    private int max_vertex;
    public static boolean compare(Edge E1, Edge E2) {
        return (E1.neighbor < E2.neighbor);
    }

    public long memory() {            // the total memory used in Bytes
        assert false;
        return 0;
    }
    public Graph() {            // create an empty graph
        nb_vertices = 0;
        nb_edges = 0;
        vertices = null;
        index = null;
        total_weight = 0.f;
        max_vertex = 0;
        EL = new Edge_list();
    }
    public String index[];			// to keep the real name of the vertices

    public boolean load_index(String input_file) {
        assert false;
        return false;
    }

    public boolean addEdgeList(int v1, int v2, float w) {
        if(v1 > max_vertex) max_vertex = v1;
        if(v2 > max_vertex) max_vertex = v2;

        if((v1 < 0) || (v2 < 0)) {
            System.err.print("error : negative vertex number");
            return false;
        }
        if(w < 0) {
            System.err.print("error : negative weight number");
            return false;
        }
        EL.add(v1, v2, w);
        return true;
    }

    public boolean processGraph() {
        Graph G = this;
        if(G.vertices != null) G.vertices = null;
        System.out.println("Max vertex: " + G.max_vertex + " total edgeList:" + G.EL.size);
        System.out.println("Atul 1 " + G.nb_vertices);

        G.nb_vertices = max_vertex + 1;
        G.vertices = new Vertex[G.nb_vertices];
        for (int i = 0; i < G.vertices.length; i++) {
            G.vertices[i] = new Vertex();
        }
        G.nb_edges = 0;
        G.total_weight = 0.f;
        System.out.println("Atul 2 " + EL.size);

        for(int i = 0; i < EL.size; i++) {
            int x = EL.V1[i];

            G.vertices[EL.V1[i]].degree++;

            G.vertices[EL.V2[i]].degree++;
            G.vertices[EL.V1[i]].total_weight += EL.W[i];
            G.vertices[EL.V2[i]].total_weight += EL.W[i];
            G.nb_edges++;
            G.total_weight += EL.W[i];
        }
        System.out.println("Atul 3");

        for(int i = 0; i < G.nb_vertices; i++) {
            if(G.vertices[i].degree == 0) {
                System.err.print("error : degree of vertex " + i + " is 0");
            return false;
            }
            G.vertices[i].edges = new Edge[G.vertices[i].degree + 1];
            for (int j = 0; j < G.vertices[i].edges.length; j++) {
                G.vertices[i].edges[j] = new Edge();
            }
            G.vertices[i].edges[0].neighbor = i;
            G.vertices[i].edges[0].weight = G.vertices[i].total_weight/(G.vertices[i].degree);
            G.vertices[i].total_weight+= G.vertices[i].total_weight/(G.vertices[i].degree);
            G.vertices[i].degree = 1;
        }

        for(int i = 0; i < EL.size; i++) {
            G.vertices[EL.V1[i]].edges[G.vertices[EL.V1[i]].degree].neighbor = EL.V2[i];
            G.vertices[EL.V1[i]].edges[G.vertices[EL.V1[i]].degree].weight = EL.W[i];
            G.vertices[EL.V1[i]].degree++;
            G.vertices[EL.V2[i]].edges[G.vertices[EL.V2[i]].degree].neighbor = EL.V1[i];
            G.vertices[EL.V2[i]].edges[G.vertices[EL.V2[i]].degree].weight = EL.W[i];
            G.vertices[EL.V2[i]].degree++;
        }
        System.out.println("Atul 5");

        for(int i = 0; i < G.nb_vertices; i++)
            Arrays.sort(G.vertices[i].edges, new Comparator<Edge>() {
                @Override
                public int compare(Edge o1, Edge o2) {
                    return (o1.neighbor < o2.neighbor) ? 1 : 0;
                }
            });
        System.out.println("Atul 6");

        for(int i = 0; i < G.nb_vertices; i++) {  // merge multi edges
            int a = 0;
            for(int b = 1; b < G.vertices[i].degree; b++) {
                if(G.vertices[i].edges[b].neighbor == G.vertices[i].edges[a].neighbor)
                    G.vertices[i].edges[a].weight += G.vertices[i].edges[b].weight;
                else
                    G.vertices[i].edges[++a] = G.vertices[i].edges[b];
            }
            G.vertices[i].degree = a+1;
        }
        System.out.println("Atul 7");


        return true;

    }
    class Edge_list {
        public int V1[];
        public int V2[];
        public float W[];

        int size;
        int size_max;

        void add(int v1, int v2, float w) {
            if(size == size_max) {
                int tmp1[] = new int[2*size_max];
                int tmp2[] = new int[2*size_max];
                float tmp3[] = new float[2*size_max];
                for(int i = 0; i < size_max; i++) {
                    tmp1[i] = V1[i];
                    tmp2[i] = V2[i];
                    tmp3[i] = W[i];
                }
                V1 = tmp1;
                V2 = tmp2;
                W = tmp3;
                size_max *= 2;
            }
            V1[size] = v1;
            V2[size] = v2;
            W[size] = w;
            size++;
        }
        public Edge_list() {
            size = 0;
            size_max = 1024;
            V1 = new int[1024];
            V2 = new int[1024];
            W = new float[1024];
        }
    };

}
