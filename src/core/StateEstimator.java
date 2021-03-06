package core;

import java.util.ArrayList;

import utils.Utils;

import data_collection.DataCollection;
import data_collection.EncoderPair;
import logging.Log;
import map.Map;
import map.MapBlock;
import map.Pose;
import map.Robot;
import map.Segment;

public class StateEstimator implements Runnable {
    private static StateEstimator instance;

    private DataCollection dc;

    boolean ready = false;
    public boolean[] tooClose;
    public boolean anyTooClose;

    public int numCollectedBlocks;

    public int numBlocksLeft;

    public Map map;

    boolean started;

    private StateEstimator() {
        map = Map.getInstance();
        dc = DataCollection.getInstance();
        started = false;
    }

    public static StateEstimator getInstance() {
        if (instance == null)
            instance = new StateEstimator();
        return instance;
    }

    public void step() {
        updatePose();
        // updateBlocks();
        // sonarCheck();

        Log.log(this.toString());
    }

    public void updatePose() {
        if (dc.getEncoders() == null) {
            return;
        }
        EncoderPair enc = dc.getEncoders();

        double dl = enc.dLeft * Config.METERS_PER_TICK;
        double dr = enc.dRight * Config.METERS_PER_TICK;

        if (dr == 0 && dl == 0)
            return; // we haven't moved at all

        double dTheta = (dr - dl) / Config.WHEELBASE;
        Robot bot = Map.getInstance().bot;

        double newX = bot.pose.x + (dl + dr) * Math.cos(bot.pose.theta) / 2.0;
        double newY = bot.pose.y + (dl + dr) * Math.sin(bot.pose.theta) / 2.0;
        double newTheta = bot.pose.theta + dTheta;
        
        newTheta = Utils.wrapAngle(newTheta);
        
        Pose nextPose = new Pose(newX, newY, newTheta);
        bot.pose = nextPose;

         if (map.checkSegment(new Segment(bot.pose,nextPose),bot.pose.theta))
         bot.pose = nextPose;
    }

    public void updateBlocks() {

        MapBlock tempBlock;
        // for (Block b : dc.getBlocks()) {
        ArrayList<Block> blocks = dc.getBlocks();
        for (int b = blocks.size() - 1; b >= 0; b--) {
            tempBlock = new MapBlock(Map.getInstance().bot.getAbsolute(blocks.get(b).relX, blocks.get(b).relY),
                    blocks.get(b).color);

            map.addBlock(tempBlock);
        }
    }

    public void sonarCheck() {
        anyTooClose = false;
        if (tooClose == null)
            return;
        for (int i = 0; i < tooClose.length; i++) {
            tooClose[i] = (dc.getSonars().get(i).meas < Config.TOOCLOSE);
            if (tooClose[i])
                anyTooClose = true;
        }
    }

    // returns zero for no block, 1 for single block 2 for double block;
    public int getCaptureStatus() {
        boolean[] digitalIn = dc.getDigitalPins();
        boolean one = digitalIn[Config.ONE_BLOCK_PIN];
        boolean two = digitalIn[Config.TWO_BLOCK_PIN];

        if (one && two)
            return 2;
        if (one && !two)
            return 1;
        return 0;
    }

    public MapBlock getClosestBlock() {
        return map.closestBlock();
    }

    public String toString() {
        return map.bot.pose.toString();
    }

    @Override
    public void run() {
        while (true) {
            step();
        }

    }
}
