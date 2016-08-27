package apoc.algo;

import apoc.algo.pagerank.*;
import apoc.algo.pagerank.PageRank;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.logging.Log;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class Algorithm {

    public static final int INITIAL_ARRAY_SIZE=100_000;
    private final GraphDatabaseAPI db;
    private final Log log;
    private final ExecutorService pool;
    private int nodeCount;
    private int relCount;

    private AlgorithmStatistics stats = new AlgorithmStatistics();

    // Arrays to hold the graph.
    // Mapping from Algo node ID to Graph nodeID
    public int [] nodeMapping;

    // Degree of each algo node.
    public int [] sourceDegreeData;

    // Starting index in relationship arrays for each algo node.
    public int [] sourceChunkStartingIndex;

    // Storing relationships
    public int [] relationshipTarget;
    public int [] relationshipWeight;

    public Algorithm(GraphDatabaseAPI db,
                     ExecutorService pool,
                     Log log) {
        this.pool = pool;
        this.db = db;
        this.log = log;
    }

    public boolean readNodeAndRelCypherIntoArrays(String relCypher, String nodeCypher) {
        Result nodeResult = db.execute(nodeCypher);

        long before = System.currentTimeMillis();
        ResourceIterator<Object> resultIterator = nodeResult.columnAs("id");
        int index = 0;
        int totalNodes = 0;
        nodeMapping = new int[INITIAL_ARRAY_SIZE];
        int currentSize = INITIAL_ARRAY_SIZE;
        while(resultIterator.hasNext()) {
            int node  = ((Long)resultIterator.next()).intValue();

            if (index >= currentSize) {
                if (log.isDebugEnabled()) log.debug("Node Doubling size " + currentSize);
                nodeMapping = doubleSize(nodeMapping, currentSize);
                currentSize = currentSize * 2;
            }
            nodeMapping[index] = node;
            index++;
            totalNodes++;
        }

        this.nodeCount = totalNodes;
        Arrays.sort(nodeMapping, 0, nodeCount);
        long after = System.currentTimeMillis();
        stats.readNodeMillis = (after - before);
        stats.nodes = totalNodes;
        log.info("Time to make nodes structure = " + stats.readNodeMillis + " millis");
        before = System.currentTimeMillis();

        sourceDegreeData = new int[totalNodes];

//        previousPageRanks = new int[totalNodes];
//        pageRanksAtomic = new AtomicIntegerArray(totalNodes);
        sourceChunkStartingIndex = new int[totalNodes];
        Arrays.fill(sourceChunkStartingIndex, -1);

        int totalRelationships = readRelationshipMetadata(relCypher);
        this.relCount = totalRelationships;
        relationshipTarget = new int[totalRelationships];
        relationshipWeight = new int[totalRelationships];
        Arrays.fill(relationshipTarget, -1);
        Arrays.fill(relationshipWeight, -1);
        calculateChunkIndices();
        readRelationships(relCypher, totalRelationships);
        after = System.currentTimeMillis();
        stats.relationships = totalRelationships;
        stats.readRelationshipMillis = (after - before);
        log.info("Time for iteration over " + totalRelationships + " relations = " + stats.readRelationshipMillis + " millis");
        return true;
    }

    public AlgorithmStatistics getStatistics() {
        return stats;
    }

    private int getNodeIndex(int node) {
        int index = Arrays.binarySearch(nodeMapping, 0, nodeCount, node);
        return index;
    }

    // TODO Create buckets instead of copying data.
    // Not doing it right now because of the complications of the interface.
    private int [] doubleSize(int [] array, int currentSize) {
        int newArray[] = new int[currentSize * 2];
        System.arraycopy(array, 0, newArray, 0, currentSize);
        return newArray;
    }


    private void calculateChunkIndices() {
        int currentIndex = 0;
        for (int i = 0; i < nodeCount; i++) {
            sourceChunkStartingIndex[i] = currentIndex;
            if (sourceDegreeData[i] == -1)
                continue;
            currentIndex += sourceDegreeData[i];
        }
    }

    private int readRelationshipMetadata(String relCypher) {
        long before = System.currentTimeMillis();
        Result result = db.execute(relCypher);
        int totalRelationships = 0;
        int sourceIndex = 0;
        while(result.hasNext()) {
            Map<String, Object> res = result.next();
            int source = ((Long) res.get("source")).intValue();
            sourceIndex = getNodeIndex(source);

            sourceDegreeData[sourceIndex]++;
            totalRelationships++;
        }
        result.close();
        long after = System.currentTimeMillis();
        log.info("Time to read relationship metadata " + (after - before) + " ms");
        return totalRelationships;
    }

    private void readRelationships(String relCypher, int totalRelationships) {
        Result result = db.execute(relCypher);
        long before = System.currentTimeMillis();
        int sourceIndex = 0;
        while(result.hasNext()) {
            Map<String, Object> res = result.next();
            int source = ((Long) res.get("source")).intValue();
            sourceIndex = getNodeIndex(source);
            int target = ((Long) res.get("target")).intValue();
            int weight = ((Long) res.getOrDefault("weight", 1)).intValue();
            int logicalTargetIndex = getNodeIndex(target);
            int chunkIndex = sourceChunkStartingIndex[sourceIndex];
            while(relationshipTarget[chunkIndex] != -1) {
                chunkIndex++;
            }
            relationshipTarget[chunkIndex] = logicalTargetIndex;
            relationshipWeight[chunkIndex] = weight;
        }
        result.close();
        long after = System.currentTimeMillis();
        log.info("Time to read relationship data " + (after - before) + " ms");
    }

    class AlgorithmStatistics {
        public long nodes, relationships, readNodeMillis, readRelationshipMillis;
        public boolean write;

        public AlgorithmStatistics(long nodes, long relationships, long readNodeMillis, long readRelationshipMillis) {
            this.nodes = nodes;
            this.relationships = relationships;
            this.readNodeMillis = readNodeMillis;
            this.readRelationshipMillis = readRelationshipMillis;
        }

        public AlgorithmStatistics() {
        }
    }
}
