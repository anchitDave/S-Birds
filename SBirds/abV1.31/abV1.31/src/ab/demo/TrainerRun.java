package ab.demo;

public class TrainerRun {
	public static void main(String[] args) {
		int level = 13;
		if(args.length!=0) {
			level = Integer.parseInt(args[0]);
		}
		TrainerNaiveAgent.main(level);
	}
}
