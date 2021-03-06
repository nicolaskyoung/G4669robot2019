/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package org.usfirst.frc.team4669.robot.subsystems;

import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import org.usfirst.frc.team4669.robot.RobotMap;
import org.usfirst.frc.team4669.robot.commands.arm.ArmDefault;
import org.usfirst.frc.team4669.robot.misc.Constants;

import edu.wpi.first.wpilibj.command.Subsystem;

/**
 * Robot Arm subsystem.
 */
public class Arm extends Subsystem {
  // Put methods for controlling this subsystem
  // here. Call these from Commands.

  public static double xPos;
  public static double yPos;

  double a1 = Constants.upperArmLength;
  double a2 = Constants.forearmLength;

  private WPI_TalonSRX shoulderMotor;
  private WPI_TalonSRX elbowMotor;
  private WPI_TalonSRX wristMotor;

  @Override
  public void initDefaultCommand() {
    // Set the default command for a subsystem here.
    // setDefaultCommand(new MySpecialCommand());
    setDefaultCommand(new ArmDefault());
  }

  public Arm() {
    shoulderMotor = new WPI_TalonSRX(RobotMap.shoulderMotor);
    elbowMotor = new WPI_TalonSRX(RobotMap.elbowMotor);
    wristMotor = new WPI_TalonSRX(RobotMap.wristMotor);

    talonConfig(shoulderMotor, false);
    shoulderMotor.setSelectedSensorPosition(Constants.startShoulder);

    talonConfig(elbowMotor, false);
    elbowMotor.setSelectedSensorPosition(Constants.startElbow);

    talonConfig(wristMotor, true);
    wristMotor.setSelectedSensorPosition(Constants.startWrist);

    setCurrentLimit(elbowMotor);
    setCurrentLimit(wristMotor);
    setCurrentLimit(shoulderMotor);

    setMotorOutputs(shoulderMotor, 0, 0.8);
    setMotorOutputs(elbowMotor, 0, 0.8);
    setMotorOutputs(wristMotor, 0, 1);

    setMotionVelAccel(shoulderMotor, Constants.shoulderVel, Constants.shoulderAccel);
    setMotionVelAccel(elbowMotor, Constants.elbowVel, Constants.elbowAccel);
    setMotionVelAccel(wristMotor, Constants.wristVel, Constants.wristAccel);

    shoulderMotor.configOpenloopRamp(2);
    elbowMotor.configOpenloopRamp(0.5);

  }

  /**
   * Method to control the arm motors individually
   * 
   * @param shoulderMotorPower Controls the shoulder motor, inputs [-1,1]
   * @param elbowMotorPower    Controls the elbow motor, inputs [-1,1]
   * @param wristMotorPower    Controls the wrist motor, inputs [-1,1]
   */
  public void motorControl(TalonSRX talon, double power) {
    talon.set(ControlMode.PercentOutput,power);
  }

  public void stop() {
    shoulderMotor.set(0);
    elbowMotor.set(0);
    wristMotor.set(0);
  }

  public void setCurrentLimit(TalonSRX talon) {
    talon.configContinuousCurrentLimit(Constants.continuousCurrentLimitArm, Constants.timeout);
    talon.configPeakCurrentLimit(Constants.peakCurrentLimitArm, Constants.timeout);
    talon.configPeakCurrentDuration(Constants.currentDuration, Constants.timeout);
    talon.enableCurrentLimit(true);
  }

  public void setMotorOutputs(TalonSRX talon, double nominalOutput, double peakOutput) {
    talon.configNominalOutputForward(nominalOutput);
    talon.configNominalOutputReverse(-nominalOutput);
    talon.configPeakOutputForward(peakOutput);
    talon.configPeakOutputReverse(-peakOutput);
  }

  /**
   * Method to get the angle to provide to the arm motors to get to a target (x,y)
   * position
   * 
   * @param xGrip      Target length away from the base of the arm. Units in inches
   * @param yGrip      Target height from the ground. Units in inches
   * @param targetGrabberAngle Target wrist angle
   * @param flipUp Changes whether to flip up the elbow or not
   * @param ballMode Whether we're trying to grab a cargo or hatch panel
   * @return An array with the shoulder angle and elbow angle, null, or NaN if impossible
   */
  public double[] calculateAngles(double xGrip, double xCorrect, double yGrip, double yCorrect,
                                  double targetGrabberAngle, double angleCorrect,boolean flipUp, boolean ballMode) {
    double a3;
    //Changes the mode of the wrist depending on whether we want the hatch or the cargo 
    if(ballMode){
      a3 = Constants.handHoopLength;
    }
    else  {
      a3 = Constants.handHookLength;
    }

    //Avoids making the target height too low
    if (yGrip < 6.5){
      System.out.println("Target y too low");
      return null;
    }
    if (xGrip > Constants.robotToArmFront + 28||xGrip < -(Constants.robotToArmBack + 28)){
      System.out.println("Target x too far");
      return null;
    }

    //Corrects our x and y position based on the length of the hand
    double xWrist = xGrip - a3 * Math.cos(Math.toRadians(targetGrabberAngle));
    double yWrist = yGrip - a3 * Math.sin(Math.toRadians(targetGrabberAngle)) - Constants.shoulderHeight;

    if(ballMode){
      targetGrabberAngle+=180;
    }

    //Gets the distance from the base of the arm to the target position
    double distance = Math.sqrt(Math.pow(xWrist, 2) + Math.pow(yWrist, 2));

    //Checks if target position is feasible with the lengths of the arm
    if (distance > a1 + a2 || distance < Math.abs(a1 - a2)){
      System.out.println("Distance out of range, " + distance);
      return null;
    }
    yWrist += yCorrect;
    xWrist += xCorrect;
    distance = Math.sqrt(Math.pow(xWrist, 2) + Math.pow(yWrist, 2));

    //Now calculate the angles for each motor
    double elbowRad = Math.acos((Math.pow(distance, 2) - Math.pow(a1, 2) - Math.pow(a2, 2)) / (-2 * a1 * a2)) - Math.PI;
    if (flipUp)
      elbowRad = -elbowRad;
    double shoulderRad;
    if (flipUp)
      shoulderRad = Math.atan2(yWrist, xWrist)
          - Math.acos((Math.pow(a2, 2) - Math.pow(a1, 2) - Math.pow(xWrist, 2) - Math.pow(yWrist, 2)) / (-2 * a1 * distance));
    else
      shoulderRad = Math.atan2(yWrist, xWrist)
          + Math.acos((Math.pow(a2, 2) - Math.pow(a1, 2) - Math.pow(xWrist, 2) - Math.pow(yWrist, 2)) / (-2 * a1 * distance));
    if(shoulderRad<-Math.PI){
      shoulderRad+=Math.PI*2;
    }
    if(shoulderRad>Math.PI){
      shoulderRad-=Math.PI*2;
    }
    double elbowDeg = Math.toDegrees(elbowRad);
    double shoulderDeg = Math.toDegrees(shoulderRad);
    double wristDeg = targetGrabberAngle + angleCorrect - (shoulderDeg - 90);

    //Checks if our angles are too large, or if angles aren't possible
    if (shoulderDeg < 0 || shoulderDeg > 180 || Math.abs(elbowDeg) > 160 || Double.isNaN(shoulderDeg) || Double.isNaN(shoulderDeg)|| Double.isNaN(wristDeg)){
        System.out.println("Angles are too large or impossible."+ " Shoulder: " + shoulderDeg+ " Elbow: "+ elbowDeg);

        return null;
    }

    //Returns the angles in the form of an array
    double[] anglesArr = { shoulderDeg, elbowDeg, wristDeg };
    return anglesArr;
  }

  /**
   * Method to set the angle of individual motors on the arm
   * 
   * @param jointTalon Which joint arm motor to use
   * @param degrees    Target angle in degrees
   */
  public void setToAngle(TalonSRX jointTalon, double degrees) {
    double sprocketRatio = 0;
    if (jointTalon == shoulderMotor)
      sprocketRatio = Constants.shoulderGearRatio;
    else if (jointTalon == elbowMotor)
      sprocketRatio = Constants.elbowGearRatio;
    else if (jointTalon == wristMotor)
      sprocketRatio = Constants.wristGearRatio;
    if (sprocketRatio != 0) {
      double targetPos = degrees * Constants.encoderTicksPerRotation * sprocketRatio / 360;
      setMotorPosMagic(jointTalon, targetPos);
    }
  }

  /**
   * Method to get the current x position of the arm
   * 
   * @return x position of the arm
   */
  public double getX(boolean ballMode) {
    double a3;
    if(ballMode)
      a3 = Constants.handHoopLength;
    else 
      a3 = Constants.handHookLength;
    double shoulderAngle = getMotorAngle(getShoulderMotor());
    double elbowAngle = getMotorAngle(getElbowMotor());
    double x = a3 + a1 * Math.cos(Math.toRadians(shoulderAngle))
        + a2 * Math.cos(Math.toRadians(shoulderAngle) + Math.toRadians(elbowAngle));
    return x;
  }

  /**
   * Method to get the current y position of the arm
   * 
   * @return y position of the arm
   */
  public double getY() {
    double shoulderAngle = getMotorAngle(getShoulderMotor());
    double elbowAngle = getMotorAngle(getElbowMotor());
    double y = a1 * Math.sin(Math.toRadians(shoulderAngle))
        + a2 * Math.sin(Math.toRadians(shoulderAngle) + Math.toRadians(elbowAngle));
    return y;
  }

  /**
   * Uses Motion Magic to set motors to a target encoder position
   * 
   * @param talon     Joint motor to control
   * @param targetPos Target encoder position
   */
  public void setMotorPosMagic(TalonSRX talon, double targetPos) {
    talon.set(ControlMode.MotionMagic, targetPos);
  }

  /**
   * Uses Position Closed Loop to set motors to a target encoder position
   * 
   * @param talon     Joint motor to control
   * @param targetPos Target encoder position
   */
  public void setMotorPos(TalonSRX talon, double targetPos) {
    talon.set(ControlMode.Position, targetPos);
  }

  public void setMotionVelAccel(TalonSRX talon, int velocity, int accel) {
    talon.configMotionCruiseVelocity(velocity, Constants.timeout);
    talon.configMotionAcceleration(accel, Constants.timeout);
  }

  public void setPosition(TalonSRX talon, double position) {
    talon.set(ControlMode.MotionMagic, position);
  }

  public void talonConfig(TalonSRX talon, boolean invert) {
    talon.configFactoryDefault();
    double[] pidArr = { 0, 0, 0, 0, 0 };

    if (talon == shoulderMotor)
      pidArr = Constants.shoulderPID;
    if (talon == elbowMotor)
      pidArr = Constants.elbowPID;
    if (talon == wristMotor)
      pidArr = Constants.wristPID;
    
    talon.setNeutralMode(NeutralMode.Brake);
    talon.setInverted(invert);
    talon.setSensorPhase(true);
    talon.configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, RobotMap.pidIdx, Constants.timeout);
    talon.selectProfileSlot(RobotMap.slotIdx, RobotMap.pidIdx);
    talon.config_kF(RobotMap.slotIdx, pidArr[0], Constants.timeout);
    talon.config_kP(RobotMap.slotIdx, pidArr[1], Constants.timeout);
    talon.config_kI(RobotMap.slotIdx, pidArr[2], Constants.timeout);
    talon.config_kD(RobotMap.slotIdx, pidArr[3], Constants.timeout);
    talon.config_IntegralZone(RobotMap.slotIdx, (int) pidArr[4], Constants.timeout);
    talon.configClearPositionOnLimitF(false, Constants.timeout);
    talon.configClearPositionOnLimitR(false, Constants.timeout);
    talon.setSelectedSensorPosition(0, RobotMap.pidIdx, Constants.timeout);

  }

  public void zeroEncoders() {
    shoulderMotor.setSelectedSensorPosition(0, RobotMap.pidIdx, Constants.timeout);
    elbowMotor.setSelectedSensorPosition(0, RobotMap.pidIdx, Constants.timeout);
    wristMotor.setSelectedSensorPosition(0, RobotMap.pidIdx, Constants.timeout);
  }

  public double getEncoderPosition(TalonSRX talon) {
    return talon.getSelectedSensorPosition();
  }

  public double getEncoderVelocity(TalonSRX talon) {
    return talon.getSelectedSensorVelocity();
  }

  public double getMotorAngle(TalonSRX jointTalon) {
    double sprocketRatio = 0;
    if (jointTalon == shoulderMotor)
      sprocketRatio = Constants.shoulderGearRatio;
    else if (jointTalon == elbowMotor)
      sprocketRatio = Constants.elbowGearRatio;
    else if (jointTalon == wristMotor)
      sprocketRatio = Constants.wristGearRatio;

    return getEncoderPosition(jointTalon) / 4096 * 360 / sprocketRatio;
  }

  /**
   * @return the wristMotor
   */
  public WPI_TalonSRX getShoulderMotor() {
    return shoulderMotor;
  }

  /**
   * @return the wristMotor
   */
  public WPI_TalonSRX getElbowMotor() {
    return elbowMotor;
  }

  /**
   * @return the wristMotor
   */
  public WPI_TalonSRX getWristMotor() {
    return wristMotor;
  }

}
