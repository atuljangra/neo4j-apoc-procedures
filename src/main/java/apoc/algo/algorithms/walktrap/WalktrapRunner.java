package apoc.algo.algorithms.walktrap;

import apoc.algo.algorithms.AlgoUtils;
import apoc.algo.algorithms.Algorithm;
import apoc.algo.algorithms.AlgorithmInterface;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.logging.Log;

import java.util.concurrent.ExecutorService;

public class WalktrapRunner implements AlgorithmInterface {
    private Algorithm algorithm;
    public static final int WRITE_BATCH=100_000;
    private Log log;
    GraphDatabaseAPI db;
    ExecutorService pool;
    private int nodeCount;
    private int relCount;
    private Statistics stats = new Statistics();

    public WalktrapRunner(GraphDatabaseAPI dbAPI, ExecutorService pool, Log log) {
        this.pool = pool;
        this.db = dbAPI;
        this.log = log;
        algorithm = new Algorithm(db, pool, log);
    }

    @Override
    public double getResult(long node) {
        return 0;
    }

    @Override
    public long numberOfNodes() {
        return nodeCount;
    }

    @Override
    public String getPropertyName() {
        return "walktrap_community";
    }

    @Override
    public int getMappedNode(int index) {
        int node = algorithm.nodeMapping[index];
        return node;
    }

    public boolean readNodeAndRelCypherData(String relCypher, String nodeCypher) {
        boolean success = algorithm.readNodesAndRelCypherWeighted(relCypher, nodeCypher);
        this.nodeCount = algorithm.nodeCount;
        this.relCount = algorithm.relCount;
        stats.readNodeMillis = algorithm.readNodeMillis;
        stats.readRelationshipMillis = algorithm.readRelationshipMillis;
        stats.nodes = nodeCount;
        stats.relationships = relCount;
        return success;
    }

    public long numberOfRels() {
        return relCount;
    }

    public void compute() {
        long before = System.currentTimeMillis();
        int length = 4;
        boolean silent = false;
        long max_memory = -1;
        int details = 5;
        log.info("Going to create the graph");
        Graph G = new Graph();
        for (int i = 0; i < nodeCount; i++) {
            int chunkIndex = algorithm.sourceChunkStartingIndex[i];
            int degree = algorithm.sourceDegreeData[i];

            for (int j = 0; j < degree; j++) {
                int source = i;
                int target = algorithm.relationshipTarget[chunkIndex + j];
                int weight = algorithm.relationshipWeight[chunkIndex + j];
                G.addEdgeList(source, target, weight);
            }
        }
        log.info("Added all the edges to edge list");
        G.processGraph();

        log.info("Graph created " + G.nb_edges + " " + G.nb_vertices);

        Communities C = new Communities(G, length, silent, details, max_memory);
        log.info("Community instantiated");

        while (!C.H.is_empty()) {
            System.out.printf("Heap contains " + C.H.size);
            System.out.printf("Merging communities "  +  C.nb_active_communities  + "  " + C.nb_communities);
            C.merge_nearest_communities();
        }

        if(true)
            C.print_best_modularity_partition();


        for (int i = 0; i < G.nb_vertices;i++) {
            log.info(i + "\t" + C.matrix[i][0] + "\t" + C.matrix[i][1]);
        }

        long after = System.currentTimeMillis();
        long difference = after - before;
        log.info("Computations took " + difference + " milliseconds");
        stats.computeMillis = difference;
    }

    public void writeResultsToDB() {
        stats.write = true;
        long before = System.currentTimeMillis();
        AlgoUtils.writeBackResults(pool, db, this, WRITE_BATCH);
        stats.writeMillis = System.currentTimeMillis() - before;
        stats.property = getPropertyName();
    }

    public Statistics getStatistics() {
        return stats;
    }
}
