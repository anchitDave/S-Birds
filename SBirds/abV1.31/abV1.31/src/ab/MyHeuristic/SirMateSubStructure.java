package ab.MyHeuristic;

import ab.utils.ABUtil;
import ab.vision.ABObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hitarth on 29-07-2014.
 */
public class SirMateSubStructure
{

    public ArrayList<ABObject> structureObjects;
    public static List<ABObject> allObjects;
    public SirMateSubStructure()
    {
        //this.allObjects = allObjects;
        structureObjects = new ArrayList<ABObject>();

    }

    public void addBlocks(ABObject obj) {
        List<ABObject> supports = ABUtil.getSupporters(obj, allObjects);
        List<ABObject> supportees = ABUtil.getSupportees(obj, allObjects);
        structureObjects.add(obj);
        allObjects.remove(obj);
        //boolean check1 = true;
        //boolean check2 = true;
        for (ABObject o2 : supports) {
            if (!structureObjects.contains(o2)) {
                //System.out.println("added :"+o2);
                //structureObjects.add(o2);
                //allObjects.remove(o2);
                addBlocks(o2);
                //check1 = false;
            }
        }

        for (ABObject o2 : supportees)
        {
            if(!structureObjects.contains(o2))
            {
                //System.out.println("added :"+o2);
                //structureObjects.add(o2);
                //allObjects.remove(o2);
                addBlocks(o2);
            }
        }

    }

    public boolean contains(ABObject block)
    {
        return structureObjects.contains(block);
    }

    public void addSupport(ABObject obj)
    {
        List<ABObject> supports = ABUtil.getSupporters(obj, allObjects);


    }

    public void addSupportee(ABObject obj)
    {
        List<ABObject> supportees = ABUtil.getSupportees(obj, allObjects);
    }

}
