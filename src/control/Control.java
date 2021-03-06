package control;

import core.Config;
import data_collection.EncoderPair;
import rrt.PathPlanning;
import map.Map;
import map.Point;
import map.Pose;
import map.Robot;
import map.Segment;
import uORCInterface.OrcController;
import utils.Utils;

public class Control {
    private static Control instance;
    
    private PathPlanning pp;
    private Robot bot;

    private WheelVelocityController leftController;
    private WheelVelocityController rightController;

    private PID rotPid;
    private PID velPid;

    public Control() {
    	OrcController orc = new OrcController(new int[]{0,1});
        this.pp = PathPlanning.getInstance();
        bot = Map.getInstance().bot;
        
        rotPid = new PID(.05, 0, 0, 0, .2);
        rotPid.start(0, 0);

        velPid = new PID(3, 0, 0, 0, .2);
        velPid.start(0, 0);
        
        leftController = new WheelVelocityController(orc, WheelVelocityController.LEFT);
        rightController = new WheelVelocityController(orc, WheelVelocityController.RIGHT);

    }
    
    public static Control getInstance() {
        if (instance == null)
            instance = new Control();
        return instance;   
    }
    
    private void setMotion(double vel, double rot) {
        setVelocity(-vel + rot, -vel - rot);
    }
    
    private void setVelocity(double left, double right) {    	
    	leftController.setVelocity(left);
        rightController.setVelocity(right);
        System.out.println("Set left controller to "+left+" Set right controller to "+right);
    }
    
    public void step() {
        goToWaypoint();
        leftController.step();
        rightController.step();
    }

    public void goToWaypoint() {
        Point wayPoint = pp.getNextWaypoint();
        
        if (wayPoint == null) {
        	setMotion(0,0);
        	return;
        }
        
        System.out.println("From: " + bot.pose + " to:" + wayPoint + " with theta " + bot.pose.angleTo(wayPoint));
        
        double distance = bot.pose.distance(wayPoint);
        double thetaErr = Math.toDegrees(Utils.thetaDiff(bot.pose.theta, bot.pose.angleTo(wayPoint)));

        double vel = velPid.step(distance);
        if (Math.abs(thetaErr) < 7)
        	vel *= (7-Math.abs(thetaErr)) / 7;
        else
            vel = 0;

        double rot = rotPid.step(-thetaErr);
        
        setMotion(vel, rot);
    }
    
    
    public static void main(String[] Args){
    	OrcController orc = new OrcController(new int[]{0,1});
    	WheelVelocityController leftController = new WheelVelocityController(orc, WheelVelocityController.LEFT);
    	WheelVelocityController rightController = new WheelVelocityController(orc, WheelVelocityController.RIGHT);
    	double theta = 0;
    	double x = 0;
    	double y = 0;
    	EncoderPair enc = new EncoderPair();
    	leftController.setVelocity(0.2);
		rightController.setVelocity(0.2);
		double l = 0;
		double r = 0;
		while (l < 1 || r < 1 ){
    		System.out.println("l = "+ l);
    		System.out.println("r = "+ r);
    		leftController.step();
    		rightController.step();
    		enc.sample();
    		/*
    		double dl = enc.dLeft * Config.METERS_PER_TICK;
    		double dr = enc.dRight * Config.METERS_PER_TICK;
    		double dTheta = (dr - dl) / Config.WHEELBASE;
    		double newTheta = theta + dTheta;
    		x = x + (dl + dr) * Math.cos(theta) / 2.0;
            y = y + (dl + dr) * Math.sin(theta) / 2.0;
    		theta = newTheta;
			*/
    
    		l = enc.left* Config.METERS_PER_TICK;
    		r = enc.right*Config.METERS_PER_TICK;
    		if (l >= 1){
    			
    			rightController.setVelocity(0);
    		}
    		if (r >= 1){
    			leftController.setVelocity(0);
    		}
    	}
    	
		System.out.println(l);
    }
}
