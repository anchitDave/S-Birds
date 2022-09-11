package ab.demo;

public class TrainerEntry {
	// the entry of the software.
	public static void main(String args[])
	{
		
		
		String command = "";
		if (args.length > 0)
		{
			command = args[0];
			if (args.length == 1 && command.equalsIgnoreCase("-nasc"))
			{
				TrainerNaiveClientAgent na = new TrainerNaiveClientAgent();
				na.run();
			} 
			else 
				if (args.length == 2 && command.equalsIgnoreCase("-nasc"))
				{
					TrainerNaiveClientAgent na = new TrainerNaiveClientAgent(args[1]);
					na.run();
				}
				else
					if(args.length == 3 && command.equalsIgnoreCase("-nasc"))
					{
						int id = Integer.parseInt(args[2]);
						TrainerNaiveClientAgent na = new TrainerNaiveClientAgent(args[1],id);
						na.run();
					}
		}
		else 
			System.out.println("Please input the correct command");
		System.exit(0);
	}
}

