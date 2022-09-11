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
import java.io.File;
import java.util.*;

import ab.Heuristic.HeuristicEngine;
import ab.demo.other.ActionRobot;
import ab.demo.other.Shot;
import ab.learn.VectorQuantizer;
import ab.planner.TrajectoryPlanner;
import ab.utils.StateUtil;
import ab.vision.ABObject;
import ab.vision.ABType;
import ab.vision.GameStateExtractor.GameState;
import ab.vision.Vision;

public class NaiveAgent implements Runnable {

	private ActionRobot aRobot;
	private Random randomGenerator;
	public int currentLevel = 1;
	public static int time_limit = 12;
	private Map<Integer, Integer> scores = new LinkedHashMap<Integer, Integer>();
	TrajectoryPlanner tp;
	private boolean firstShot;
	private Point prevTarget;
	public static int flag = 0;
	public ArrayList<Integer> levelHitBlocks = new ArrayList<Integer>();

	// a standalone implementation of the Naive Agent
	public NaiveAgent() {

		aRobot = new ActionRobot();
		tp = new TrajectoryPlanner();
		prevTarget = null;
		firstShot = true;
		randomGenerator = new Random();
		// --- go to the Poached Eggs episode level selection page ---
		ActionRobot.GoFromMainMenuToLevelSelection();

	}

	public boolean isBottomUp() {
		return false;
	}

	// run the client
	public void run() {

		aRobot.loadLevel(currentLevel);
		while (true) {
			GameState state = solve();
			if (state == GameState.WON) {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				int score = StateUtil.getScore(ActionRobot.proxy);

				if (!scores.containsKey(currentLevel))
					scores.put(currentLevel, score);
				else {
					if (scores.get(currentLevel) < score)
						scores.put(currentLevel, score);
				}
				int totalScore = 0;
				for (Integer key : scores.keySet()) {

					totalScore += scores.get(key);
					System.out.println(" Level " + key + " Score: " + scores.get(key) + " ");
				}
				System.out.println("Total Score: " + totalScore);
				aRobot.loadLevel(++currentLevel);
				// make a new trajectory planner whenever a new level is entered
				tp = new TrajectoryPlanner();

				// first shot on this level, try high shot first
				firstShot = true;
			} else if (state == GameState.LOST) {
				System.out.println("Restart");
				aRobot.restartLevel();
			} else if (state == GameState.LEVEL_SELECTION) {
				System.out.println("Unexpected level selection page, go to the last current level : " + currentLevel);
				aRobot.loadLevel(currentLevel);
			} else if (state == GameState.MAIN_MENU) {
				System.out.println("Unexpected main menu page, go to the last current level : " + currentLevel);
				ActionRobot.GoFromMainMenuToLevelSelection();
				aRobot.loadLevel(currentLevel);
			} else if (state == GameState.EPISODE_MENU) {
				System.out.println("Unexpected episode menu page, go to the last current level : " + currentLevel);
				ActionRobot.GoFromMainMenuToLevelSelection();
				aRobot.loadLevel(currentLevel);
			}

		}

	}

	private double distance(Point p1, Point p2) {
		return Math.sqrt((double) ((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y)));
	}

	public GameState solve() {

		// capture Image
		BufferedImage screenShot = ActionRobot.doScreenShot();

		// process image
		Vision vision = new Vision(screenShot);

		// find the slingshot
		Rectangle sling = vision.findSlingshotMBR();

		// confirm the slingshot
		while (sling == null && aRobot.getState() == GameState.PLAYING) {
			System.out.println("No slingshot detected. Please remove pop up or zoom out");
			ActionRobot.fullyZoomOut();
			screenShot = ActionRobot.doScreenShot();
			vision = new Vision(screenShot);
			sling = vision.findSlingshotMBR();
		}
		// get all the pigs
		List<ABObject> pigs = vision.findPigsMBR();

		GameState state = aRobot.getState();

		// if there is a sling, then play, otherwise just skip.
		if (sling != null) {

			if (!pigs.isEmpty()) {

				Point finalReleasePoint = null;
				Shot shot = new Shot();
				int dx, dy;
				{
					// random pick up a pig
					List<ABObject> wood = vision.getMBRVision().constructABObjects(vision.getMBRVision().findWoodMBR(),
							ABType.Wood);
					List<ABObject> ice = vision.getMBRVision().constructABObjects(vision.getMBRVision().findIceMBR(),
							ABType.Ice);
					List<ABObject> stone = vision.getMBRVision()
							.constructABObjects(vision.getMBRVision().findStonesMBR(), ABType.Stone);
					List<ABObject> TNT = vision.getMBRVision().constructABObjects(vision.getMBRVision().findTNTsMBR(),
							ABType.TNT);
					List<ABObject> hill = findHill(vision.findBlocksRealShape());
					List<ABObject> birds = vision.findBirdsMBR();
					int bird_count = birds.size();

					HeuristicEngine he = new HeuristicEngine(sling, pigs, wood, ice, stone, TNT, hill, birds,
							aRobot.getBirdTypeOnSling());
					VectorQuantizer vq = new VectorQuantizer(pigs, wood, ice, stone, TNT);
					Rectangle boundingRectangle = vq.getBoundingStructure();
					double[][][] qunantizedStructure = vq.quantize(boundingRectangle);
					he.generateAirBlocks(qunantizedStructure, boundingRectangle);
					he.calculateSupportWeight();
					he.downwardsFactor();
					he.getDisplacement();
					he.penetrationWeight();
					ArrayList<ABObject> finalCandidateBlocks = he.getFinalCandidateBlocks();
					ABObject blockToHit = finalCandidateBlocks.get(0);
					Point targetPoint = blockToHit.getCenter();
					List<Point> releasePoints;
					// Right now bottom up so choose low angle shot
					releasePoints = tp.estimateLaunchPoint(sling, targetPoint);
					if (releasePoints.size() == 0) {

						{
							System.out.println("No release point found for the target");
							System.out.println("Try a shot with 45 degree");
							finalReleasePoint = tp.findReleasePoint(sling, Math.PI / 4);
						}

					} else
						finalReleasePoint = releasePoints.get(0);
					// Get the reference point
					Point refPoint = tp.getReferencePoint(sling);

					// Calculate the tapping time according the bird type
					if (finalReleasePoint != null) {
						double releaseAngle = tp.getReleaseAngle(sling, finalReleasePoint);
						// System.out.println("Release Point: " + releasePoint);
						System.out.println("Release Angle: " + Math.toDegrees(releaseAngle));
						int tapInterval = 0;
						switch (aRobot.getBirdTypeOnSling()) {

						case RedBird:
							tapInterval = 0;
							break; // start of trajectory
						case YellowBird:
							tapInterval = 75 + randomGenerator.nextInt(20);
							break; // 65-90% of the way
						case WhiteBird:
							tapInterval = 70 + randomGenerator.nextInt(20);
							break; // 70-90% of the way
						case BlackBird:
							tapInterval = 70 + randomGenerator.nextInt(20);
							break; // 70-90% of the way
						case BlueBird:
							tapInterval = 65 + randomGenerator.nextInt(20);
							break; // 65-85% of the way
						default:
							tapInterval = 60;
						}

						int tapTime = tp.getTapTime(sling, finalReleasePoint, targetPoint, tapInterval);
						dx = (int) finalReleasePoint.getX() - refPoint.x;
						dy = (int) finalReleasePoint.getY() - refPoint.y;
						shot = new Shot(refPoint.x, refPoint.y, dx, dy, 0, tapTime);
					} else {
						System.err.println("No Release Point Found");
						return state;
					}
				}

				// check whether the slingshot is changed. the change of the
				// slingshot indicates a change in the scale.
				{
					ActionRobot.fullyZoomOut();
					screenShot = ActionRobot.doScreenShot();
					vision = new Vision(screenShot);
					Rectangle _sling = vision.findSlingshotMBR();
					if (_sling != null) {
						double scale_diff = Math.pow((sling.width - _sling.width), 2)
								+ Math.pow((sling.height - _sling.height), 2);
						if (scale_diff < 25) {
							if (dx < 0) {
								aRobot.cshoot(shot);
								state = aRobot.getState();
								if (state == GameState.PLAYING) {
									screenShot = ActionRobot.doScreenShot();
									vision = new Vision(screenShot);
									List<Point> traj = vision.findTrajPoints();
									tp.adjustTrajectory(traj, sling, finalReleasePoint);
									firstShot = false;
								}
							}
						} else
							System.out
									.println("Scale is changed, can not execute the shot, will re-segement the image");
					} else
						System.out.println("no sling detected, can not execute the shot, will re-segement the image");
				}

			}

		}
		return state;
	}

	public static List<ABObject> findHill(List<ABObject> objects) {
		List<ABObject> hills = new LinkedList<ABObject>();
		for (ABObject obj : objects) {
			if (obj.type == ABType.Hill)
				hills.add(obj);
		}
		return hills;
	}

	public static void main(String args[]) {

		NaiveAgent na = new NaiveAgent();
		if (args.length > 0)
			na.currentLevel = Integer.parseInt(args[0]);
		na.run();
	}
}
