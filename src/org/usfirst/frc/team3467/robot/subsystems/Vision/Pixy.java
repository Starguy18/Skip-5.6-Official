package org.usfirst.frc.team3467.robot.subsystems.Vision;

import edu.wpi.first.wpilibj.Preferences;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.AnalogInput;

public class Pixy {
	
	private DigitalInput axis;
	private AnalogInput value;
	private Preferences m_prefs;

//Vision Processing Constants
	//Is the goal on target
	private boolean imageOnTarget = false;

	//Height of of the top of the Top Target
	private static final int TOP_TARGET_HEIGHT = 97;
	
	//Image Variables Axis Cameras
	private static final double Image_length_Axis = 320; //Pixels
	private static final double Image_height_Axis = 240; //Pixels
	
	//Camera Variables (Axis M1011) Original Camera
	private static final double M1011_FOVx = 47.0; //Degrees
	private static final double M1011_Height = 0.979; //Feet
	private static final double M1011_Pitch = 57.0; //Degrees
	
	//Camera Variables (Axis M1013) Second Camera 
	private static final double M1013_FOVx = 67.0; //Degrees
	private static final double M1013_length = 0.435; //Inches
	private static final double M1013_focus = 0.1102; //Inches
	
	//Pixy Camera Variables
	private static final double Pixy_FOVx = 75.0;  //Degrees
	private static final double Pixy_length = 0.0;  //Inches
	private static final double Pixy_focus = 0.0; //Inches
	
	//Image Variables Pixy Camera
	private static final double Image_length_Pixy = 1280.0; //Pixels
	private static final double Image_height_Pixy = 800.0; //Pixels
	
	//Target Variables
	private static final double Target_Length = 20; //Inches
	private static final double Target_Height = 12.0; //Inches
	
	//Calculated Values
	private static double angle_theta = 0.0;
	private static double distance_delta = 0.0;
	private static double changeinDistance = 0.0;
	private static double changeinAngle = 0.0;
	private static final double effectiveLeg = 241.7336;
	
	private static final double targetx = 150.1;
	private static final double targety = 0.0;
	private static double target_distance = 13.0;
	private static double target_angle = -8.5;
	
	//Tolerance values
	private static final double TOLERANCE_distance = 5.0;;
	private static final double TOLERANCE_angle = 0.5;
	
	//Image Matricies and Values
	
	private double Centerx = 0.0;;
	private double Centery = 0.0;
	
	public Pixy() {
		axis = new DigitalInput(0);
		
		value = new AnalogInput(0);
		
		System.out.println("Network Tabes initiated");
	}
	
	//Get values from Network Table and work with those values
	public void createImage () {			
		//Print values to SmartDashboard
		SmartDashboard.putNumber("Vision: Centerx", Centerx);
		SmartDashboard.putNumber("Vision: Centery", Centery);
		//SmartDashboard.putNumber("Vision: Width", Width);			
		//SmartDashboard.putNumber("Vision: Contours", centerx.length);				
	}
	
	//Get the specific values from the network table
	public double getCenterX() {
		return this.Centerx;
	}
	
	public double getCenterY() {
		return this.Centery;
	}

	
	public double getAngle_theta() {
		return angle_theta;
	}
	
	public double getDistance_delta() {
		return distance_delta;
	}
	
	public double getChangeinAngle() {
		System.out.println("Change in Angle " + changeinAngle);
		return changeinAngle;
	}
	
	public double getChangeinDistance() {
		return changeinDistance;
	}
	
	
	//Calibrate where the target should be
	public void setTarget_distnce(double distance) {
		target_distance = distance;
		m_prefs.putDouble("Ideal Distance", target_distance);
	}
	
	public void setTarget_angle(double angle) {
		target_angle = angle;
		m_prefs.putDouble("Ideal Angle", target_angle);
	}
	

	public void printData() {
		SmartDashboard.putNumber("Vision: Distance", distance_delta);
		SmartDashboard.putNumber("Vision: Angle", angle_theta);
		
		SmartDashboard.putNumber("Vision: Change in Angle", changeinAngle);
		SmartDashboard.putNumber("Vision: Change in Distance", changeinDistance);
	}
	
	
	//Calculations of distance and of position
	public boolean isOnPixel() {
		boolean onPixel = false;
		if ((Centerx - targetx) >=0 && (Centerx - targetx) <= 1/*Pixel Tolerance*/) {
			if (Centery - targety >= 0 && Centery - targety <=1/*Pixel Tolerance*/) {
				onPixel = true;
			}
		}
		return onPixel;
	}
	
	public void calculateTargetData() {
		imageOnTarget = false;
		
		//Calculates The Distance From the Target
			//distance_delta = (Target_Length_ft * M1013_FOVx_px)/(2 * Width * Math.tan(angle_theta));
			//distance_delta = (M1013_focus * Target_Height)/((Image_height_Axis - Height) * M1013_length/Image_height) + 0;
			//distance_delta = (M1013_focus * Target_Height)/((Image_height - Height) * M1013_length/Image_height) + 0;
		
		//Calculates The Angle of the Target
			//angle_theta = ((Centerx - Image_length/2)/(Image_length/2)) * M1011_FOVx;
			angle_theta = (Math.atan((Centerx - Image_length_Axis/2)/effectiveLeg) *180)/Math.PI;
		
		//Calculate the distances and angles needed to move
			changeinDistance = distance_delta - target_distance;
			changeinAngle = angle_theta - target_angle;
		
			printData();
	}
	
	public boolean isOnTarget() {
		if ((Math.abs(changeinDistance) >= 0 && Math.abs(changeinDistance) <= TOLERANCE_distance) &&
			 Math.abs(changeinAngle) >= 0 && Math.abs(changeinAngle) <= TOLERANCE_angle) {
			imageOnTarget = true;
		}
		else {
			imageOnTarget = false;
		}
		
		SmartDashboard.putBoolean("Vision: Image On Target", imageOnTarget);
		
		return imageOnTarget;
	}
}	
