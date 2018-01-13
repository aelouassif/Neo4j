package fr.insa;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.neo4j.graphalgo.*;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.Label;

import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.io.fs.FileUtils;
import org.neo4j.graphdb.DynamicLabel;

public class App
{
    private int line = 900,col = 1800 ;
    private String csvFile = "data/probability-5.csv";
    private static final File databaseDirectory = new File( "target/neo4j-db" );
    // our data
    Double[][] data = new CSVReader(csvFile,col,line).getData();
    // our graph
    GraphDatabaseService graphDb;
    Node[][] nodes = new Node[line][col];
    Label Point = Label.label( "Point" );

    //createReltype
    private static enum RelTypes implements RelationshipType{
        NEAR
    }

    public static void main( final String[] args ) throws IOException{
        App app = new App();
        app.createDb(0.05);

//        Point start = new Point(250,3);
//        Point end = new Point(500,255);

//      find the shortest path with dijkstra
//        WeightedPath path = app.dijkstra(start,end);
//        app.displayNodes(path);

        app.shutDown();
    }

    public App() throws IOException{
//        if we want create a new graph
        FileUtils.deleteRecursively( databaseDirectory );
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(databaseDirectory)
                .setConfig(GraphDatabaseSettings.pagecache_memory, "20G").newGraphDatabase();
        registerShutdownHook( graphDb );
    }

    void createDb(Double eps){
        try( Transaction tx = graphDb.beginTx() ){
//          create nodes
            for(int y=0;y<line;y++){
                for(int x=0;x<col;x++){
                    try{
                        if(data[y][x]>eps){
                            Node node = graphDb.createNode();
                            node.setProperty("ID",x+":"+y);
                            node.setProperty("x",x);
                            node.setProperty("y",y);
                            node.addLabel( Point );
                            nodes[y][x] = node;
                        }
                        else
                            nodes[y][x] = null;
                    }
                    catch (Exception ex){
                        System.out.println(ex);
                        continue;
                    }
                }
            }
//          create edges
            for(int y=0;y<line;y++){
                for(int x=0;x<col;x++){
                    Relationship relationship;
                    try{
                        System.out.println(x+" "+y);
                        if(nodes[y][x]==null)
                            continue;
                        //2 points in the same line
                        if(nodes[y+1][x] !=null){
                            relationship = (nodes[y][x]).createRelationshipTo((nodes[y+1][x]),RelTypes.NEAR);
                            relationship.setProperty("probability",1-data[y+1][x]);
                        }
                        if(nodes[y+1][x]!=null){
                            relationship = (nodes[y+1][x]).createRelationshipTo((nodes[y][x]),RelTypes.NEAR);
                            relationship.setProperty("probability",1-data[y][x]);
                        }

                        //2 points in the same column
                        if(nodes[y][x+1]!=null){
                            relationship = (nodes[y][x]).createRelationshipTo((nodes[y][x+1]),RelTypes.NEAR);
                            relationship.setProperty("probability",1-data[y][x+1]);
                        }
                        if(nodes[y][x]!=null){
                            relationship = (nodes[y][x+1]).createRelationshipTo((nodes[y][x]),RelTypes.NEAR);
                            relationship.setProperty("probability",1-data[y][x]);
                        }

                        //2 points in the same diagonal
                        if(nodes[y+1][x+1]!=null){
                            relationship = (nodes[y][x]).createRelationshipTo((nodes[y+1][x+1]),RelTypes.NEAR);
                            relationship.setProperty("probability",1-data[y+1][x+1]);
                        }
                        if(nodes[y][x]!=null){
                            relationship = (nodes[y+1][x+1]).createRelationshipTo((nodes[y][x]),RelTypes.NEAR);
                            relationship.setProperty("probability",1-data[y][x]);
                        }

                        //2 points in the same anti-diagonal
                        if(nodes[y+1][x-1]!=null){
                            relationship = (nodes[y][x]).createRelationshipTo((nodes[y+1][x-1]),RelTypes.NEAR);
                            relationship.setProperty("probability",1-data[y+1][x-1]);
                        }
                        if(nodes[y][x]!=null){
                            relationship = (nodes[y+1][x-1]).createRelationshipTo((nodes[y][x]),RelTypes.NEAR);
                            relationship.setProperty("probability",1-data[y][x]);
                        }

                    }
                    catch (Exception ex){
                        System.out.println(ex);
                        continue;
                    }
                }
            }


            //test
//            ResourceIterator<Node> points = graphDb.findNodes(Point,"x",10);
//            while ( points.hasNext() )
//            {
//                Node point = points.next();
//                System.out.println(point.getProperties("y"));
//                for ( Relationship actedIn : point.getRelationships( RelationshipType.withName( "NEAR" ), Direction.OUTGOING ) )
//                {
//                    Node endNode = actedIn.getEndNode();
//                    System.out.println(endNode.getProperty("y"));
//                }
//                System.out.println(point.getRelationships());
//            }

            tx.success();
        }
    }

    public WeightedPath dijkstra(Point start,Point end) {

        WeightedPath path;
        try (Transaction tx = graphDb.beginTx()) {
            PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra(
                    PathExpanders.forTypeAndDirection( RelTypes.NEAR, Direction.BOTH ), "probability" );

            Node startNode = graphDb.findNode(Point,"id",start.getX()+":"+start.getY());
            Node endNode = graphDb.findNode(Point,"id",end.getX()+":"+end.getY());
            path = finder.findSinglePath(startNode,endNode );

            // Get the weight for the found path
            System.out.println(path.weight());
            tx.success();
        }
        return path;
    }

    public void displayNodes(WeightedPath path){
        Iterable<Node> nodes = path.nodes();

        try (Transaction tx = graphDb.beginTx()) {
            for (Node node: nodes) {
                System.out.println(node.getProperty("x")+":"+node.getProperty("y"));
            }

            tx.success();
        }
    }

    void shutDown()
    {
        System.out.println();
        System.out.println( "Shutting down database ..." );
        // START SNIPPET: shutdownServer
        graphDb.shutdown();
        // END SNIPPET: shutdownServer
    }

    // START SNIPPET: shutdownHook
    private static void registerShutdownHook( final GraphDatabaseService graphDb )
    {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                graphDb.shutdown();
            }
        } );
    }


}