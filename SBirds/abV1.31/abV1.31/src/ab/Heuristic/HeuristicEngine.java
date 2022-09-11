package ab.Heuristic;

//import ab.MyHeuristic.SirMateSubStructure;
import ab.MyHeuristic.SirMateSubStructure;
import ab.demo.NaiveAgent;
import ab.demo.TrainerNaiveAgent;
import ab.vision.ABObject;
import ab.vision.ABType;

import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Created by Hitarth on 29-05-2014.
 */

import java.awt.Rectangle;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.lang.Math;
import ab.planner.TrajectoryPlanner;
import ab.utils.ABUtil;
import ab.learn.VectorQuantizer;
import ab.vision.Vision;
import ab.vision.real.shape.Poly;


public class HeuristicEngine {
	private static TrajectoryPlanner tp = new TrajectoryPlanner();
	public List<Point> trajectoryPoints = null;
    public static int count=0;
    public static double bottomUpStandardDeviation =1000000;
    public static double topDownStandardDeviation=1000000;
    public static ArrayList<Double> bottomUpNormalizedScores =new ArrayList<Double>();
    public static ArrayList<Double> topDownNormalizedScores=new ArrayList<Double>();
    public static double bottomUpMean=0;
    public static double topDownMean;
    public static double threshold=10;
    private Rectangle _slingShot;
    private List<ABObject> _pigs;
    private List<ABObject> _wood;
    private List<ABObject> _ice;
    private List<ABObject> _stones;
    private List<ABObject> _hill;
    public List<ABObject> _air;
    private List<ABObject> _tnt;
    private ABType currentBird;
    private List<ABObject> allObjects;
    boolean hasAnyBlockCome = false;
    private List<SirMateSubStructure> subStructures;
//    private List<Rectangle> _redBirds;
//    private List<ABObject> _blueBirds;
//    private List<Rectangle> _yellowBirds;
//    private List<Rectangle> _whiteBirds;
//    private List<Rectangle> _blackBirds;
    private List<ABObject> birds;
    private static final int HIGHER = 30;
    private static final int MEDIUM = 20;
    private static final int LOW = 10;
    private int iceDensity = MEDIUM;
    private int woodDensity = LOW;
    private int stoneDensity = HIGHER;
    private BufferedImage image;
    private Vision vision;
    private int ground;
    public static int flagBottomUp = 1;
    public static int flagTopDown = 1;


    public static double alphaBottomUp=0.65;
    public static double alphaTopDown=0.65;
    public static double betaBottomUp=0.10;
    public static double betaTopDown=0.15;
    public static double gamma=0.25;
    public static double delta=0.20;
    private static  double changeValue=0.05;
    

    //static NaiveAgent naiveAgent=new NaiveAgent();
    
    public HeuristicEngine(List<ABObject> objects)
    {
        this.allObjects=objects;
        subStructures = new LinkedList<SirMateSubStructure>();
    }

    public HeuristicEngine(Rectangle slingShot, List<ABObject> pigs, List<ABObject> wood, List<ABObject> ice, List<ABObject> stones,List<ABObject> TNT, List<ABObject> terrain,List<ABObject> birds,BufferedImage image) {

        _slingShot = slingShot;
        _wood = wood;
        _pigs = pigs;
        _ice = ice;
        _stones = stones;
        _hill = terrain;
        this.birds = birds;
        this._tnt = TNT;
        _air = new LinkedList<ABObject>();
        subStructures = new LinkedList<SirMateSubStructure>();
        //this.currentBird = currentBird;
        this.image = image;
        vision=new Vision(image);
        ground=vision.getVisionRealShape().getGround();
    }

    public HeuristicEngine(Rectangle slingShot, List<ABObject> pigs, List<ABObject> wood, List<ABObject> ice, List<ABObject> stones,List<ABObject> TNT, List<ABObject> terrain,List<ABObject> birds,ABType currentBird) {

        _slingShot = slingShot;
        _wood = wood;
        _pigs = pigs;
        _ice = ice;
        _stones = stones;
        _hill = terrain;
        this.birds = birds;
        this._tnt = TNT;
        this.currentBird = currentBird;
        _air = new LinkedList<ABObject>();
        subStructures = new LinkedList<SirMateSubStructure>();
    }

    public HeuristicEngine(List<ABObject> allObjects,List<ABObject> _pigs,List<ABObject> _hill)
    {
        this.allObjects = allObjects;
        this._pigs = _pigs;
        subStructures = new LinkedList<SirMateSubStructure>();
        _air = new LinkedList<ABObject>();
        //this.currentBird = currentBird;
        this._hill=_hill;
    }

    public HeuristicEngine(Rectangle _slingShot,List<ABObject> allObjects,List<ABObject> _pigs,List<ABObject> _hill,ABType currentBird)
    {
        this.allObjects = allObjects;
        this._pigs = _pigs;
        subStructures = new LinkedList<SirMateSubStructure>();
        _air = new LinkedList<ABObject>();
        this._slingShot = _slingShot;
        this._hill=_hill;
        this.currentBird = currentBird;
    }

    public ArrayList<ABObject> getScoreBlocks()
    {
        ArrayList<ABObject> scoringBlocks = new ArrayList<ABObject>();
        if(_wood!=null)
            scoringBlocks.addAll(_wood);
        if(_stones!=null)
            scoringBlocks.addAll(_stones);
        if(_ice!=null)
            scoringBlocks.addAll(_ice);
        if(_pigs!=null)
            scoringBlocks.addAll(_pigs);
        if(_tnt!=null)
            scoringBlocks.addAll(_tnt);


        return scoringBlocks;
    }

    public class LineIterator implements Iterator<Point2D> {
        final static double DEFAULT_PRECISION = 1.0;
        final Line2D line;
        final double precision;

        final double sx, sy;
        final double dx, dy;

        double x,y,error;

        public LineIterator(Line2D line, double precision) {
            this.line = line;
            this.precision = precision;

            sx = line.getX1() < line.getX2() ? precision : -1 * precision;
            sy = line.getY1() < line.getY2() ? precision : -1 * precision;

            dx =  Math.abs(line.getX2() - line.getX1());
            dy = Math.abs(line.getY2() - line.getY1());

            error = dx - dy;

            y = line.getY1();
            x = line.getX1();
        }

        public LineIterator(Line2D line) {
            this(line, DEFAULT_PRECISION);
        }

        @Override
        public boolean hasNext() {
            return Math.abs( x - line.getX2()) > 0.9 || ( Math.abs(y - line.getY2()) > 0.9);
        }

        @Override
        public Point2D next() {
            Point2D ret = new Point2D.Double(x, y);

            double e2 = 2*error;
            if (e2 > -dy) {
                error -= dy;
                x += sx;
            }
            if (e2 < dx) {
                error += dx;
                y += sy;
            }

            return ret;
        }

        @Override
        public void remove() {
            throw new AssertionError();
        }
    }


    public Point findFirstIntersection(List<Point> trajectory) {
        Point intersectionPoint = new Point();
        // decompose all the blocks to points.
        ArrayList<Point> _allPoints = new ArrayList<Point>(0);
        _allPoints.addAll(VectorQuantizer.decomposeToPoints(_pigs));
        _allPoints.addAll(VectorQuantizer.decomposeToPoints(_wood));
        _allPoints.addAll(VectorQuantizer.decomposeToPoints(_ice));
        _allPoints.addAll(VectorQuantizer.decomposeToPoints(_stones));
        _allPoints.addAll(VectorQuantizer.decomposeToPoints(_tnt));
        HashSet<Point> allPoints = new HashSet<Point>();  // Use HashSet for quick membership check
        for (Point p: _allPoints) allPoints.add(p);
        // Find intersection of trajectory and allPoints
        double minX = 10000000;
        for (Point p: trajectory) {
            if (allPoints.contains(p) && p.getX() < minX) {
                intersectionPoint = p;
                minX = p.getX();
            }
        }
        return intersectionPoint;
    }

    public int numberOfBlocksBeforeTerrain(List<Point> trajectory) {
        if(trajectory == null)
            return -1;

        int intersectionX = 100000;
        int intersectionY = -1;
        Point intersection = null;

        for(Point p : trajectory) {
            if(this._hill.contains(p)) {
                if (p.getX() < intersectionX) {
                    intersectionX = (int)p.getX();
                    intersection = p;
                }
            }
        }

        if(intersection == null) {
            return 1000;
        }

        intersectionY = (int)intersection.getY();

        List<Rectangle> blocks = new ArrayList<Rectangle>(0);
        if(_pigs != null)
            blocks.addAll(_pigs);
        if(_wood != null)
            blocks.addAll(_wood);
        if(_ice != null)
            blocks.addAll(_ice);
        if(_stones != null)
            blocks.addAll(_stones);

        List<Point> blockPerimeterPoints = new ArrayList<Point>(0);
        for(Rectangle r : blocks) {
            int topLeftX = (int)r.getX();
            int topLeftY = (int)r.getY();
            int height = (int)r.getHeight();
            int width = (int)r.getWidth();

            for(int i=0;i<height;i++) {
                blockPerimeterPoints.add(new Point(topLeftX, topLeftY + i));
                blockPerimeterPoints.add(new Point(topLeftX + (width - 1), topLeftY + i));
            }
            for(int i=0;i<width;i++) {
                blockPerimeterPoints.add(new Point(topLeftX + i, topLeftY));
                blockPerimeterPoints.add(new Point(topLeftX + i, topLeftY + (height - 1)));
            }
        }

        List<Point> validPoints = new ArrayList<Point>(0);
        for(Point p : blockPerimeterPoints) {
            if(p.getX() < intersectionX && p.getY() < intersectionY) {
                validPoints.add(p);
            }
        }

        int blockIntersections=0;
        for(Point t : trajectory) {
            if(validPoints.contains(t))
                blockIntersections++;
        }

        return (int)(blockIntersections/2);
    }


    // finds outermost blocks of the structure
    public List<Rectangle> findOuterBlocks() {
        ArrayList<Rectangle> allRect = new ArrayList<Rectangle>(0);
        if(_pigs != null)
            allRect.addAll(_pigs);
        if(_wood != null)
            allRect.addAll(_wood);
        if(_ice != null)
            allRect.addAll(_ice);
        if(_stones != null)
            allRect.addAll(_stones);

        ArrayList<Rectangle> result = new ArrayList<Rectangle>(0);
        for(Rectangle r: allRect)
        {
            ArrayList<Point2D.Double> points = new ArrayList<Point2D.Double>(0);
            for(int i=1;i<=30;i++)
            {
                Point2D.Double point = new Point2D.Double(r.getX()-i,r.getY()+(r.height/2));
                points.add(point);
            }
            for(int i=1;i<=30;i++)
            {
                Point2D.Double point = new Point2D.Double(r.getX()-30,r.getY()+i);
                points.add(point);
            }

            boolean inside = false;
            for(Rectangle s: allRect)
            {
                boolean temp = false;
                for(Point2D p:points)
                {
                    if(s.contains(p))
                    {
                        temp = true;
                        break;
                    }
                    else
                        temp = false;
                }

                if(temp)
                {
                    inside = false;
                    break;
                }
                else
                    inside = true;
            }

            if(inside)
                result.add(r);
        }
        //return findConnectedOuterBlocks(allRect);
        result.addAll(_pigs);

        return result;


    }

    // methods to determine type of a rectangle
    //weighted distance method from outer block to nearest pig
    //input - result of outerblocks()
    //output - list of weighted distances from each outer block

    public ArrayList<Integer> weightedDistance(List<Rectangle> outerblocks)
    {
        ArrayList<Rectangle> allRect = new ArrayList<Rectangle>(0);
        //if(_pigs != null)
        //	allRect.addAll(_pigs);
        if(_wood != null)
            allRect.addAll(_wood);
        if(_ice != null)
            allRect.addAll(_ice);
        if(_stones != null)
            allRect.addAll(_stones);

        ArrayList<Integer> weighted_dist = new ArrayList<Integer>(0);
        int ob = 0;
        //filtering outerblocks but still not in picture....
        double pig_max_x=0;
        for(Rectangle r: _pigs)
        {
            if(pig_max_x<r.getX())
            {
                pig_max_x = r.getX();
            }
        }
        ////////////////////////////
        for(Rectangle r: outerblocks )
        {
            int nearest_pig = 0;
            double min_dis = 10000;
            int index = 0;
            for(Rectangle p: _pigs)
            {
                double temp = Point2D.distance(p.getX(),p.getY(),r.getX(),r.getY());
                if(p.getX() > r.getX() && temp < min_dis && !isPig(r)) // pig should be after the outer block.
                {
                    min_dis = temp;
                    nearest_pig = index;
                }
                index+=1;
                //System.out.println("nearrest pig is found if not -1"+ nearest_pig);
            }

            Line2D.Double line = new Line2D.Double(new Point2D.Double(_pigs.get(nearest_pig).getX(),_pigs.get(nearest_pig).getY()),new Point2D.Double(r.getX(),r.getY()));

            //System.out.println(line.intersects(allRect.get(0)));
            //List<Point2D> vertices = line.vertices();
            int weight = 0;

            if(currentBird==ABType.RedBird)//Red Bird
            {

                //for(Point2D v: vertices)
                //{
                for(Rectangle blocks: allRect)
                {

                    //int area = blocks.width * blocks.height;
                    int area = Math.min(blocks.width,blocks.height);
                    if(line.intersects(blocks))
                    {
                        //System.out.println("redcalled");
                        if(isWood(blocks))
                        {
                            weight += LOW * area;

                            //System.out.println(weight);
                        }
                        else if(isIce(blocks))
                            weight += MEDIUM * area;
                        else if(isStone(blocks))
                            weight += HIGHER * area;
                        else
                        {
                            continue;
                        }
                        //System.out.println(weight/area);

                    }
                }
                //}
            }
            else if (currentBird==ABType.BlueBird) //Blue Bird
            {
                //for(Point2D v: vertices)
                //{
                for(Rectangle blocks: allRect)
                {
                    //int area = blocks.width * blocks.height;
                    int area = Math.min(blocks.width,blocks.height);
                    if(line.intersects(blocks))
                    {
                        if(isWood(blocks))
                            weight += MEDIUM * area;
                        else if(isIce(blocks))
                            weight += LOW * area;
                        else if(isStone(blocks))
                            weight += HIGHER * area;
                        else
                            continue;
                    }
                }
                //}

            }
            else if (currentBird==ABType.YellowBird) // Yellow Bird
            {
                //for(Point2D v: vertices)
                //{
                for(Rectangle blocks: allRect)
                {
                    int area = Math.min(blocks.width,blocks.height);
                    if(line.intersects(blocks))
                    {
                        if(isWood(blocks))
                            weight += LOW * area;
                        else if(isIce(blocks))
                            weight += MEDIUM * area;
                        else if(isStone(blocks))
                            weight += HIGHER * area;
                        else
                            continue;
                    }
                }
                //}
            }
            else if (currentBird==ABType.WhiteBird) // White Bird
            {
                //for(Point2D v: vertices)
                //{
                for(Rectangle blocks: allRect)
                {
                    int area = Math.min(blocks.width,blocks.height);
                    if(line.intersects(blocks))
                    {
                        if(isWood(blocks))
                            weight += HIGHER * area;
                        else if(isIce(blocks))
                            weight += MEDIUM * area;
                        else if(isStone(blocks))
                            weight += LOW * area;
                        else
                            continue;
                    }
                }
                //}

            }
            else //Black Bird
            {
                //for(Point2D v: vertices)
                //{
                for(Rectangle blocks: allRect)
                {
                    int area = Math.min(blocks.width,blocks.height);
                    if(line.intersects(blocks))
                    {
                        if(isWood(blocks))
                            weight += LOW * area;
                        else if(isIce(blocks))
                            weight += MEDIUM * area;
                        else if(isStone(blocks))
                            weight += HIGHER * area;
                        else
                            continue;
                    }
                }
                //}
            }
            int terrain_intersection = 0;
            //line = new Line2D.Double(new Point2D.Double(_pigs.get(nearest_pig_x).getX(),_pigs.get(nearest_pig_x).getY()),new Point2D.Double(r.getX(),r.getY()));

            List<Point2D> ary = new ArrayList<Point2D>();
            Point2D current;
            for(Iterator<Point2D> iter = new LineIterator(line); iter.hasNext();) {
                current =iter.next();
                ary.add(current);
            }
            for(Point2D p:ary)
            {
                Point pnt = new Point((int)p.getX(),(int)p.getY());
                for(int i=0;i<_hill.size();i++)
                {
                    Poly h = (Poly) _hill.get(i);
                    if(h.polygon.contains(p))
                        terrain_intersection++;
                }
            }
//			System.out.println("terrain_intersection" + terrain_intersection + ", weight =  "+weight);

            if(terrain_intersection > 0)
                weight = 2000000;

            if(r.getX() < pig_max_x)
                weighted_dist.add(weight);
            else
                weighted_dist.add(3000000);
//			System.out.println("For outer block number " + ob + "weight is" + weight);
            ob+=1;
        }
        return weighted_dist;
    }

    public List<Integer> filter_outer_blocks(List<Rectangle> outerblocks,List<Integer> distances)
    {
        ArrayList<Integer> new_weights = new ArrayList<Integer>(0);
        for (Integer i: distances) {
            new_weights.add(i);
        }

        int counter = 0;
        for(Rectangle r: outerblocks){

            int nearest_pig = 0;
            double min_dis = 100000;
            int index = 0;

            for(Rectangle p: _pigs)
            {
                double temp = Point2D.distance(p.getX(),p.getY(),r.getX(),r.getY());
                if(p.getX() > r.getX() && temp < min_dis && !isPig(r)) // pig should be after the outer block.
                {
                    min_dis = temp;
                    nearest_pig = index;
                }
                index+=1;
                //System.out.println("nearrest pig is found if not -1"+ nearest_pig);
            }
            double nearest_pig_x = _pigs.get(nearest_pig).getX();
            double nearest_pig_y = _pigs.get(nearest_pig).getY();
            ArrayList<Point2D.Double> targets =  new ArrayList<Point2D.Double>(0);
            targets.add(new Point2D.Double(r.getX(),r.getY()+r.height/2));

            TrajectoryPlanner tp = new TrajectoryPlanner();
            ArrayList<Point> releasepoints = tp.estimateLaunchPoint(_slingShot,new Point((int)targets.get(0).getX(),(int)targets.get(0).getY()));
            int terrain_intersection=0;
            boolean ignore = false;
            for(Point p: releasepoints)
            {
                TrajectoryPlanner temptp = new TrajectoryPlanner();
                temptp.setTrajectory(_slingShot,p);
                List<Point> traj_points = temptp._trajectory;
                terrain_intersection=0;
                for(Point t: traj_points)
                {
                    for(int i=0;i<_hill.size();i++)
                    {
                        Poly h = (Poly)_hill.get(i);
                        if(h.contains(t))
                        {
                            if(t.getX() < nearest_pig_x && t.getY() < nearest_pig_y)
                                terrain_intersection++;
                        }
                    }

                }

                if(terrain_intersection>0)
                    ignore = true;
                else
                {
                    ignore = false;
                    break;
                }
                //System.out.println("Traj Intersection for outer block" + counter + "with terrain is =" + terrain_intersection);
            }
            if(ignore)
                new_weights.set(counter,1000000);
//		System.out.println("New weights is " + new_weights.get(counter));
            counter+=1;
        }
        return new_weights;
    }

	/*public List<Integer> ranking_outer_blocks(List<Rectangle> outerblocks)
	{
		List<Integer> ranks = new List<Integer>(0);
		for(Rectangle r:outerblocks)
		{
			ranks.add(-1);
		}


		return null;

	}*/

    public int find_outerblock_to_hit(List<Rectangle> outerblocks, List<Integer> distances)
    {

        int min = -1;
        int value = 100000;
        int counter = 0;
        boolean found = false;
        for(Integer i:distances)
        {
            if(value>i && i>10)
            {
                found = true;
                value = i;
                min = counter;
            }
            counter+=1;
        }
//		System.out.println("Found outer block and its index is + " + min +"found="+found);
        if(found && value < 1000)
            return min;
        else
        {
            int i=0;
            for(Rectangle r: outerblocks)
            {
                if(isPig(r)) {
                    TrajectoryPlanner tp = new TrajectoryPlanner();
                    ArrayList<Point> releasepoints = tp.estimateLaunchPoint(_slingShot,new Point((int)r.getX(),(int)r.getY()));
                    int terrain_intersection=0;
                    boolean ignore = false;
                    for(Point p: releasepoints)
                    {
                        TrajectoryPlanner temptp = new TrajectoryPlanner();
                        temptp.setTrajectory(_slingShot,p);
                        List<Point> traj_points = temptp._trajectory;
                        terrain_intersection=0;
                        for(Point t: traj_points)
                        {
                            if(_hill.contains(t)){
                                if(t.getX() < r.getX() && t.getY() < r.getY())
                                    terrain_intersection+=1;
                            }
                        }

                        if(terrain_intersection>0)
                            ignore = true;
                        else
                        {
                            ignore = false;
                            break;
                        }


                        //System.out.println("Traj Intersection for outer block" + counter + "with terrain is =" + terrain_intersection);
                    }
                    if(!ignore)
                        return i;
                }
                i++;
            }
            return i;
        }
    }

    public Point2D find_point_on_outer_block(Rectangle r)
    {
        int targetpoint;
        ArrayList<Point2D.Double> targets =  new ArrayList<Point2D.Double>(0);
        targets.add(new Point2D.Double(r.getX(),r.getY()));
        targets.add(new Point2D.Double(r.getX(),r.getY()+r.height/2));
        targets.add(new Point2D.Double(r.getX(),r.getY()+r.height));

        int index=0;
        int nearest_pig=0;
        int min_dis = 100000;
        for(Rectangle p: _pigs)
        {
            double temp = Point2D.distance(p.getX(),p.getY(),r.getX(),r.getY());
            if(p.getX() > r.getX() && temp < min_dis) // pig should be after the outer block.
            {
                nearest_pig = index;
            }
            index+=1;
            //System.out.println("index"+nearest_pig);
        }
        double nearest_pig_x = _pigs.get(nearest_pig).getX();
        double nearest_pig_y = _pigs.get(nearest_pig).getY();

        for(int i=0;i<3;i++)
        {
            TrajectoryPlanner traj = new TrajectoryPlanner();
            ArrayList<Point> releasepoints = traj.estimateLaunchPoint(_slingShot,new Point((int)targets.get(i).getX(),(int)targets.get(i).getY()));
            for(Point p:releasepoints)
            {
                traj.setTrajectory(_slingShot,p);
                ArrayList<Point> trajectory_points = traj._trajectory;
                //System.out.println(trajectory_points.size());
                for(Point t:trajectory_points)
                {
                    if(t.getX()==nearest_pig_x && t.getY()==nearest_pig_y)
                    {
//						System.out.println("Target Point is" + i);
                        return targets.get(i);
                    }
                }
            }
        }
        return targets.get(1);//returning middle point of block when x co ordinate of pig not on trajectory path.
    }

    public List<Point> rust(List<Point> targets, List<Point> blackRelPoints) {
        ArrayList<Point> relPoints = new ArrayList<Point>(0);
        TrajectoryPlanner tp = new TrajectoryPlanner();

        for(Point p : targets) {
            List<Point> releasePoints = tp.estimateLaunchPoint(_slingShot, p);
            relPoints.addAll(releasePoints);
        }
        //System.out.println("relpoints - "+relPoints);
//        System.out.println("size of blackrelpoints - "+blackRelPoints.size());
        List<Point> whiteListedPoints = new ArrayList<Point>();
        whiteListedPoints.addAll(relPoints);
        for (Point p : relPoints) {
            for (Point p2 : blackRelPoints) {
                if (Math.abs(p2.getX() - p.getX())+Math.abs(p2.getY() - p.getY()) < 0.0002) {
                    whiteListedPoints.remove(p);
                }
            }
        }
        return whiteListedPoints;
    }

    public boolean isPig(Rectangle obj) {
        return _pigs.contains(obj);
    }
    public boolean isWood(Rectangle obj) {
        return _wood.contains(obj);
    }
    public boolean isIce(Rectangle obj) {
        return _ice.contains(obj);
    }
    public boolean isStone(Rectangle obj) {
        return _stones.contains(obj);
    }


    public Rectangle pigBoundingRectangle()
    {
        ABObject minXpig;
        ABObject minYpig;
        int maxXpigWidth=0;
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;

        for(ABObject pig: _pigs)
        {
            if(pig.getX()<minX)
            {
                minXpig = pig;
                minX = (int)pig.getX();
            }
        }

        for(ABObject pig: _pigs)
        {
            if(pig.getX()>maxX)
            {
                maxXpigWidth = (int)pig.getWidth();
                maxX = (int)pig.getX();
            }
        }


        for(ABObject pig: _pigs)
        {
            if(pig.getY()<minY)
            {
                minYpig = pig;
                minY = (int)pig.getY();
            }
        }

        Rectangle required = new Rectangle(minX,minY,maxX+maxXpigWidth - minX ,ground - minY );
        return required;


    }

    public ArrayList<ABObject> getSupportCandidateBlocks(Rectangle pigBoundiRectangle)
    {
        ArrayList<ABObject> allBlocks = new ArrayList<ABObject>();
        if(this.allObjects==null){
        allBlocks.addAll(_ice);
        allBlocks.addAll(_stones);
        allBlocks.addAll(_wood);}
        else
        allBlocks.addAll(this.allObjects);
        for(ABObject object:allBlocks)
        {
            if(pigBoundiRectangle.intersects(object) || pigBoundiRectangle.contains(object))
                continue;
            else
                allBlocks.remove(object);
        }
        return allBlocks;
    }

    private int getBlockDensity(ABObject block)
    {
        return block.type==ABType.Ice?iceDensity:block.type==ABType.Wood?woodDensity:stoneDensity;
    }

    public void calculateSupportWeight()
    {

        ArrayList<ABObject> candidateBlocks = new ArrayList<ABObject>();
        if(allObjects==null){
        if(_stones!=null)
            candidateBlocks.addAll(_stones);
        if(_wood!=null)
            candidateBlocks.addAll(_wood);
        if(_ice!= null)
            candidateBlocks.addAll(_ice);
        if(_tnt!=null)
            candidateBlocks.addAll(_tnt);}
        else
        candidateBlocks.addAll(allObjects);
        for(ABObject object: candidateBlocks)
        {
            SirMateSubStructure current=null;
            for(int i=0;i<subStructures.size();i++)
            {
                if(subStructures.get(i).contains(object))
                    current = subStructures.get(i);

            }
//            System.out.println("current substructure:"+subStructures.indexOf(current));
            if(current!=null) {
                for (ABObject pig : _pigs) {
                    Line2D.Double line = new Line2D.Double(object.getCenterX(), object.getCenterY(), pig.getCenterX(), pig.getCenterY());
                    int count = 0;
                    double support = 0;
                    for (ABObject obj : candidateBlocks) {

                        if (line.intersects(obj)&& current.contains(obj)) {

                            support += ((obj.getHeight() / obj.getWidth()) / (obj.area * getBlockDensity(obj)));
                            count++;
                        }
                    }
                    object.supportWeight += support / count;

                }
                object.supportWeight*=100000;
            }
        }
        return;
    }


//    public void generateSupporters()
//    {
//        ArrayList<ABObject> allObjects = new ArrayList<ABObject>();
//        if(_wood != null)
//            allObjects.addAll(_wood);
//        if(_ice != null)
//            allObjects.addAll(_ice);
//        if(_stones != null)
//            allObjects.addAll(_stones);
//        for(ABObject obj : allObjects)
//        {
//            List<ABObject> supports = ABUtil.getSupporters(obj,allObjects);
//            List<ABObject> supportees = ABUtil.getSupportees(obj,allObjects);
//            obj.supporters = supports;
//            obj.supportees = supportees;
//        }
//    }




    public void generateAirBlocks(double[][][] structure,Rectangle boundingRectangle)
    {
        boolean[][] air = isAir(structure);
        for(int i = 0;i<air.length;i++)
        {
            for(int j = 0;j<air.length;j++)
            {
                if(air[i][j])
                {
                    Rectangle mbr = new Rectangle((int)(boundingRectangle.x+i*(boundingRectangle.getWidth()/structure.length)),(int)(boundingRectangle.y+j*(boundingRectangle.getHeight()/structure.length)),(int)(boundingRectangle.getWidth()/structure.length),(int)(boundingRectangle.getHeight()/structure.length));
                    _air.add(new ABObject(mbr,ABType.Air));
                }
            }
        }

    }



    public boolean[][] isAir(double[][][] structure)
    {
        boolean[][] air = new boolean[structure.length][structure.length];
        for(int i = 0;i<structure.length;i++)
        {
            for(int j=0;j<structure[i].length;j++)
            {
                double pig_percentage = structure[i][j][0];
                double wood_percentage = structure[i][j][1];
                double ice_percentage = structure[i][j][2];
                double stone_percentage = structure[i][j][3];
                double tnt_percentage = structure[i][j][4];
                if(pig_percentage + wood_percentage + ice_percentage + stone_percentage + tnt_percentage<10)
                    air[i][j] = true;
                else
                    air[i][j]  = false;
            }
        }
        return air;
    }

    public void addAir()
    {
        allObjects.addAll(_air);
    }

    public void generateSubStructures()
    {
        if(this.allObjects==null) {
            ArrayList<ABObject> allBlocks = new ArrayList<ABObject>();
            if (_wood != null)
                allBlocks.addAll(_wood);
            if (_ice != null)
                allBlocks.addAll(_ice);
            if (_stones != null)
                allBlocks.addAll(_stones);

            SirMateSubStructure.allObjects = allBlocks;
            while (SirMateSubStructure.allObjects.size() > 0) {
                subStructures.add(new SirMateSubStructure());
                subStructures.get(subStructures.size() - 1).addBlocks(allBlocks.get(0));
            }

            int count = 1;
            for (SirMateSubStructure s : subStructures) {
//                System.out.println("count " + count);
                count++;
//                for (ABObject o : s.structureObjects) {
//                    System.out.println(o);
//                }
            }

//        for(SirMateSubStructure s : subStructures)
//        {
//            int maxX=Integer.MIN_VALUE;
//            int maxY=Integer.MIN_VALUE;
//            int minX=Integer.MAX_VALUE;
//            int minY=Integer.MAX_VALUE;
//            System.out.println("new");
//            for(ABObject obj: s.structureObjects)
//            {
//                if(minX>obj.getMinX())
//                    minX=(int)obj.getMinX();
//                if(maxX<obj.getMaxX())
//                    maxX=(int)obj.getMaxX();
//                if(minY>obj.getMinY())
//                    minY=(int)obj.getMinY();
//                if(maxY<obj.getMaxY())
//                    maxY=(int)obj.getMaxY();
//            }
            //System.out.println("minX "+minX+" minY "+minY+" maxX "+maxX+" maxY "+maxY);
            //}
        }
        else
        {
            List<ABObject> allObjects = new LinkedList<ABObject>();
            allObjects.addAll(this.allObjects);
            SirMateSubStructure.allObjects = allObjects;
            while (SirMateSubStructure.allObjects.size() > 0) {
                subStructures.add(new SirMateSubStructure());
                subStructures.get(subStructures.size() - 1).addBlocks(allObjects.get(0));
            }
//            for(int i=0;i<subStructures.size();i++)
//            {
//                if(subStructures.get(i).structureObjects.size()==0)
//                    subStructures.remove(i);
//            }
            int count = 1;
            for (SirMateSubStructure s : subStructures) {
//                System.out.println("count " + count);
                count++;
//                for (ABObject o : s.structureObjects) {
//                    System.out.println(o);
//                }
            }
        }
    }

    public double slopOfLine(Line2D line)
    {
        return (line.getY2()-line.getY1())/(line.getX2()-line.getX1());
    }
    public ABObject nextBlockPredictor(ABObject currentBlock, Line2D line)
    {
        return null;
    }
    int standard=10000;

    public void downwardsFactor()
    {
        //Rectangle boundingRectangle;
        ArrayList<ABObject> allBlocks=new ArrayList<ABObject>();
        if(allObjects==null){
        for(ABObject obj: _stones)
            allBlocks.add(obj);
        for(ABObject obj: _wood)
            allBlocks.add(obj);
        for(ABObject obj: _ice)
            allBlocks.add(obj);
        for(ABObject obj: _tnt)
            allBlocks.add(obj);}
        else
        allBlocks.addAll(allObjects);

        //int ground=vision.getVisionRealShape().getGround();

        for(ABObject obj: allBlocks)
        {
//            int sumOfLateralDistance=0;
            int sumOfDensity=0;
            Line2D verticalLine=new Line2D.Double(obj.getCenter(), new Point2D.Double(obj.getCenterX(),ground));

            int cnt=0;
            for(ABObject block: allBlocks)
            {
                cnt=0;
                if(verticalLine.intersects(block) || verticalLine.contains(block))
                {
                    sumOfDensity+=getBlockDensity(block);
                    cnt++;
                }
            }

            for(ABObject pig: _pigs)
            {
                if(pig.getMinY()>obj.getMaxY())
                {
//                    sumOfLateralDistance += Math.abs(pig.getCenterX() - verticalLine.getX1());   // because for a vertical line, x1=x2
                    Line2D lineToPig=new Line2D.Double(pig.getCenter(),new Point2D.Double(verticalLine.getX1(),pig.getCenterY()));
                    for(ABObject blockBetweenPig: allBlocks)
                    {
                        if(lineToPig.intersects(blockBetweenPig) || lineToPig.contains(blockBetweenPig))
                        {
                            sumOfDensity+=getBlockDensity(blockBetweenPig);
                            cnt++;
                        }
                    }

                }
            }

            if(sumOfDensity!=0 && cnt!=0)
                sumOfDensity/=cnt;

            obj.downwardsFactorValue=sumOfDensity;

        }
    }


    public double getLastX()
    {

        List<ABObject> blocks = new ArrayList<ABObject>(0);
        if(this.allObjects==null){
        if(_pigs != null)
            blocks.addAll(_pigs);
        if(_wood != null)
            blocks.addAll(_wood);
        if(_ice != null)
            blocks.addAll(_ice);
        if(_stones != null)
            blocks.addAll(_stones);}
        else
        blocks.addAll(allObjects);

        double max=0.0;
        for(ABObject r:blocks)
        {
            double last=r.getX()+r.getWidth();
            if(last>max)
                max=last;
        }

        return max;
    }

    public void getDisplacement()
    {
        List<ABObject> allRect = new ArrayList<ABObject>(0);
        if(this.allObjects==null){
            if(_pigs != null)
                allRect.addAll(_pigs);
            if(_wood != null)
                allRect.addAll(_wood);
            if(_ice != null)
                allRect.addAll(_ice);
            if(_stones != null)
                allRect.addAll(_stones);
            if(_air != null)
                allRect.addAll(_air);}
        else
            allRect.addAll(this.allObjects);

        for(ABObject obj: allRect)
        {
            if(obj.type!=ABType.Air) {
                Line2D.Double line = new Line2D.Double(obj.getCenterX(), obj.getCenterY(), getLastX(), obj.getY());

                int weight = 0;
                int standard = 10000;
                //ABType currentBird = aRobfindCurrentBird();
                if (currentBird == ABType.RedBird)//Red Bird
                {
//                    System.out.println("red bird selected");
                    //for(Point2D v: vertices)

                    //{
                    //System.out.println("Candidate: "+obj);
                    for (ABObject blocks : allRect) {

                        //int area = blocks.width * blocks.height;
                        int area = Math.min(blocks.width, blocks.height);
                        //System.out.println(line.intersects(blocks));
                        if (line.intersects(blocks)) {
                            //System.out.println(blocks);
                            //System.out.println("redcalled");
                            if (blocks.type == ABType.Wood) {

                                weight += 1 / (LOW * area);

                                //System.out.println(weight);
                            } else if (blocks.type == ABType.Ice)
                                weight += 1 / (MEDIUM * area);
                            else if (blocks.type == ABType.Stone)
                                weight += 1 / (HIGHER * area);
                            else if (blocks.type == ABType.Air)
                                weight += standard / blocks.getX();
                            else {
                                continue;
                            }
                            //System.out.println(weight/area);

                        }
                    }
                    //}
                } else if (currentBird == ABType.BlueBird) //Blue Bird
                {
                    //for(Point2D v: vertices)
                    //{
                    for (ABObject blocks : allRect) {
                        //int area = blocks.width * blocks.height;
                        int area = Math.min(blocks.width, blocks.height);
                        if (line.intersects(blocks)) {
                            if (blocks.type == ABType.Wood)
                                weight += 1 / (MEDIUM * area);
                            else if (blocks.type == ABType.Ice)
                                weight += 1 / (LOW * area);
                            else if (blocks.type == ABType.Stone)
                                weight += 1 / (HIGHER * area);
                            else if (blocks.type == ABType.Air)
                                weight += standard / blocks.getX();
                            else
                                continue;
                        }
                    }
                    //}

                } else if (currentBird == ABType.YellowBird) // Yellow Bird
                {
                    //for(Point2D v: vertices)
                    //{
                    for (ABObject blocks : allRect) {
                        int area = Math.min(blocks.width, blocks.height);
                        if (line.intersects(blocks)) {
                            if (blocks.type == ABType.Wood)
                                weight += 1 / (LOW * area);
                            else if (blocks.type == ABType.Ice)
                                weight += 1 / (MEDIUM * area);
                            else if (blocks.type == ABType.Stone)
                                weight += 1 / (HIGHER * area);
                            else if (blocks.type == ABType.Air)
                                weight += standard / blocks.getX();
                            else
                                continue;
                        }
                    }
                    //}
                } else if (currentBird == ABType.WhiteBird) // White Bird
                {
                    //for(Point2D v: vertices)
                    //{
                    for (ABObject blocks : allRect) {
                        int area = Math.min(blocks.width, blocks.height);
                        if (line.intersects(blocks)) {
                            if (blocks.type == ABType.Wood)
                                weight += 1 / (HIGHER * area);
                            else if (blocks.type == ABType.Ice)
                                weight += 1 / (MEDIUM * area);
                            else if (blocks.type == ABType.Stone)
                                weight += 1 / (LOW * area);
                            else if (blocks.type == ABType.Air)
                                weight += standard / blocks.getX();
                            else
                                continue;
                        }
                    }
                    //}

                } else //Black Bird
                {
                    //for(Point2D v: vertices)
                    //{
                    for (ABObject blocks : allRect) {
                        int area = Math.min(blocks.width, blocks.height);
                        if (line.intersects(blocks)) {
                            if (blocks.type == ABType.Wood)
                                weight += 1 / (LOW * area);
                            else if (blocks.type == ABType.Ice)
                                weight += 1 / (MEDIUM * area);
                            else if (blocks.type == ABType.Stone)
                                weight += 1 / (HIGHER * area);
                            else if (blocks.type == ABType.Air)
                                weight += standard / blocks.getX();
                            else
                                continue;
                        }
                    }
                    //}
                }
//                System.out.println("weight="+weight);
                obj.displacementWeight = weight;
            }
        }

    }



    public int assignDensity(ABObject block, int wood, int ice, int stone,int air,int tnt,int pig)
    {
        int weight=0;
        //int area = Math.min(block.width,block.height);
        if(block.type==ABType.Wood) {
            weight += (wood);
            
            hasAnyBlockCome=true;
        }
        else if(block.type==ABType.Ice) {
            weight += (ice);
            hasAnyBlockCome=true;
        }
        else if(block.type==ABType.Stone) {
            weight += (stone);
            hasAnyBlockCome=true;
        }
        else if(block.type==ABType.Air&& hasAnyBlockCome)

            weight += air;
        else if(block.type==ABType.TNT) {
            weight += tnt;
            hasAnyBlockCome=true;
        }
        else if(block.type==ABType.Pig) {
            weight += pig;
            hasAnyBlockCome=true;
        }



        return weight;
    }

    public void penetrationWeight()
    {
        List<ABObject> allRect = new ArrayList<ABObject>(0);
        if(this.allObjects==null){
        if(_pigs != null)
            allRect.addAll(_pigs);
        if(_wood != null)
            allRect.addAll(_wood);
        if(_ice != null)
            allRect.addAll(_ice);
        if(_stones != null)
            allRect.addAll(_stones);
        if(_air != null)
            allRect.addAll(_air);}
        else
        allRect.addAll(allObjects);

        for(ABObject obj: allRect)
        {

            if(obj.type!=ABType.Air){
            int weight=1;
            hasAnyBlockCome = false;
            TrajectoryPlanner trajectory = new TrajectoryPlanner();

            ArrayList<Point> releasePoints = trajectory.estimateLaunchPoint(_slingShot,new Point((int) obj.getX(), (int) obj.getY()));

            for(Point p:releasePoints) {

                List<ABObject> trajectoryBlocks = new ArrayList<ABObject>(0);
                if(this.allObjects==null){
                if (_wood != null)
                    trajectoryBlocks.addAll(_wood);
                if (_ice != null)
                    trajectoryBlocks.addAll(_ice);
                if (_stones != null)
                    trajectoryBlocks.addAll(_stones);
                if (_air != null)
                    trajectoryBlocks.addAll(_air);}
                else
                trajectoryBlocks.addAll(this.allObjects);
                List<ABObject> removedBlocks = new LinkedList<ABObject>();
                trajectory.setTrajectory(_slingShot, p);
                ArrayList<Point> trajectory_points = trajectory._trajectory;
                //System.out.println(trajectory_points.size());
                for (Point t : trajectory_points) {
                    int flag = 0;
                    for (int i = 0; i < _hill.size(); i++) {
                        Poly h = (Poly) _hill.get(i);
                        if (h.polygon.contains(t) && t.getX() <= obj.getX()) {
                            flag = 1;
                            weight = Integer.MIN_VALUE;
                            break;
                        }
                    }

                    if (flag == 1)
                        break;
                    int length = 0;
                    for (int i=0;i<trajectoryBlocks.size();i++) {
                        if (trajectoryBlocks.get(i) != obj && trajectoryBlocks.get(i).getX() <= obj.getX() && trajectoryBlocks.get(i).contains(t)) {
                            /* WOOD, ICE, STONE */
                            ABObject blocks = trajectoryBlocks.get(i);
                            if(blocks.angle>0.785)
                                length+=Math.min(blocks.width,blocks.height);
                            else
                                length+=Math.min(blocks.width,blocks.height);
                            if (currentBird == ABType.RedBird)//Red Bird
                                weight += assignDensity(blocks,LOW,MEDIUM,HIGHER,5,2,20 )*length;
                            else if (currentBird == ABType.BlueBird) //Blue Bird
                                weight += assignDensity(blocks, MEDIUM, LOW, HIGHER,5,2,10)*length;
                            else if (currentBird == ABType.YellowBird) // Yellow Bird
                                weight += assignDensity(blocks, LOW, MEDIUM, HIGHER,5,2,10)*length;
                            else if (currentBird == ABType.WhiteBird) // White Bird
                                weight += assignDensity(blocks, HIGHER, MEDIUM, LOW,5,2,10)*length;
                            else //Black Bird
                                weight += assignDensity(blocks, LOW, MEDIUM, LOW,5,2,5)*length;

//                            System.out.println("WEIGHT_____" +weight);
                            trajectoryBlocks.remove(blocks);
                            i--;
                            removedBlocks.add(blocks);
                        }
                    }
                    //trajectoryBlocks.addAll(removedBlocks);
                }
                trajectoryBlocks.addAll(removedBlocks);
                int k  = 100000;

                obj.penetrationWeight[releasePoints.indexOf(p)] = k/weight;
                
            }
                if(releasePoints.size()<2)
                    obj.penetrationWeight[1]=obj.penetrationWeight[0];

            }
                       

        }
    }

    public ArrayList<ABObject> getFinalCandidateBlocks() {

        /*
        * y=0 for bottomUp & y=1 for topDown
        * ABObject[x][y], where x is the rank of block corresponding to y
        */
//    	System.out.println("Voila");
        ArrayList<ABObject> listOfFinalCandidateBlocks=new ArrayList<ABObject>();
        ArrayList<ABObject> allBlocks = new ArrayList<ABObject>(0);
        if(this.allObjects==null){
        if (_pigs != null)
            allBlocks.addAll(_pigs);
        if (_ice != null)
            allBlocks.addAll(_ice);
        if (_wood != null)
            allBlocks.addAll(_wood);
        if (_stones != null)
            allBlocks.addAll(_stones);
        if (_tnt != null)
            allBlocks.addAll(_tnt);}
        else
        {
            allBlocks.addAll(allObjects);
            allBlocks.addAll(_pigs);
            allBlocks.removeAll(_air);
        }

        ArrayList<Double> bottomUpFactors = new ArrayList<Double>(0);
        for (ABObject block : allBlocks)
            block.bottomUpFactorValue = (alphaBottomUp * block.penetrationWeight[0]) + (betaBottomUp * block.displacementWeight) + (gamma * block.supportWeight);
        for (ABObject block : allBlocks)
            block.topDownFactorValue = (alphaTopDown * block.penetrationWeight[1]) + (betaTopDown * block.displacementWeight) + (delta * block.downwardsFactorValue);

        //BottomUp
        for (int i = 0; i < allBlocks.size(); i++) {
            for (int j = 0; j < allBlocks.size() - 1; j++) {
                if (allBlocks.get(j).bottomUpFactorValue < allBlocks.get(j + 1).bottomUpFactorValue) {
                    ABObject temp = allBlocks.get(j + 1);//array[y+1];
                    allBlocks.remove(j + 1);
                    allBlocks.add(j + 1, allBlocks.get(j));
                    allBlocks.remove(j);
                    allBlocks.add(j, temp);
                }
            }
        }
        ArrayList<ABObject> bottomUp=new ArrayList<ABObject>();
        bottomUp.addAll(allBlocks);
//        System.out.println("______________________");
//        System.out.println("Bottom up list:");
//        for(ABObject obj:bottomUp)
//        {
//            System.out.println(obj);
//        }

            // topDown
        for (int i = 0; i < allBlocks.size(); i++) {
            for (int j = 0; j < allBlocks.size() - 1; j++) {
                if (allBlocks.get(j).topDownFactorValue < allBlocks.get(j + 1).topDownFactorValue) {
                    ABObject temp = allBlocks.get(j + 1);//array[y+1];
                    allBlocks.remove(j + 1);
                    allBlocks.add(j + 1, allBlocks.get(j));
                    allBlocks.remove(j);
                    allBlocks.add(j, temp);
                }
            }
        }
//        System.out.println("______________________");
//        System.out.println("Topdown list:");
        ArrayList<ABObject> topDown=new ArrayList<ABObject>();
        topDown.addAll(allBlocks);
//        for(ABObject obj: allBlocks)
//        {
//            System.out.println(obj);
//
//        }
//        System.out.println("__________________________");

//        int bottom=0;
//        int top=0;
        int size = allBlocks.size();
        double sum = 0;
        
        for(ABObject obj:bottomUp)
        {
        	 sum = sum + obj.bottomUpFactorValue;
        	 
        }
        double average = sum/size ;
//        System.out.println("Bottom"+average +"  "+allBlocks.size()+" "+sum);
//        System.out.println("AT 0 = "+bottomUp.get(0).bottomUpFactorValue);
        for(ABObject obj:bottomUp)
        {
            obj.deltaBottomUp = ( obj.bottomUpFactorValue - bottomUp.get((size-1)).bottomUpFactorValue)/average;
//            System.out.println("________"+obj.deltaBottomUp+"   "+obj.bottomUpFactorValue);
        }
        sum =0;
        for(ABObject obj: topDown)
        {
           sum = sum + obj.topDownFactorValue;
        }
        average = sum/size;
        for(ABObject obj: topDown)
        {
            obj.deltaTopDown = (obj.topDownFactorValue - topDown.get((size-1)).topDownFactorValue)/average;
//            System.out.println("_________ "+obj.deltaTopDown+"   "+obj.topDownFactorValue);
            obj.avaerageDelta = (obj.deltaBottomUp + obj.deltaTopDown)/2;
        }

        ArrayList<ABObject> finalList = new ArrayList<ABObject>();
        finalList.addAll(topDown);
        for(int i=0;i<finalList.size()-1;i++)
        {
            for(int j=i+1;j<finalList.size();j++)
            {
            	
                if(finalList.get(j).avaerageDelta<finalList.get(i).avaerageDelta)
                {
                	
                    ABObject temp = finalList.get(j);
                    finalList.set(j,finalList.get(i));
                    finalList.set(i,temp);
                   
                }
            }
        }
//        for(ABObject obj: finalList)
//        {
//            System.out.println("AverageDelta of "+obj.id+"=  "+obj.avaerageDelta);
//
//        }
        
        
//        System.out.println("Final List size:"+finalList.size());


        return finalList;

    }

    public int getMaxScore()
    {
        int maxScore=0;
        int pigScore=5000;
        int tntScore=500;
        int woodScore=500;
        int iceScore=500;
        int stoneScore=1500;

        ArrayList<ABObject> allBlocks=new ArrayList<ABObject>();
        if(_pigs!=null)
            allBlocks.addAll(_pigs);
        if(_wood!=null)
            allBlocks.addAll(_wood);
        if(_ice!=null)
            allBlocks.addAll(_ice);
        if(_stones!=null)
            allBlocks.addAll(_stones);
        if(_tnt!=null)
            allBlocks.addAll(_tnt);

        for(ABObject block: allBlocks)
        {
            if(block.type==ABType.Pig)
                maxScore+=pigScore;
            else if(block.type==ABType.TNT)
                maxScore+=tntScore;
            else
            {
                if(block.type==ABType.Stone)
                    maxScore+=stoneScore;
                else if(block.type==ABType.Wood)
                    maxScore+=woodScore;
                else if(block.type==ABType.Ice)
                    maxScore+=iceScore;
                else if(block.type==ABType.TNT)
                    maxScore+=tntScore;
            }
        }
        return maxScore;
    }

    public static void trainCoefficients(double normalizedScore,boolean isBottomup)
    {
        if(isBottomup) {
            bottomUpMean = (bottomUpMean * bottomUpNormalizedScores.size() + normalizedScore) / (bottomUpNormalizedScores.size() + 1);
            bottomUpNormalizedScores.add(normalizedScore);
            double[] diff = new double[bottomUpNormalizedScores.size()];
            for (int i = 0; i < bottomUpNormalizedScores.size(); i++) {
                diff[i] = Math.pow(bottomUpNormalizedScores.get(i) - bottomUpMean, 2);
            }
            double variance = 0;
            for (int i = 0;i<diff.length; i++) {
                variance += diff[i];
            }
            variance = variance / bottomUpNormalizedScores.size();
            bottomUpStandardDeviation = Math.sqrt(variance);

            if (flagBottomUp == 1) {
                if (bottomUpStandardDeviation > threshold) {
                    alphaBottomUp -= changeValue;
                    gamma += changeValue;
                    count=-1;
                    //naiveAgent.run();
                }
            } else if (flagBottomUp == 2) {
                if (bottomUpStandardDeviation > threshold) {
                    alphaBottomUp -= changeValue;
                    betaBottomUp += changeValue;
                    count=-1;
                    //naiveAgent.run();
                }
            } else if (flagBottomUp == 3) {
                if (bottomUpStandardDeviation > threshold) {
                    gamma -= changeValue;
                    betaBottomUp += changeValue;
                    count=-1;
                    //naiveAgent.run();

                }
                count++;
                isFlipped(isBottomup);
            }
        }
        else
        {
            topDownMean = (topDownMean * topDownNormalizedScores.size() + normalizedScore) / (topDownNormalizedScores.size() + 1);
            topDownNormalizedScores.add(normalizedScore);
            double[] diff = new double[topDownNormalizedScores.size()];
            for (int i = 0; i < topDownNormalizedScores.size(); i++) {
                diff[i] = Math.pow(topDownNormalizedScores.get(i) - topDownMean, 2);
            }
            double variance = 0;
            for (int i = 0; i <diff.length; i++) {
                variance += diff[i];
            }
            variance = variance / topDownNormalizedScores.size();
            topDownStandardDeviation = Math.sqrt(variance);

            //int flag = naiveAgent.flag;

            if (flagTopDown == 1) {
                if (topDownStandardDeviation > threshold) {
                    alphaTopDown -= changeValue;
                    delta += changeValue;
                    count=-1;
                    //naiveAgent.run();

                }
            } else if (flagTopDown == 2) {
                if (topDownStandardDeviation > threshold) {
                    alphaTopDown -= changeValue;
                    betaTopDown += changeValue;
                    count=-1;
                    //naiveAgent.run();
                }
            } else if (flagTopDown == 3) {
                if (topDownStandardDeviation > threshold) {
                    delta -= changeValue;
                    betaBottomUp += changeValue;
                    count=-1;
                    //naiveAgent.run();
                }
            }
            count++;
            isFlipped(isBottomup);
        }


    }

    public static boolean isFlipped(boolean block)
    {
        if(block)
        {
            if(flagBottomUp==0)             // alpha & gamma
            {
                if(alphaBottomUp==0.25 && gamma==0.65)
                {
                    alphaBottomUp=0.65;
                    gamma=0.25;
                    flagBottomUp++;
                    return true;
                }
            }
            else if(flagBottomUp==2)        // alpha && beta
            {
                if(alphaBottomUp==0.10 && betaBottomUp==0.65)
                {
                    alphaBottomUp=0.65;
                    betaBottomUp=0.10;
                    flagBottomUp++;
                    return true;
                }
            }
            else if(flagBottomUp==3)        // beta && gamma
            {
                if(betaBottomUp==0.25 && gamma==0.10)
                {
                    betaBottomUp=0.10;
                    gamma=0.25;
                    flagBottomUp++;
                    return true;
                }
            }
            else
                return false;
        }
        else
        {
            if(flagTopDown==1)             // alpha & delta
            {
                if(alphaTopDown==0.20 && delta==0.65)
                {
                    alphaTopDown=0.65;
                    delta=0.20;
                    flagTopDown++;
                    return true;
                }
            }
            else if(flagTopDown==2)      // alpha && beta
            {
                if(alphaTopDown==0.15 && betaTopDown==0.65)
                {
                    alphaTopDown=0.65;
                    betaTopDown=0.15;
                    flagTopDown++;
                    return true;
                }
            }
            else if(flagTopDown==3)        // beta && gamma
            {
                if(betaTopDown==0.20 && delta==0.15)
                {
                    betaTopDown=0.15;
                    delta=0.20;
                    flagTopDown++;
                    return true;
                }
            }
            else
                return false;
        }

        return false;
//        public static double alpha=0.65;
//        public static double betaBottomUp=0.10;
//        public static double betaTopDown=0.15;
//        public static double gamma=0.25;
//        public static double delta=0.20;
//        private static  double changeValue=5;

    }
//    private static double getStandardDeviation()
//    {
//        double standardDeviation=0;
//
//
//
//        return standardDeviation;
//    }

//-----------------------------------------------------------------------
//@created by Anchit Dave
    
    public List<Point> traverseAllReleasePoints(ABObject block){
    	//Structure Type
    	boolean horizontal = true;
    	int width = block.width;
    	int height = block.height;
    	
    	if(width<height)
    		horizontal = false;
    	//--------------
    	List<Point> possiblePoints = new LinkedList<Point>();
    	Point left = block.getLeftEnd(block.getCenter(), block.width, block.height);
        Point right = block.getRightEnd(block.getCenter(), block.width, block.height);
        boolean check = true;
        int count=0;
        //If Top Down Shot
        for(double i = left.getY();i<= right.getY();i=i+6  ){
        	count++;
        	Point temp = new Point((int)(left.getX()),(int)(i));
        	possiblePoints.add(temp);
        	check = true;
        	if((temp.getY()+6)>right.getY()){
        		if(temp.getY()==right.getY()){
        			check = false;
        		}
        		if(check){
        			temp = new Point((int)(left.getX()),(int)(right.getY()));
        			possiblePoints.add(temp);
        		}
        		
        	}
           /* TrajectoryPlanner trajectory = new TrajectoryPlanner();
            ArrayList<Point> releasePoints = new ArrayList<Point>();
            for(int  =0;i<count;i++){
            releasePoints = trajectory.estimateLaunchPoint(_slingShot,possiblePoints.get(i));
            }
            System.out.println("release Point -- "+releasePoints);*/
        }
        
        //If Bottom Up shot
        for(double i = (left.getX()+6);i<= right.getX();i=i+6 ){
        	Point temp = new Point((int)(i),(int)(right.getY()));
        	possiblePoints.add(temp);
        	check = true;
        	if((temp.getX()+6)>(right.getX())){
        		if(temp.getX()==right.getX()){
        			check = false;
        		}
        		if(check){
        			temp = new Point((int)(right.getX()),(int)(right.getY()));
        			possiblePoints.add(temp);
        		}
        		
        	}
        }
        return possiblePoints;
    }
    
    public Point structureType(ABObject block){
    	return new Point((int)block.getCenterX()-(block.width/2),(int)block.getCenterY());
    }
    
    
    public void mostFeasibleReleasePoint(){
    	
    }
    
    
    /**
	*	estimates the objects that are in the trajectory
	*/
	public static List<ABObject> estimateObjectsInTheWay(Rectangle sling, List<ABObject> objects, Point targetPoint, ABObject targetObject, Point releasePoint, int range)
	{ 
		List<Point> trajectoryPoints = tp.predictTrajectory(sling, releasePoint);
		
		return estimateObjectsInTheWay(trajectoryPoints, objects, targetPoint, targetObject, range);
	} 
//	------------------------------------------xxxxxxxxxxxxxxxxxxx-----------------------------------xxxxxxxxxxxxxx----------------------
	
	/**
	*	estimates the objects that are in the trajectory
	*/
	public static List<ABObject> estimateObjectsInTheWay(List<Point> trajectoryPoints, List<ABObject> objects, Point targetPoint, ABObject targetObject, int range)
	{ 
		List<ABObject> foundObjects = new ArrayList<ABObject>();

		for (Point point : trajectoryPoints)
		{
			boolean reachedTarget = false;

			if (point.x < 840 && point.y < 480 && point.y > 100 && point.x > 200)
			{
				for (ABObject ab: objects)
				{
					if (pointHitsObjectWithinARange(ab, point, range)
						&& !pointHitsObjectWithinARange(targetObject, point, range))
					{
						if (!foundObjects.contains(ab))
						{
							ab.trajectoryHitPoint = point;
							foundObjects.add(ab);
						}
					}
					else if (pointHitsObjectWithinARange(targetObject, point, range))
					{
						reachedTarget = true;
						break;
					}
				}
			}

			if (reachedTarget)
				break;
		}
		
		return foundObjects;
	}
	
	/**
	* @return true if a point hits object within a range (point included) - the range is usually the bird's radius
	*/
	public static boolean pointHitsObjectWithinARange(ABObject object, Point point, int range)
	{
		if (object.contains(point))
			return true;
		
		int pX = point.x;
		int pY = point.y;
		
		if (range > 0
			&& (object.contains(new Point(pX - range, pY))
				|| object.contains(new Point(pX + range, pY))
				|| object.contains(new Point(pX, pY + range))
				|| object.contains(new Point(pX, pY - range))
				)
			)
			return true;
		
		return false;
	}
	
	public static ABObject findTheFirstBlock(Rectangle sling, List<ABObject> objects, Point targetPoint, ABObject targetObject, Point releasePoint, int range){
		List<ABObject> inTheWay = new LinkedList<ABObject>();
		inTheWay = (estimateObjectsInTheWay(sling,objects,targetPoint,targetObject,releasePoint,range));
//		System.out.println(inTheWay);
		int temp = 0;
//		estimate the first block on the trajectory
		if(inTheWay.size()==0)
			return targetObject;
		int small = (int)inTheWay.get(0).getCenterX();
		for(int i=0;i<inTheWay.size();i++){
			int x = (int)inTheWay.get(i).getCenterX();
			if(x<small){
				small=x;
				temp=i;
			}
		}
		
		return inTheWay.get(temp);
		
	}
	
	public Point hillHitPoint(Rectangle sling,Point releasePoint,List<ABObject> allHills,ABObject targetObject){
		Point hillHitPoint = new Point();
		/*if(allHills.size()==0)
			return hillHitPoint=new Point(0,0);
		List<Point> trajectoryPoints = tp.predictTrajectory(sling, releasePoint);
		for(ABObject o:allHills){
			for(Point p:trajectoryPoints){
				Point extremeleft = new Point(o.getLeftEnd(o.getCenter(), o.width, o.height));
				Point extremeRight = new Point(o.getRightEnd(o.getCenter(), o.width, o.height));
				for(int i = extremeleft.y;i<(extremeleft.y+o.height);i++){
					if(p.x==extremeleft.x&&p.y==i)
						hillHitPoint = new Point(p.x,p.y);
				}
				for(int i = extremeRight.x;i>(extremeRight.x-o.width);i--){
					if(p.x==i&&p.y==extremeRight.y)
						hillHitPoint = new Point(p.x,p.y);
				}
				
		}*/
		List<Point> trajectoryPoints = tp.predictTrajectory(sling, releasePoint);
		for(Point p:trajectoryPoints){
			List<Point> estimatefor = new LinkedList();
            List<ABObject> estimatedobjectsfor = new LinkedList();
			estimatefor.add(p);
			estimatedobjectsfor = estimateObjectsInTheWay(sling,allHills, p,targetObject,releasePoint,1);
			if(estimatedobjectsfor.size()!=0){
				hillHitPoint = p;
				break;
			}
		}
//		System.out.println("-------========--------");

//		System.out.println(hillHitPoint);
		return hillHitPoint;
		
	}
	










}
