package org.usfirst.frc.team910.robot;

import com.kauailabs.navx.frc.AHRS;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.Talon;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class DriveTrain {

	AHRS navX;

	Talon lmTalon;
	Talon rmTalon;

	CANTalon LFmCANTalon; // LF
	CANTalon LBmCANTalon;
	CANTalon RFmCANTalon; // RF
	CANTalon RBmCANTalon; //

	Encoder lEncoder;
	Encoder rEncoder;

	public DriveTrain(AHRS x) {

		if (Robot.TEST) {
			LFmCANTalon = new CANTalon(2);
			LBmCANTalon = new CANTalon(3);
			RFmCANTalon = new CANTalon(0);
			RBmCANTalon = new CANTalon(1);

			lEncoder = new Encoder(IO.LEFT_DRIVE_A_ENCODER, IO.LEFT_DRIVE_B_ENCODER, false);
			rEncoder = new Encoder(IO.RIGHT_DRIVE_A_ENCODER, IO.RIGHT_DRIVE_B_ENCODER, false);
			lEncoder.setDistancePerPulse(120.0 / 2244.0);
			rEncoder.setDistancePerPulse(-120.0 / 1571.0);

		} else {
			lmTalon = new Talon(IO.LEFT_DRIVE_MOTOR);
			rmTalon = new Talon(IO.RIGHT_DRIVE_MOTOR);
			lEncoder = new Encoder(IO.LEFT_DRIVE_A_ENCODER, IO.LEFT_DRIVE_B_ENCODER, false);
			rEncoder = new Encoder(IO.RIGHT_DRIVE_A_ENCODER, IO.RIGHT_DRIVE_B_ENCODER, false);
		}

		navX = x;

	}

	public void tankDrive(double YAxisLeft, double YAxisRight) {
		if (Math.abs(YAxisLeft) > 1)
			YAxisLeft = YAxisLeft / Math.abs(YAxisLeft);
		if (Math.abs(YAxisRight) > 1)
			YAxisRight = YAxisRight / Math.abs(YAxisRight);

		if (Robot.TEST) {
			LFmCANTalon.set(-YAxisLeft * 0.5);
			LBmCANTalon.set(-YAxisLeft * 0.5);
			RFmCANTalon.set(YAxisRight * 0.5);
			RBmCANTalon.set(YAxisRight * 0.5);
		} else {
			lmTalon.set(-YAxisLeft);
			rmTalon.set(YAxisRight);
		}

	}

	// when ljoystick trigger is pressed get the intial encoder value
	// while held, contiually wheel position to intial value initiate
	// dynamicBreaking
	// if encoders change value, engage motor power to move back to held value
	// when reach held value stop
	// smart way, pid gain value to get to 0
	double startEncL, startEncR;

	public void dynamicBraking(boolean firstTime) {

		if (firstTime) {
			startEncL = lEncoder.getDistance();
			startEncR = rEncoder.getDistance();

		} else {
			// set encoder
			double lPwr = startEncL - lEncoder.getDistance();
			double rPwr = startEncR - rEncoder.getDistance();
			tankDrive(lPwr, rPwr);
		}

	}

	double intlevalue;
	double intrevalue;

	public void driveStraight(double lpower, boolean firstTime) {

		double levalue;
		double revalue;

		double currentdiff;
		double intdiff;
		double gooddiff;
		double adj;

		if (firstTime) {
			intlevalue = lEncoder.getDistance();
			intrevalue = rEncoder.getDistance();

		} else {

			intdiff = intlevalue - intrevalue;
			SmartDashboard.putNumber("init diff", intdiff);

			levalue = lEncoder.getDistance();
			revalue = rEncoder.getDistance();

			currentdiff = levalue - revalue;
			SmartDashboard.putNumber("curr diff", currentdiff);

			gooddiff = currentdiff - intdiff;
			SmartDashboard.putNumber("good diff", gooddiff);

			adj = gooddiff * .25;

			double lnew = lpower - adj;
			double rnew = lpower + adj;
			tankDrive(lnew, rnew);
		}
	}

	boolean previousDbrake = false;
	boolean previousSdrive = false;
	boolean previousCdrive = false;

	public void run(double yAxisLeft, double yAxisRight, double pov, boolean sDrive, boolean dBrake,
			boolean compassDrive, double rThrottle) {

		if (dBrake) {
			// Dynamic Braking Function//
			dynamicBraking(!previousDbrake);
			previousDbrake = true;
			previousSdrive = false;
			previousCdrive = false;
		}

		else if (sDrive) {
			// Straight Drive Function//
			driveStraight(yAxisRight, !previousSdrive);
			previousDbrake = false;
			previousSdrive = true;
			previousCdrive = false;

		} else if (pov != -5000 && navX.isConnected()) {
			// Compass Drive Function//
			compassDrive((-rThrottle + 1) / 2, navX.getYaw(), !previousCdrive, pov);
			previousCdrive = true;
			previousDbrake = false;
			previousSdrive = false;
		}

		else {
			tankDrive(yAxisLeft, yAxisRight);
			previousDbrake = false;
			previousSdrive = false;
			previousCdrive = false;
		}

		SmartDashboard.putNumber("L Encoder", lEncoder.getDistance());
		SmartDashboard.putNumber("R Encoder", rEncoder.getDistance());

	}

	public void compassDrive(double power, double currentYAW, boolean firstYAW, double targetAngle) {

		double diff;
		double adj;
		double inverse = 1;

		SmartDashboard.putNumber("targetAngle", targetAngle);

		if (targetAngle > 134 || targetAngle < -134) {
			targetAngle = targetAngle + 180;
			inverse = -1;
		} else {
			inverse = 1;
		}

		if (Math.abs(power) > 1)
			power = power / Math.abs(power);

		diff = currentYAW - targetAngle;

		SmartDashboard.putNumber("preAdjDiff", diff);

		if (Math.abs(diff) > 360) {
			if (diff > 0)
				diff = diff - 360;
			else
				diff = diff + 360;
		}
		if (diff > 180) {
			diff = -360 + diff;
		} else if (diff < -180) {
			diff = 360 + diff;
		}

		SmartDashboard.putNumber("adjustedDiff", diff);
		SmartDashboard.putNumber("power", power);
		SmartDashboard.putNumber("inverse", inverse);

		if (diff > 30) {
			tankDrive(-power, power);
		} else if (diff < -30) {
			tankDrive(power, -power);
		} else {
			adj = diff * .02;
			SmartDashboard.putNumber("adjustment", adj);

			// power = power * inverse;
			double lnew = power * inverse - adj;
			double rnew = power * inverse + adj;

			if (Math.abs(lnew) > power) {
				lnew /= Math.abs(lnew);
				rnew /= Math.abs(lnew);
				lnew *= power;
				rnew *= power;
			}

			tankDrive(lnew, rnew);

		}

	}

	public double getAngle(double y, double x) {
		return Math.atan2(y, x) * 180 / Math.PI;
	}

	public double getR(double y, double x) {
		double c;
		c = (x * x) + (y * y);
		return c;
	}
}
