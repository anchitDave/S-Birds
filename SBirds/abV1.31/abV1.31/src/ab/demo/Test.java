package ab.demo;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Hitarth on 18-08-2014.
 */
public class Test {

    public static void main(String[] args)
    {
        List<Integer> l = new LinkedList<Integer>();
        l.add(1);
        l.add(2);
        l.add(3);
        l.add(4);
        l.add(5);
        List<Integer> l1 = new LinkedList<Integer>();
        for(int i=0;i<l.size();i++)
        {
            if(l.get(i)<=3)
            {
                Integer x = l.get(i);
                l1.add(x);
                l.remove(x);
                i--;
            }
        }
        for(Integer x:l)
        {
            System.out.println(x);
        }

        for(Integer x:l1)
        {
            System.out.println(x);
        }
    }
}
