package org.usfirst.frc.team3467.robot.subsystems.Shooter;

import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.AnalogPotentiometer;
import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.Preferences;
import edu.wpi.first.wpilibj.command.PIDSubsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

import org.usfirst.frc.team3467.robot.RobotMap;
import org.usfirst.frc.team3467.robot.subsystems.Brownout.Brownout;
import org.usfirst.frc.team3467.robot.subsystems.Brownout.PowerConsumer;
import org.usfirst.frc.team3467.robot.subsystems.Shooter.commands.ShooterReset;

/*
	How the Shooter works:
	
	Manual mode:  Left stick Y axis controls the reset bar.
	
	PID mode: PID loop runs continuously and responds to changes in setpoint 
	made by calls to latch() and clear()
 
 */

public class Shooter extends PIDSubsystem implements PowerConsumer {

	// Controls display to SmartDashboard
	private static final boolean debugging = true;
	
	// Talon PDP channel number
	private static final int WINCH_PDP_CHANNEL = 0;
	
	// Brownout power level
	private Brownout.PowerLevel powerlevel = Brownout.PowerLevel.Normal;
	boolean dontShoot = false;
	
	//Catapult Objects
	private CANTalon m_resetBar;
	private DoubleSolenoid m_catLatch;
	private AnalogPotentiometer m_resetAngle;
	
	//PID Constants
	private static final double SHOOT_P = 25.0;
	private static final double SHOOT_I = 0.0;
	private static final double SHOOT_D = 0.0;
	
	private static final double TOLERANCE = 0.01;
	
	// PID variables
	private double m_resetBarSetpoint;
	private boolean m_usePID;

	// Reset bar setpoints
	private double m_clearPoint = 0.69;  // bar is out of the way of catapult
	private double m_latchPoint = 0.30; // bar is holding catapult so it can be latched
	private double m_deltaPot = 0.40; //Change in pot signal between clear and latch points
	// The roboRio Preferences
	Preferences m_prefs = Preferences.getInstance();
	
	// If true, reverse solenoid outputs (3 is release, 4 is latch)
	private boolean m_isCompBot = false;
	
	// Has the robot been calibrated
	private boolean m_hasBeenCalibrated = false;
	
	//Shooter Constructor
	public Shooter(boolean IScompBot) {
	
		super("Shooter", SHOOT_P, SHOOT_I, SHOOT_D);

		m_isCompBot = IScompBot;
		
		m_resetAngle = new AnalogPotentiometer(new AnalogInput(RobotMap.catapult_potentiometer_port));
		m_resetBar = new CANTalon(RobotMap.catapult_Talon);
		
		m_resetBar.enableBrakeMode(true);
		
	//	if (m_isCompBot) {
			m_catLatch = new DoubleSolenoid(RobotMap.catapult_solenoid_latch, RobotMap.catapult_solenoid_release);
		//}
	//	else {
		//	m_catLatch = new DoubleSolenoid(RobotMap.catapult_solenoid_release, RobotMap.catapult_solenoid_latch);
	//	}
		//	m_cataLatch = new DoubleSolenoid(RobotMap.catapult_solenoid_latch, ,)
		
		// Start with setpoint at the current potentiometer reading 
		m_resetBarSetpoint = m_resetAngle.get();
		m_usePID = false;
		this.setAbsoluteTolerance(TOLERANCE);
		
		// Update reset bar setpoints from Preferences
		double cp = m_clearPoint; double lp = m_latchPoint;
		m_clearPoint = m_prefs.getDouble("Shooter Clear Point", cp);
		m_latchPoint = m_prefs.getDouble("Shooter Latch Point", lp);
		
		// Update PID gains from Preferences
		double p, i, d;
		p = m_prefs.getDouble("Shooter P Gain", SHOOT_P);
		i = m_prefs.getDouble("Shooter P Gain", SHOOT_I);
		d = m_prefs.getDouble("Shooter P Gain", SHOOT_D);
		this.getPIDController().setPID(p, i, d);		
		
		// Register with Brownout subsystem
		Brownout.getInstance().registerCallback(this);		
	}
		
	protected void initDefaultCommand() {
		this.setDefaultCommand(new ShooterReset());	// Drive Manually by default
	}
	
	public void initManualMode() {
		
		if (m_usePID) {
			m_usePID = false;
			this.disable();
			
			// Stop motor until we are ready to set speed
			m_resetBar.set(0);
			
			if (debugging)
		    	SmartDashboard.putBoolean("Shooter PID Enabled", false);
		}
	}
	
	public void driveManual(double speed) {
		
		// Drive reset bar at commanded speed,
		// being sure to check for limit switches
		m_resetBar.set(check4Endpoints(speed));	

		// Update the reset bar setpoint even while in manual mode
		// to avoid surprises when returning to PID control
		double angle = m_resetAngle.get();
		if (debugging) {
	    	SmartDashboard.putNumber("Shooter Reset Angle", angle);
		}
		m_resetBarSetpoint = angle;
	}

	public boolean initPIDMode() 
	{
		if (!m_usePID) {
			m_usePID = true;

			this.setSetpoint(m_resetBarSetpoint);
			this.enable();
		
			if (debugging)
		    	SmartDashboard.putBoolean("Shooter PID Enabled", true);
		}
		return true;
	}
	
	/*
	 * Methods to move Reset Bar to useful positions
	 */
	public void latch() {

		this.setSetpoint(m_latchPoint);
		
		// Save the position
		m_resetBarSetpoint = m_latchPoint;
		
		if (debugging)
			SmartDashboard.putNumber("Shooter Setpoint", m_resetBarSetpoint);
	}
	
	public void clear() {

		this.setSetpoint(m_clearPoint);
		
		// Save the position
		m_resetBarSetpoint = m_clearPoint;
		
		if (debugging)
			SmartDashboard.putNumber("Shooter Setpoint", m_resetBarSetpoint);
	}
	
	public boolean resetBarIsClear() {
		// If reset angle is >= m_clearPoint, or if clear limit switch is closed, return true
		return ((m_resetAngle.get() >= m_clearPoint) || m_resetBar.isFwdLimitSwitchClosed());
	}
	
	// Control the solenoid that latches the catapult
	public void cataLatch() {
		m_catLatch.set(DoubleSolenoid.Value.kForward);
	}
	
	public void cataShoot() {
		m_catLatch.set(DoubleSolenoid.Value.kReverse);		
	}

	public void cataStop() {
		m_catLatch.set(DoubleSolenoid.Value.kOff);		
	}

	// PowerConsumer
	public void callbackAlert(Brownout.PowerLevel level) {
		switch (level) {
		case Chill:
								dontShoot = true;
			break;
			
		case Critical:
								dontShoot = true;
			break;
			
		default:
							dontShoot = false;
			break;
		}
	}
	
	public boolean checkBrownOut() {
		return dontShoot;
	}
	
	// This is called during RobotInit
	public void cataCalibrate() {
		
		m_clearPoint = m_resetAngle.get();
		m_latchPoint = m_clearPoint - m_deltaPot;
		m_hasBeenCalibrated = true;
	}
	
	//Has the robot been calibrated before?
	public boolean hasBeenCalibrated() {
		return m_hasBeenCalibrated;
	}
	
	// Check Clear catapult limit switch
	public boolean checkClearLimit() {
		return m_resetBar.isFwdLimitSwitchClosed();
	}
	
	// Check Latch catapult limit switch
	public boolean checkLatchLimit() {
		return m_resetBar.isRevLimitSwitchClosed();
	}
	
	// PIDController methods
	protected double returnPIDInput() {
		double angle = m_resetAngle.get();
		if (debugging) {
	    	SmartDashboard.putBoolean("Shooter PID Enabled", true);
	    	SmartDashboard.putNumber("Shooter Reset Angle", angle);
		}
		return angle;
	}

	protected void usePIDOutput(double output) {
		if (debugging) {
			SmartDashboard.putNumber("Shooter Setpoint", this.getSetpoint());
			SmartDashboard.putNumber("Shooter Current",
					Brownout.getInstance().getCurrent(WINCH_PDP_CHANNEL));			
		}
		m_resetBar.set(check4Endpoints(output));
	}

	private double check4Endpoints(double speed) {
		SmartDashboard.putBoolean("Shooter Out of Calibration", false);
		// If trying to drive and a limit switch is hit, then stop...
		if(checkClearLimit() && speed > 0.0) {
			speed = 0.0;
			if (Math.abs(m_resetAngle.get() - m_clearPoint) > .2)
				SmartDashboard.putBoolean("Shooter Out of Calibration", true);
		}
		else if(checkLatchLimit() && speed < 0.0) {
			speed = 0.0;
			if (Math.abs(m_resetAngle.get() - m_latchPoint) > .2)
				SmartDashboard.putBoolean("Shooter Out of Calibration", true);
		}
		return(speed);
	}
}