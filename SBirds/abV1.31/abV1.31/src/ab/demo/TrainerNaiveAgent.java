/*****************************************************************************
 ** ANGRYBIRDS AI AGENT FRAMEWORK
 ** Copyright (c) 2014, XiaoYu (Gary) Ge, Stephen Gould, Jochen Renz
 **  Sahan Abeyasinghe,Jim Keys,  Andrew Wang, Peng Zhang
 ** All rights reserved.
 **This work is licensed under the Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License. 
 **To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-sa/3.0/ 
 *or send a letter to Creative Commons, 444 Castro Street, Suite 900, Mountain View, California, 94041, USA.
 *****************************************************************************/
package ab.demo;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import ab.Heuristic.HeuristicEngine;
import ab.demo.other.ActionRobot;
import ab.demo.other.ClientActionRobotJava;
import ab.demo.other.Shot;
import ab.learn.VectorQuantizer;
import ab.planner.TrajectoryPlanner;
import ab.utils.StateUtil;
import ab.vision.ABObject;
import ab.vision.ABType;
import ab.vision.GameStateExtractor;
import ab.vision.GameStateExtractor.GameState;
import ab.vision.Vision;

public class TrainerNaiveAgent implements Runnable {

    public static int shotn=0;
    private Rectangle _slingShot;
    static boolean isOkay=false;
    static int continuousLevelsPlayed = 0;
    static BufferedWriter write;
    static FileWriter file;
    private ClientActionRobotJava aRobot;
    private Random randomGenerator;
    public int currentLevel = 13;
    public static int time_limit = 12;
    private Map<Integer,Integer> scores = new LinkedHashMap<Integer,Integer>();
    TrajectoryPlanner tp;
    private boolean firstShot;
    private Point prevTarget;
//    public  static int flagBottomUp=0;
//    public  static int flagTopDown=0;
    public static double normalizedScore;
    

	/**
	 * Constructor using the default IP
	 * */
	public ClientNaiveAgent() {
		// the default ip is the localhost
		aRobot = new ClientActionRobotJava("127.0.0.1");
		tp = new TrajectoryPlanner();
		randomGenerator = new Random();
		prevTarget = null;
		firstShot = true;
		System.out.println("Enter");

	}
	/**
	 * Constructor with a specified IP
	 * */
	public ClientNaiveAgent(String ip) {
		aRobot = new ClientActionRobotJava(ip);
		tp = new TrajectoryPlanner();
		randomGenerator = new Random();
		prevTarget = null;
		firstShot = true;
		System.out.println("Enter");

	}
	public ClientNaiveAgent(String ip, int id)
	{
		aRobot = new ClientActionRobotJava(ip);
		tp = new TrajectoryPlanner();
		randomGenerator = new Random();
		prevTarget = null;
		firstShot = true;
		this.id = id;
		System.out.println("Enter");
	}
    
    
    
        // a standalone implementation of the Naive Agent
    public TrainerNaiveAgent(int level) {
    	currentLevel=level;
        aRobot = new ActionRobot();
        tp = new TrajectoryPlanner();
        prevTarget = null;
        firstShot = true;
        randomGenerator = new Random();
        // --- go to the Poached Eggs episode level selection page ---
        ActionRobot.GoFromMainMenuToLevelSelection();

    }

    public TrainerNaiveAgent(int level) {
    	currentLevel=level;
        aRobot = new ActionRobot();
        tp = new TrajectoryPlanner();
        prevTarget = null;
        firstShot = true;
        randomGenerator = new Random();
        // --- go to the Poached Eggs episode level selection page ---
        ActionRobot.GoFromMainMenuToLevelSelection();

    }

    public TrainerNaiveAgent() {
        aRobot = new ClientActionRobotJava();
        tp = new TrajectoryPlanner();
        prevTarget = null;
        firstShot = true;
        randomGenerator = new Random();
        // --- go to the Poached Eggs episode level selection page ---
        ActionRobot.GoFromMainMenuToLevelSelection();

    }

    public boolean isBottomUp()
    {
        return false;
    }

    // run the client
    public void run() {


        aRobot.loadLevel((byte)currentLevel);
        while (true) {
            GameState state = solve();
            if (state == GameState.WON) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int score = StateUtil.getScore(ActionRobot.proxy);
                if(!scores.containsKey(currentLevel))
                    scores.put(currentLevel, score);
                else
                {
                    if(scores.get(currentLevel) < score)
                        scores.put(currentLevel, score);
                }
                int totalScore = 0;
                for(Integer key: scores.keySet()){

                    totalScore += scores.get(key);
                    System.out.println(" Level " + key
                            + " Score: " + scores.get(key) + " ");
                }
                System.out.println("Total Score: " + totalScore);
                if(currentLevel==21)
                {
                    currentLevel=1;
                    aRobot.loadLevel((byte)currentLevel);
                }
                else {
                    aRobot.loadLevel((byte)++currentLevel);
                    // make a new trajectory planner whenever a new level is entered
                    tp = new TrajectoryPlanner();

                    // first shot on this level, try high shot first
                    firstShot = true;
                }
            } else if (state == GameState.LOST) {
                if(currentLevel==21)
                    currentLevel=0;

                aRobot.loadLevel((byte)++currentLevel);
            } else if (state == GameState.LEVEL_SELECTION) {
                System.out
                        .println("Unexpected level selection page, go to the last current level : "
                                + currentLevel);
                aRobot.loadLevel((byte)currentLevel);
            } else if (state == GameState.MAIN_MENU) {
                System.out
                        .println("Unexpected main menu page, go to the last current level : "
                                + currentLevel);
                ActionRobot.GoFromMainMenuToLevelSelection();
                aRobot.loadLevel((byte)currentLevel);
            } else if (state == GameState.EPISODE_MENU) {
                System.out
                        .println("Unexpected episode menu page, go to the last current level : "
                                + currentLevel);
                ActionRobot.GoFromMainMenuToLevelSelection();
                aRobot.loadLevel((byte)currentLevel);
            }

        }

    }

    private double distance(Point p1, Point p2) {
        return Math
                .sqrt((double) ((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y)
                        * (p1.y - p2.y)));
    }

    public GameState solve()
    {
        // capture Image
        BufferedImage screenShot = ActionRobot.doScreenShot();

        // process image
        Vision vision = new Vision(screenShot);

        // find the slingshot
        Rectangle sling = vision.findSlingshotMBR();

        // confirm the slingshot
        while (sling == null && aRobot.checkState() == GameState.PLAYING) {
            System.out
                    .println("No slingshot detected. Please remove pop up or zoom out");
            ActionRobot.fullyZoomOut();
            screenShot = ActionRobot.doScreenShot();
            vision = new Vision(screenShot);
            sling = vision.findSlingshotMBR();
        }
        // get all the pigs
        List<ABObject> pigs = vision.findPigsMBR();

        GameState state = aRobot.checkState();
        
        // if there is a sling, then play, otherwise just skip.
        if (sling != null) {

            if (!pigs.isEmpty()) {

                Point finalReleasePoint = null;
                GameStateExtractor stateExtractor = new GameStateExtractor();
                int beginScore = stateExtractor.getScoreInGame(screenShot);
                Shot shot = new Shot();
                int dx,dy;

                    // random pick up a pig
                    List<ABObject> allObjects = vision.getVisionRealShape().findObjects();
                   
                    for(ABObject o:allObjects)
                    {
                        System.out.println(o);
                    }
                    
//                    List<ABObject> wood = vision.getMBRVision().constructABObjects(vision.getMBRVision().findWoodMBR(), ABType.Wood);
//                    List<ABObject> ice = vision.getMBRVision().constructABObjects(vision.getMBRVision().findIceMBR(),ABType.Ice);
//                    List<ABObject> stone = vision.getMBRVision().constructABObjects(vision.getMBRVision().findStonesMBR(),ABType.Stone);
//                    List<ABObject> TNT = vision.getMBRVision().constructABObjects(vision.getMBRVision().findTNTsMBR(), ABType.TNT);
//                    List<ABObject> hill = findHill(vision.findBlocksRealShape());
//                    List<ABObject> birds = vision.findBirdsMBR();
//                    int bird_count = birds.size();

                    //HeuristicEngine he = new HeuristicEngine(sling,pigs,wood,ice,stone,TNT,hill,birds,aRobot.getBirdTypeOnSling());
                List<ABObject> hill = vision.getVisionRealShape().findHills();
                List<ABObject> Pigs = vision.getVisionRealShape().findPigs();
                HeuristicEngine he = new HeuristicEngine(sling,allObjects,pigs,hill,aRobot.getBirdTypeOnSling());
               //----------------------------------------------------------------------------
                //@created by Anchit Dave
                //Traverse the possible points on a particular block
                //Most feasible release point for the the particular block
                //Comparison of feasible release points with that of other feasible blocks + ALl the physics
                       //he.traverseAllReleasePoints(screenShot);
                       // allObjects = vision.getVisionRealShape().findObjects();
              /*  TrajectoryPlanner trajectory = new TrajectoryPlanner();
                System.out.println("Possible Points traversal");
                        for(ABObject o:allObjects)
                        {
                           List<Point> temp = he.traverseAllReleasePoints(o);
                           Point timepass = new Point(temp.get(0));
                           System.out.println(timepass);
                           System.out.println("For block "+o.id);
                           System.out.println(temp);
                           System.out.println();
                           //ArrayList<Point> releasePoints = trajectory.estimateLaunchPoint(_slingShot,new Point((int) timepass.getX(), (int) timepass.getY()));
                           //releasePoints = trajectory.estimateLaunchPoint(_slingShot,temp.get(2));
                           //System.out.println(releasePoints);
                        }
                       
                        System.out.println("Possible Points traversal complete");*/
           //----------------------------------------------------------------------------
                for(ABObject o:hill)
                	allObjects.add(o);
                for(ABObject o:Pigs)
                	allObjects.add(o);
                for(ABObject o:allObjects)
                	System.out.println(o);
                List<ABObject> wood  = new LinkedList<ABObject>();
                List<ABObject> ice = new LinkedList<ABObject>();
                List<ABObject> stone = new LinkedList<ABObject>();
                List<ABObject> TNT = new LinkedList<ABObject>();
                System.out.println("hill");

                System.out.println(hill);
                wood.addAll(findWood(allObjects));
                ice.addAll(findIce(allObjects));
                stone.addAll(findStone(allObjects));
                TNT.addAll(findTNT(allObjects));
                VectorQuantizer vq = new VectorQuantizer(pigs,wood,ice,stone,TNT);
                    Rectangle boundingRectangle = vq.getBoundingStructure();
                    double[][][] qunantizedStructure = vq.quantize(boundingRectangle);
                    he.generateAirBlocks(qunantizedStructure,boundingRectangle);
                    System.out.println("Airblocks done!!");
                    he.generateSubStructures();
                    System.out.println("substructures done!!");

                    he.calculateSupportWeight();
                    System.out.println("Suuport done!!");
                //he.downwardsFactor();
                    he.addAir();
                he.getDisplacement();
                System.out.println("displcement done!!");
                he.penetrationWeight();
                    System.out.println("All done!!");           
                    ArrayList<ABObject> finalCandidateBlocks = he.getFinalCandidateBlocks();
                    System.out.println("Before");
                    ABObject blockToHit = finalCandidateBlocks.get(0);
                    System.out.println("After");
                    System.out.println("Block to hit: "+blockToHit);
                    Point targetPoint = blockToHit.getCenter();
                    Point Left = blockToHit.getLeftEnd(targetPoint, blockToHit.width, blockToHit.height);
                    Point Right = blockToHit.getRightEnd(targetPoint, blockToHit.width, blockToHit.height);
                    System.out.println("-----------------------"+Left+"  "+Right+"  "+targetPoint+"  "+blockToHit.width+"  "+blockToHit.height);
                    List<Point> releasePoints;
                    //Right now bottom up so choose low angle shot
                    releasePoints = tp.estimateLaunchPoint(sling,targetPoint);
                    System.out.println("releasepoints="+releasePoints.size());
                    if(releasePoints.size()==0)
                    {

                        {
                            System.out.println("No release point found for the target");
                            System.out.println("Try a shot with 45 degree");
                            finalReleasePoint = tp.findReleasePoint(sling, Math.PI/4);
                        }

                    }
                    else {
                        if(releasePoints.size()==2 && blockToHit.isBottomUp)
                        {
                            finalReleasePoint=releasePoints.get(0);

                        }
                        else
                        {
                            if(releasePoints.size()==1)
                            finalReleasePoint=releasePoints.get(0);
                            else
                                finalReleasePoint=releasePoints.get(1);

                        }

                    }
                    // Get the reference point
                    Point refPoint = tp.getReferencePoint(sling);
//                    List<ABObject> inTheWay = new LinkedList();
 /*                   Point inTheWay = new Point();
                    Boolean bottom=true;
                    List<ABObject> hillInTheWayBottom = new LinkedList();
                    List<ABObject> hillInTheWayTop = new LinkedList();
//                    inTheWay = he.estimateObjectsInTheWay(sling, allObjects, targetPoint, blockToHit, releasePoints.get(0), 1);
                    hillInTheWayBottom = he.estimateObjectsInTheWay(sling, hill, targetPoint, blockToHit, releasePoints.get(0), 1);
                    hillInTheWayTop = he.estimateObjectsInTheWay(sling, hill, targetPoint, blockToHit, releasePoints.get(1), 1);
                    int leastPigX = -1;
                    for(ABObject o:allObjects)
                    {
                        if(o.type.equals(ABType.Pig)){
//                        	System.out.println(o.x);
                        	if(leastPigX==-1){
                        		leastPigX=o.x;
                        	}else if(o.x<leastPigX){
                        		leastPigX=o.x;
                        	}
                        }
                    }
                    
//                    ---------------++++++++++---------------
                    
                    if(hillInTheWayBottom.size()==0){
                    	finalReleasePoint= releasePoints.get(0);	
                    }else if(hillInTheWayBottom.size()!=0){
                    	int temp = (int)hillInTheWayBottom.get(0).getCenterX();
                    	for(ABObject o:hillInTheWayBottom){
                    		if(o.x<temp){
                    			temp=o.x;
                    			inTheWay = new Point(o.x-(o.width/2),o.y);
                    		}
                    		System.out.println("in the way"+inTheWay);
                    		if(inTheWay.x<leastPigX){
                        		System.out.println("Top----------xxxxx");

                    			bottom = false;
                    			finalReleasePoint = releasePoints.get(1);
                    		}
                    	}
                    	
                    }*/
                  /*  for(ABObject o:inTheWay)
                    System.out.println("---+++++---"+o);
                    
                    int leastPigX = -1;
                    for(ABObject o:allObjects)
                    {
                        if(o.type.equals(ABType.Pig)){
//                        	System.out.println(o.x);
                        	if(leastPigX==-1){
                        		leastPigX=o.x;
                        	}else if(o.x<leastPigX){
                        		leastPigX=o.x;
                        	}
                        }
                    }
//					System.out.println(leastPigX);

                    boolean shouldTopDown = false;
                    for (int i = 0; i < inTheWay.size(); i++) {
                    	ABObject currObject = inTheWay.get(i);
//						System.out.println(currObject);

						if(currObject.type.equals(ABType.Hill)||currObject.type.equals(ABType.Ground)){
							System.out.println(currObject.getCenterX());

							if(currObject.trajectoryHitPoint.getX()<leastPigX){
								System.out.println("WTF");

//								System.out.println(currObject.x);

								shouldTopDown=true;
								break;
							}
						}
					}*/
                    if(releasePoints.size()==0){
                            System.out.println("No release point found for the target");
                            System.out.println("Try a shot with 45 degree");
                            finalReleasePoint = tp.findReleasePoint(sling, Math.PI/4);
                    } /*else {
                        if(shouldTopDown&&releasePoints.size()>1){
                        	finalReleasePoint=releasePoints.get(1);
                        }else finalReleasePoint=releasePoints.get(0);
                    }*/
//                    System.out.println(finalReleasePoint);
                    finalReleasePoint=releasePoints.get(0);
                    //Calculate the tapping time according the bird type
                    if (finalReleasePoint != null) {
                        double releaseAngle = tp.getReleaseAngle(sling,
                                finalReleasePoint);
                        //System.out.println("Release Point: " + releasePoint);
                        System.out.println("Release Angle: "
                                + Math.toDegrees(releaseAngle));
                        int tapInterval = 0;
                        switch (aRobot.getBirdTypeOnSling())
                        {

                            case RedBird:
                                tapInterval = 0; break;               // start of trajectory
                            case YellowBird:
                                tapInterval = 70 + randomGenerator.nextInt(20);break; // 70-90% of the way
                            case WhiteBird:
                                tapInterval =  70 + randomGenerator.nextInt(20);break; // 70-90% of the way
                            case BlackBird:
                                tapInterval =  70 + randomGenerator.nextInt(20);break; // 70-90% of the way
                            case BlueBird:
                                tapInterval =  65 + randomGenerator.nextInt(20);break; // 65-85% of the way
                            default:
                                tapInterval =  60;
                        }
                        ABObject firstBlock = he.findTheFirstBlock(sling, allObjects, targetPoint, blockToHit, finalReleasePoint, 1);
                        int tapTime = tp.getTapTime(sling, finalReleasePoint, targetPoint, tapInterval, firstBlock);
                        dx = (int)finalReleasePoint.getX() - refPoint.x;
                        dy = (int)finalReleasePoint.getY() - refPoint.y;
                        shot = new Shot(refPoint.x, refPoint.y, dx, dy, 0, tapTime);
                    }
                    else
                    {
                        System.err.println("No Release Point Found");
                        return state;
                    }


                // check whether the slingshot is changed. the change of the slingshot indicates a change in the scale.
                {
                    ActionRobot.fullyZoomOut();
                    screenShot = ActionRobot.doScreenShot();
                    vision = new Vision(screenShot);
                    Rectangle _sling = vision.findSlingshotMBR();
                    if(_sling != null)
                    {
                        double scale_diff = Math.pow((sling.width - _sling.width),2) +  Math.pow((sling.height - _sling.height),2);
                        if(scale_diff < 25)
                        {
                            if(dx < 0)
                            {
                                aRobot.shoot(shot.getX(),shot.getY(),shot.getDx(),shot.getDy(),shot.getT_shot(),shot.getT_tap(), false);

                                state = aRobot.checkState();
                                if ( state == GameState.PLAYING )
                                {
                                    screenShot = ActionRobot.doScreenShot();
                                    vision = new Vision(screenShot);
                                    List<Point> traj = vision.findTrajPoints();
                                    tp.adjustTrajectory(traj, sling, finalReleasePoint);
                                    firstShot = false;
                                     stateExtractor = new GameStateExtractor();
                                    int finalScore = stateExtractor.getScoreInGame(screenShot);
                                    int scoreGained = finalScore - beginScore;
                                    int totalScore = he.getMaxScore();
                                    normalizedScore=100*(double)scoreGained/(double)totalScore;
                                    if(normalizedScore<=100){
                                    HeuristicEngine.trainCoefficients(100*(double)scoreGained/(double)totalScore,blockToHit.isBottomUp);
                                    String str = (++shotn)+" a:"+blockToHit.isBottomUp+" L:"+currentLevel+" ns:"+normalizedScore+" alb:"+HeuristicEngine.alphaBottomUp+" bb:"+HeuristicEngine.betaBottomUp+" g:"+HeuristicEngine.gamma+" bM:"+HeuristicEngine.bottomUpMean+" bsd+"+HeuristicEngine.bottomUpStandardDeviation+" fb+"+HeuristicEngine.flagBottomUp+" at:"+HeuristicEngine.alphaTopDown+" bt"+HeuristicEngine.betaTopDown+" d:"+HeuristicEngine.delta+" tm:"+HeuristicEngine.topDownMean+" tsd:"+HeuristicEngine.topDownStandardDeviation+" ft:"+HeuristicEngine.flagTopDown;
                                    try {
                                        write.write(str);
                                        write.newLine();
                                        write.flush();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }}

                                }
                                else if(state==GameState.WON)
                                {
                                    screenShot = ActionRobot.doScreenShot();
                                    stateExtractor = new GameStateExtractor();
                                    int finalScore = stateExtractor.getScoreEndGame(screenShot);
                                    int totalScore = he.getMaxScore();
                                    int scoreGained = finalScore - beginScore;
//                                    normalizedScore=100*(double)scoreGained/(double)totalScore;
//                                    HeuristicEngine.trainCoefficients(normalizedScore,blockToHit.isBottomUp);
//                                    String str = (++shotn)+" a:"+blockToHit.isBottomUp+" L:"+currentLevel+" ns:"+normalizedScore+" alb:"+HeuristicEngine.alphaBottomUp+" bb:"+HeuristicEngine.betaBottomUp+" g:"+HeuristicEngine.gamma+" bM:"+HeuristicEngine.bottomUpMean+" bsd+"+HeuristicEngine.bottomUpStandardDeviation+" fb+"+HeuristicEngine.flagBottomUp+" at:"+HeuristicEngine.alphaTopDown+" bt"+HeuristicEngine.betaTopDown+" d:"+HeuristicEngine.delta+" tm:"+HeuristicEngine.topDownMean+" tsd:"+HeuristicEngine.topDownStandardDeviation+" ft:"+HeuristicEngine.flagTopDown;
//                                    try {
//                                        write.write(str);
//                                        write.newLine();
//                                        write.flush();
//                                    } catch (IOException e) {
//                                        e.printStackTrace();
//                                    }
                                }
                            }
                        }
                        else
                            System.out.println("Scale is changed, can not execute the shot, will re-segement the image");
                    }
                    else
                        System.out.println("no sling detected, can not execute the shot, will re-segement the image");
                }


            }

        }
        return state;
    }

    public static List<ABObject> findHill(List<ABObject> objects)
    {
        List<ABObject> hills = new LinkedList<ABObject>();
        for(ABObject obj : objects )
        {
            if(obj.type==ABType.Hill)
                hills.add(obj);
        }
        return hills;
    }

    public List<ABObject> findWood(List<ABObject> objects)
    {
        List<ABObject> ans = new LinkedList<ABObject>();
        for(ABObject obj:objects)
        {
            if(obj.type==ABType.Wood)
                ans.add(obj);
        }
        return ans;
    }

    public List<ABObject> findIce(List<ABObject> objects)
    {
        List<ABObject> ans = new LinkedList<ABObject>();
        for(ABObject obj:objects)
        {
            if(obj.type==ABType.Ice)
                ans.add(obj);
        }
        return ans;
    }

    public List<ABObject> findStone(List<ABObject> objects)
    {
        List<ABObject> ans = new LinkedList<ABObject>();
        for(ABObject obj:objects)
        {
            if(obj.type==ABType.Stone)
                ans.add(obj);
        }
        return ans;
    }

    public List<ABObject> findTNT(List<ABObject> objects)
    {
        List<ABObject> ans = new LinkedList<ABObject>();
        for(ABObject obj:objects)
        {
            if(obj.type==ABType.TNT)
                ans.add(obj);
        }
        return ans;
    }


    public static void main(int level) {

        TrainerNaiveAgent na = new TrainerNaiveAgent(level);
            try {
                 file = new FileWriter("/Users/anchitdave/Downloads/SBirds/log.txt");
                 write = new BufferedWriter(file);
                while(HeuristicEngine.count<21 ||(HeuristicEngine.flagBottomUp==4 && HeuristicEngine.flagTopDown==4))
                {
                    na.run();
                    System.out.println("Hi");
                }

                na.run();
            } catch (IOException e) {
                e.printStackTrace();
            }

    }
}
