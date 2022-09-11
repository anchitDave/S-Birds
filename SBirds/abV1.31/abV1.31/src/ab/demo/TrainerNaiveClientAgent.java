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
import ab.demo.other.ClientActionRobot;
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

public class TrainerNaiveClientAgent implements Runnable {
	public int runningLevel = 0;
	public boolean runAll = false;
	public int[] solved;
	public int[] attempt;
	public int failedCounter = 0;
	private int id = 0;
	public static int shotn = 0;
	private Rectangle _slingShot;
	static boolean isOkay = false;
	static int continuousLevelsPlayed = 0;
	static FileWriter file;
	private ClientActionRobotJava aRobot;
	private Random randomGenerator;
	public byte currentLevel = -1;
	public static int time_limit = 12;
	private Map<Integer, Integer> scores = new LinkedHashMap<Integer, Integer>();
	TrajectoryPlanner tp;
	private boolean firstShot;
	private Point prevTarget;
	// public static int flagBottomUp=0;
	// public static int flagTopDown=0;
	public static double normalizedScore;

	/**
	 * Constructor using the default IP
	 */
	public TrainerNaiveClientAgent() {
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
	 */
	public TrainerNaiveClientAgent(String ip) {
		aRobot = new ClientActionRobotJava(ip);
		tp = new TrajectoryPlanner();
		randomGenerator = new Random();
		prevTarget = null;
		firstShot = true;
		System.out.println("Enter");

	}

	public TrainerNaiveClientAgent(String ip, int id) {
		aRobot = new ClientActionRobotJava(ip);
		tp = new TrajectoryPlanner();
		randomGenerator = new Random();
		prevTarget = null;
		firstShot = true;
		this.id = id;
		System.out.println("Enter");
	}

	public boolean isBottomUp() {
		return false;
	}

	public int getNewNextLevel() {
		int level = 0;
		boolean unsolved = false;
		// all the levels have been solved, then get the first unsolved level
		for (int i = 0; i < solved.length; i++) {
			if (solved[i] == 0) {
				unsolved = true;
				level = (byte) (i + 1);
				System.out.println(level);
				if (level <= currentLevel && currentLevel < solved.length)
					continue;
				else
					return level;
			}
		}
		if (unsolved)
			return level;
		level = (byte) ((this.currentLevel + 1) % solved.length);
		if (level == 0)
			level = solved.length;
		return level;
	}

	public int getNextLevel() {
		// attempt all once
		if ((attempt[attempt.length - 1] < 0 || runAll) && runningLevel < attempt.length) {
			++runningLevel;
		}
		// attempt unsolved
		else {
			boolean isUnsolved = false;
			for (int i = runningLevel % attempt.length; i < solved.length; i++) {
				if (solved[i] == 0) {
					runningLevel = i + 1;
					isUnsolved = true;
					break;
				}
			}
			if (runningLevel % attempt.length == 0 && !isUnsolved) {
				runAll = true;
				runningLevel = 1;
			} else if (!isUnsolved) {
				runningLevel = 0;
				return getNewNextLevel();
			}
		}
		attempt[runningLevel - 1]++;

		return runningLevel;
	}

	/*
	 * Run the Client (Naive Agent)
	 */
	public void run() {
		byte[] info = aRobot.configure(ClientActionRobot.intToByteArray(id));
		solved = new int[info[2]];
		attempt = new int[info[2]];
		for (int i = 0; i < attempt.length; i++) {
			attempt[i] = -1;

		}
		// load the initial level (default 1)
		// Check my score
		int[] _scores = aRobot.checkMyScore();
		int counter = 0;
		for (int i : _scores) {
			System.out.println(" level " + ++counter + "  " + i);
		}
		currentLevel = (byte) getNextLevel();
		aRobot.loadLevel(currentLevel);
		// aRobot.loadLevel((byte)9);
		GameState state;
		while (true) {

			state = solve();

			// If the level is solved , go to the next level
			if (state == GameState.WON) {

				/// System.out.println(" loading the level " + (currentLevel +
				/// 1) );
				System.out.println(" My score: ");
				int[] scores = aRobot.checkMyScore();
				for (int i = 0; i < scores.length; i++) {

					System.out.print(" level " + (i + 1) + ": " + scores[i]);
					if (scores[i] > 0)
						solved[i] = 1;

				}
				System.out.println();
				currentLevel = (byte) getNextLevel();
				aRobot.loadLevel(currentLevel);
				// aRobot.loadLevel((byte)9);
				// display the global best scores
				scores = aRobot.checkScore();
				System.out.println("The global best score: ");
				for (int i = 0; i < scores.length; i++) {

					System.out.print(" level " + (i + 1) + ": " + scores[i]);
				}
				System.out.println();

				// make a new trajectory planner whenever a new level is entered
				tp = new TrajectoryPlanner();

				// first shot on this level, try high shot first
				firstShot = true;

			} else
			// If lost, then restart the level
			if (state == GameState.LOST) {
				failedCounter++;

				failedCounter = 0;
				currentLevel = (byte) getNextLevel();
				aRobot.loadLevel(currentLevel);

				// aRobot.loadLevel((byte)9);

			} else if (state == GameState.LEVEL_SELECTION) {
				System.out.println("unexpected level selection page, go to the last current level : " + currentLevel);
				aRobot.loadLevel(currentLevel);
			} else if (state == GameState.MAIN_MENU) {
				System.out.println("unexpected main menu page, reload the level : " + currentLevel);
				aRobot.loadLevel(currentLevel);
			} else if (state == GameState.EPISODE_MENU) {
				System.out.println("unexpected episode menu page, reload the level: " + currentLevel);
				aRobot.loadLevel(currentLevel);
			}

		}

	}

	private double distance(Point p1, Point p2) {
		return Math.sqrt((double) ((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y)));
	}

	public GameState solve() {
		// capture Image
		BufferedImage screenShot = aRobot.doScreenShot();

		// process image
		Vision vision = new Vision(screenShot);

		// find the slingshot
		Rectangle sling = vision.findSlingshotMBR();

		// confirm the slingshot
		while (sling == null && aRobot.checkState() == GameState.PLAYING) {
			System.out.println("No slingshot detected. Please remove pop up or zoom out");
			aRobot.fullyZoomOut();
			screenShot = aRobot.doScreenShot();
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
				int dx, dy;

				// random pick up a pig
				List<ABObject> allObjects = vision.getVisionRealShape().findObjects();
				List<ABObject> hill = vision.getVisionRealShape().findHills();
				List<ABObject> Pigs = vision.getVisionRealShape().findPigs();
				HeuristicEngine he = new HeuristicEngine(sling, allObjects, pigs, hill, aRobot.getBirdTypeOnSling());
				for (ABObject o : hill)
					allObjects.add(o);
				for (ABObject o : Pigs)
					allObjects.add(o);
				List<ABObject> wood = new LinkedList<ABObject>();
				List<ABObject> ice = new LinkedList<ABObject>();
				List<ABObject> stone = new LinkedList<ABObject>();
				List<ABObject> TNT = new LinkedList<ABObject>();
				wood.addAll(findWood(allObjects));
				ice.addAll(findIce(allObjects));
				stone.addAll(findStone(allObjects));
				TNT.addAll(findTNT(allObjects));
				VectorQuantizer vq = new VectorQuantizer(pigs, wood, ice, stone, TNT);
				Rectangle boundingRectangle = vq.getBoundingStructure();
				double[][][] qunantizedStructure = vq.quantize(boundingRectangle);
				he.generateAirBlocks(qunantizedStructure, boundingRectangle);
				he.generateSubStructures();

				he.calculateSupportWeight();
				he.addAir();
				he.getDisplacement();
				he.penetrationWeight();
				ArrayList<ABObject> finalCandidateBlocks = he.getFinalCandidateBlocks();
				ABObject blockToHit = finalCandidateBlocks.get(0);
				Point targetPoint = blockToHit.getCenter();
				Point Left = blockToHit.getLeftEnd(targetPoint, blockToHit.width, blockToHit.height);
				Point Right = blockToHit.getRightEnd(targetPoint, blockToHit.width, blockToHit.height);
				List<Point> releasePoints;
				// Right now bottom up so choose low angle shot
				releasePoints = tp.estimateLaunchPoint(sling, targetPoint);
				System.out.println("releasepoints=" + releasePoints.size());
				if (releasePoints.size() == 0) {

					{
						System.out.println("No release point found for the target");
						System.out.println("Try a shot with 45 degree");
						finalReleasePoint = tp.findReleasePoint(sling, Math.PI / 4);
					}

				} else {
					if (attempt[runningLevel - 1] == 0) {
						finalReleasePoint = releasePoints.get(0);
					} else if (releasePoints.size() > 1 && attempt[runningLevel - 1] == 1) {
						finalReleasePoint = releasePoints.get(1);
					} else if (attempt[runningLevel - 1] > 1) {
						return ClientNaiveAgent.solve(aRobot);
					} else {
						finalReleasePoint = releasePoints.get(0);
					}
				}
				// Get the reference point
				Point refPoint = tp.getReferencePoint(sling);
				// Calculate the tapping time according the bird type
				if (finalReleasePoint != null) {
					double releaseAngle = tp.getReleaseAngle(sling, finalReleasePoint);
					System.out.println("Release Angle: " + Math.toDegrees(releaseAngle));
					int tapInterval = 0;
					switch (aRobot.getBirdTypeOnSling()) {

					case RedBird:
						tapInterval = 0;
						break; // start of trajectory
					case YellowBird:
						tapInterval = 95 + randomGenerator.nextInt(3);
						break; // 70-90% of the way
					case WhiteBird:
						tapInterval = 95 + randomGenerator.nextInt(3);
						break; // 70-90% of the way
					case BlackBird:
						tapInterval = 95 + randomGenerator.nextInt(3);
						break; // 70-90% of the way
					case BlueBird:
						tapInterval = 95 + randomGenerator.nextInt(3);
						break; // 65-85% of the way
					default:
						tapInterval = 98;
					}
					ABObject firstBlock = he.findTheFirstBlock(sling, allObjects, targetPoint, blockToHit,
							finalReleasePoint, 1);
					int tapTime = tp.getTapTime(sling, finalReleasePoint, targetPoint, tapInterval, firstBlock);
					dx = (int) finalReleasePoint.getX() - refPoint.x;
					dy = (int) finalReleasePoint.getY() - refPoint.y;
					shot = new Shot(refPoint.x, refPoint.y, dx, dy, 0, tapTime);
				} else {
					System.err.println("No Release Point Found");
					return aRobot.checkState();
				}

				// check whether the slingshot is changed. the change of the
				// slingshot indicates a change in the scale.
				{
					aRobot.fullyZoomOut();
					screenShot = aRobot.doScreenShot();
					vision = new Vision(screenShot);
					Rectangle _sling = vision.findSlingshotMBR();
					if (_sling != null) {
						double scale_diff = Math.pow((sling.width - _sling.width), 2)
								+ Math.pow((sling.height - _sling.height), 2);
						if (scale_diff < 25) {
							if (dx < 0) {
								aRobot.shoot(shot.getX(), shot.getY(), shot.getDx(), shot.getDy(), shot.getT_shot(),
										shot.getT_tap(), false);

								state = aRobot.checkState();
								if (state == GameState.PLAYING) {
									screenShot = aRobot.doScreenShot();
									vision = new Vision(screenShot);
									List<Point> traj = vision.findTrajPoints();
									tp.adjustTrajectory(traj, sling, finalReleasePoint);
									firstShot = false;
									stateExtractor = new GameStateExtractor();
									int finalScore = stateExtractor.getScoreInGame(screenShot);
									int scoreGained = finalScore - beginScore;
									int totalScore = he.getMaxScore();
									normalizedScore = 100 * (double) scoreGained / (double) totalScore;
									if (normalizedScore <= 100) {
										HeuristicEngine.trainCoefficients(
												100 * (double) scoreGained / (double) totalScore,
												blockToHit.isBottomUp);
									}

								} else if (state == GameState.WON) {
									screenShot = aRobot.doScreenShot();
									stateExtractor = new GameStateExtractor();
									int finalScore = stateExtractor.getScoreEndGame(screenShot);
									int totalScore = he.getMaxScore();
									int scoreGained = finalScore - beginScore;
									// normalizedScore=100*(double)scoreGained/(double)totalScore;
									// HeuristicEngine.trainCoefficients(normalizedScore,blockToHit.isBottomUp);
									// String str = (++shotn)+"
									// a:"+blockToHit.isBottomUp+"
									// L:"+currentLevel+" ns:"+normalizedScore+"
									// alb:"+HeuristicEngine.alphaBottomUp+"
									// bb:"+HeuristicEngine.betaBottomUp+"
									// g:"+HeuristicEngine.gamma+"
									// bM:"+HeuristicEngine.bottomUpMean+"
									// bsd+"+HeuristicEngine.bottomUpStandardDeviation+"
									// fb+"+HeuristicEngine.flagBottomUp+"
									// at:"+HeuristicEngine.alphaTopDown+"
									// bt"+HeuristicEngine.betaTopDown+"
									// d:"+HeuristicEngine.delta+"
									// tm:"+HeuristicEngine.topDownMean+"
									// tsd:"+HeuristicEngine.topDownStandardDeviation+"
									// ft:"+HeuristicEngine.flagTopDown;
									// try {
									// write.write(str);
									// write.newLine();
									// write.flush();
									// } catch (IOException e) {
									// e.printStackTrace();
									// }
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

	public List<ABObject> findWood(List<ABObject> objects) {
		List<ABObject> ans = new LinkedList<ABObject>();
		for (ABObject obj : objects) {
			if (obj.type == ABType.Wood)
				ans.add(obj);
		}
		return ans;
	}

	public List<ABObject> findIce(List<ABObject> objects) {
		List<ABObject> ans = new LinkedList<ABObject>();
		for (ABObject obj : objects) {
			if (obj.type == ABType.Ice)
				ans.add(obj);
		}
		return ans;
	}

	public List<ABObject> findStone(List<ABObject> objects) {
		List<ABObject> ans = new LinkedList<ABObject>();
		for (ABObject obj : objects) {
			if (obj.type == ABType.Stone)
				ans.add(obj);
		}
		return ans;
	}

	public List<ABObject> findTNT(List<ABObject> objects) {
		List<ABObject> ans = new LinkedList<ABObject>();
		for (ABObject obj : objects) {
			if (obj.type == ABType.TNT)
				ans.add(obj);
		}
		return ans;
	}

	public static void main(String args[]) {

		TrainerNaiveClientAgent na;
		if (args.length > 0)
			na = new TrainerNaiveClientAgent(args[0]);
		else
			na = new TrainerNaiveClientAgent();
		na.run();
	}
}
