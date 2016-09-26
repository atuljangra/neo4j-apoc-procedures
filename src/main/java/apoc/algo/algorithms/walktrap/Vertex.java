package apoc.algo.algorithms.walktrap;

/**
 * Created by atul on 26/09/16.
 */
public class Vertex {
    public Edge edges[];			// the edges of the vertex
    public int degree;			// number of neighbors
    public float total_weight;		// the total weight of the vertex

    public Vertex() {            // creates empty vertex
        degree = 0;
        edges = null;
        total_weight = 0.f;
    }
}
