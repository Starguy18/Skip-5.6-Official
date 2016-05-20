package org.usfirst.frc.team3467.robot.subsystems.Vision;

import edu.wpi.first.wpilibj.AnalogInput;


public class PixyCam {

	private AnalogInput Pixy;
	
	public PixyCam(){
		Pixy = new AnalogInput(2);
	}
	
	public int read(){
		return Pixy.getValue();
	}
	
	public AnalogInput getPixy(){
		return Pixy;
	}
}