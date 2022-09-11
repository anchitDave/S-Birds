package ab.MyHeuristic;

import ab.utils.ABUtil;
import ab.vision.ABObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hitarth on 28-06-2014.
 */
public class MyHeuristicEngine {
    private final int minimumGapBetweenBlocks = 4;
    ArrayList<SubStructure> subStructures;
    ArrayList<Integer> depth;
    ArrayList<Rectangle> TNTs;
    ArrayList<ABObject> pigs;
    ArrayList<ABObject> onlyBlocks;

//    public void findSubstructures(List<ABObject> objects)
//    {
//        while(pigs.size()>0)
//        {
//            for(SubStructure s : subStructures)
//            {
//                if(s.doesContain(pigs.get(0)))
//                {
//                    s.addPig(pigs.remove(0));
//                }
//            }
//            if(pigs.size()==0)
//                break;
//            ArrayList<ABObject> supports = ABUtil.getSupporters(pigs.get(0), onlyBlocks);
//            for(ABObject supp : supports)
//            {
//
//            }
//        }
//    }
}
