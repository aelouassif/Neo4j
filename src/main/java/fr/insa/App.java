package fr.insa;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.Label;

import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.io.fs.FileUtils;
import org.neo4j.graphdb.DynamicLabel;

public class App
{
    private static final File databaseDirectory = new File( "target/neo4j-db" );
    // our data
    ArrayList<ArrayList<Double>> data = CSVReader.getData("/media/abdellah/data/5SDBD/Project/data/density-map-from-20160101-to-20161231-5-probability.csv");
    // our graph
    GraphDatabaseService graphDb;
    ArrayList<ArrayList<Node>> nodes = new ArrayList<ArrayList<Node>>();
    Label Point = Label.label( "Point" );

    //createReltype
    private static enum RelTypes implements RelationshipType
    {
        NEAR
    }

    public static void main( final String[] args ) throws IOException
    {
        App app = new App();
//        app.createDb();

//        find the shortest path with dijkstra
        Point start = new Point(0,0);
        Point end = new Point(49,49);
        WeightedPath path = app.dijkstra(start,end);


        app.shutDown();
    }

    public App() throws IOException{
//        if we want create a new graph
//        FileUtils.deleteRecursively( databaseDirectory );

        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( databaseDirectory );
        registerShutdownHook( graphDb );
    }

    void createDb(){

        try( Transaction tx = graphDb.beginTx() )
        {
//          create nodes
            for(int x=0;x<50;x++){
                nodes.add(new ArrayList<Node>());
                for(int y=0;y<50;y++){
                    Node node = graphDb.createNode();
                    node.setProperty("id",x+":"+y);
                    node.setProperty("x",x);
                    node.setProperty("y",y);
                    node.addLabel( Point );
                    nodes.get(x).add(node);
                }
            }
//          create edges
            for(int x=0;x<50;x++){
                for(int y=0;y<50;y++){
                    Relationship relationship;
                    try{
                        //2 points in the same line
                        relationship = (nodes.get(x).get(y)).createRelationshipTo((nodes.get(x).get(y+1)),RelTypes.NEAR);
                        relationship.setProperty("probability",1-data.get(x).get(y+1));
                        relationship = (nodes.get(x).get(y+1)).createRelationshipTo((nodes.get(x).get(y)),RelTypes.NEAR);
                        relationship.setProperty("probability",1-data.get(x).get(y));

                        //2 points in the same column
                        relationship = (nodes.get(x).get(y)).createRelationshipTo((nodes.get(x+1).get(y)),RelTypes.NEAR);
                        relationship.setProperty("probability",1-data.get(x+1).get(y));
                        relationship = (nodes.get(x+1).get(y)).createRelationshipTo((nodes.get(x).get(y)),RelTypes.NEAR);
                        relationship.setProperty("probability",1-data.get(x).get(y));

                        //2 points in the same diagonal
                        relationship = (nodes.get(x).get(y)).createRelationshipTo((nodes.get(x+1).get(y+1)),RelTypes.NEAR);
                        relationship.setProperty("probability",1-data.get(x+1).get(y+1));
                        relationship = (nodes.get(x+1).get(y+1)).createRelationshipTo((nodes.get(x).get(y)),RelTypes.NEAR);
                        relationship.setProperty("probability",1-data.get(x).get(y));

                        //2 points in the same anti-diagonal
                        relationship = (nodes.get(x).get(y)).createRelationshipTo((nodes.get(x-1).get(y+1)),RelTypes.NEAR);
                        relationship.setProperty("probability",1-data.get(x-1).get(y+1));
                        relationship = (nodes.get(x-1).get(y+1)).createRelationshipTo((nodes.get(x).get(y)),RelTypes.NEAR);
                        relationship.setProperty("probability",1-data.get(x).get(y));
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

//    void removeData()
//    {
//        try
//        {
//            Transaction tx = graphDb.beginTx();
//            // START SNIPPET: removingData
//            // let's remove the data
//            firstNode.getSingleRelationship( RelTypes.KNOWS, Direction.OUTGOING ).delete();
//            firstNode.delete();
//            secondNode.delete();
//            // END SNIPPET: removingData
//
//            tx.success();
//        }
//        catch (Exception ex){
//
//        }
//    }

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