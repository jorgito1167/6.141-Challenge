package data_collection;

import com.sun.corba.se.spi.ior.MakeImmutable;

import orc.Orc;
import orc.QuadratureEncoder;

public class EncoderPair extends Thread {
    public long dt, prevTime, time;
    public long dLeft, prevLeft, left;
    public long dRight, prevRight, right;


    QuadratureEncoder leftEncoder;
    QuadratureEncoder rightEncoder;
	boolean ready;
    
    public EncoderPair() {
        Orc orc = Orc.makeOrc();
        leftEncoder = new QuadratureEncoder(orc, 0, false);
        rightEncoder = new QuadratureEncoder(orc, 1, true);
        
        sample();
        sample();
    }
    
    public void sample() {
      prevTime = time;
      prevLeft = left;
      prevRight = right;

      time = System.currentTimeMillis();
      right = rightEncoder.getPosition();
      left = leftEncoder.getPosition();

      dt = time - prevTime;
      dLeft = left-prevLeft;
      dRight = right-prevRight;
    }
    
    public String toString() {
        return "L: " + left + " R: " + right + " DL: " + dLeft + " DR: " + dRight ; 
    }
}
