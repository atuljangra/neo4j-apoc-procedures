package apoc.algo;
import apoc.Description;
import apoc.Pools;
import apoc.algo.algorithms.*;
import apoc.algo.algorithms.walktrap.WalktrapRunner;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

public class Walktrap {

    @Context
    public GraphDatabaseService db;

    @Context
    public Log log;

    @Context
    public GraphDatabaseAPI dbAPI;

    static final ExecutorService pool = Pools.DEFAULT;

    @Procedure("apoc.algo.walktrap")
    @Description("CALL apoc.algo.walktrap(node_cypher,rel_cypher,write) - run walktrap " +
            " community detection based on cypher input")
    public Stream<apoc.algo.algorithms.AlgorithmInterface.Statistics> walktrap(
            @Name("config") Map<String, Object> config) {
        String nodeCypher = AlgoUtils.getCypher(config, AlgoUtils.SETTING_CYPHER_NODE, AlgoUtils.DEFAULT_CYPHER_NODE);
        String relCypher = AlgoUtils.getCypher(config, AlgoUtils.SETTING_CYPHER_REL, AlgoUtils.DEFAULT_CYPHER_REL);
        boolean shouldWrite = (boolean)config.getOrDefault(AlgoUtils.SETTING_WRITE, AlgoUtils.DEFAULT_PAGE_RANK_WRITE);

        long beforeReading = System.currentTimeMillis();
        log.info("Walktrap: Reading data into local ds " + dbAPI + " |||| " + pool + " ||||| " + log );
        WalktrapRunner walktrapRunner=
                new WalktrapRunner(dbAPI, pool, log);

        boolean success = walktrapRunner.readNodeAndRelCypherData(
                relCypher, nodeCypher);
        if (!success) {
            String errorMsg = "Failure while reading cypher queries. Make sure the results are ordered.";
            log.info(errorMsg);
            throw new RuntimeException(errorMsg);
        }
        long afterReading = System.currentTimeMillis();

        log.info("Walktrap: Graph stored in local ds in " + (afterReading - beforeReading) + " milliseconds");
        log.info("Walktrap: Number of nodes: " + walktrapRunner.numberOfNodes());
        log.info("Walktrap: Number of relationships: " + walktrapRunner.numberOfRels());


        walktrapRunner.compute();

        long afterComputation = System.currentTimeMillis();
        log.info("Walktrap: Computations took " + (afterComputation - afterReading) + " milliseconds");

        if (shouldWrite) {
            walktrapRunner.writeResultsToDB();
            long afterWrite = System.currentTimeMillis();
            log.info("Walktrap: Writeback took " + (afterWrite - afterComputation) + " milliseconds");
        }

        return Stream.of(walktrapRunner.getStatistics());

    }
}

