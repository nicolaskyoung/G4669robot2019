/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package org.usfirst.frc.team4669.robot.subsystems;

import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.drive.MecanumDrive;

import org.usfirst.frc.team4669.robot.RobotMap;
import org.usfirst.frc.team4669.robot.commands.driveTrain.JoystickDrive;
import org.usfirst.frc.team4669.robot.misc.Constants;
import org.usfirst.frc.team4669.robot.misc.PIDOutputWrapper;
import org.usfirst.frc.team4669.robot.misc.PIDSourceWrapper;


import edu.wpi.first.wpilibj.ADXRS450_Gyro;
import edu.wpi.first.wpilibj.PIDController;
import edu.wpi.first.wpilibj.PIDSource;
import edu.wpi.first.wpilibj.Ultrasonic;
import edu.wpi.first.wpilibj.interfaces.Gyro;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.StatusFrameEnhanced;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;

/**
 * Robot drive train subsystem
 */
public class DriveTrain extends Subsystem {
  // Put methods for controlling this subsystem
  // here. Call these from Commands.
  private WPI_TalonSRX frontLeftMotor;
  private WPI_TalonSRX rearLeftMotor;
  private WPI_TalonSRX frontRightMotor;
  private WPI_TalonSRX rearRightMotor;

  private MecanumDrive drive;

  private PIDSourceWrapper visionDistance;
  private PIDSourceWrapper visionTurn;
  private PIDSourceWrapper strafeWrapper;

  private PIDController gyroPID;
  private PIDController visionTurnController;
  private PIDController visionDistanceController;
  private PIDController strafeController;

  private PIDOutputWrapper turnOutput;
  private PIDOutputWrapper visionTurnOutput;
  private PIDOutputWrapper visionDistanceOutput;
  private PIDOutputWrapper strafeOutput;

  public Ultrasonic frontUltrasonic;
  public Ultrasonic rearUltrasonic;


  private Gyro gyro;

  int velocity = 2300; // About 200 RPM, vel units are in sensor units per 100ms
  int accel = 4600;

  @Override
  public void initDefaultCommand() {
    // Set the default command for a subsystem here.
    setDefaultCommand(new JoystickDrive());
    // setDefaultCommand(new MySpecialCommand());
  }

  public DriveTrain() {
    super();
    gyro = new ADXRS450_Gyro();
    frontUltrasonic = new Ultrasonic(RobotMap.frontUltrasonicTrigger,RobotMap.frontUltrasonicEcho);
    // rearUltrasonic = new Ultrasonic(RobotMap.rearUltrasonicTrigger,RobotMap.rearUltrasonicEcho);
    frontUltrasonic.setAutomaticMode(true);
    // rearUltrasonic.setAutomaticMode(true);
    
    strafeWrapper = new PIDSourceWrapper(0);
    visionDistance = new PIDSourceWrapper(0);
    visionTurn = new PIDSourceWrapper(0);

    visionDistanceOutput = new PIDOutputWrapper();
    visionTurnOutput = new PIDOutputWrapper();
    turnOutput = new PIDOutputWrapper();
    strafeOutput = new PIDOutputWrapper();


    frontLeftMotor = new WPI_TalonSRX(RobotMap.driveFrontLeft);
    rearLeftMotor = new WPI_TalonSRX(RobotMap.driveRearLeft);
    frontRightMotor = new WPI_TalonSRX(RobotMap.driveFrontRight);
    rearRightMotor = new WPI_TalonSRX(RobotMap.driveRearRight);

    // Configures motors and inverts them
    talonConfig(frontRightMotor, false);
    talonConfig(frontLeftMotor, true);
    talonConfig(rearRightMotor, false);
    talonConfig(rearLeftMotor, true);

    // Set Current limit
    setCurrentLimit(frontRightMotor);
    setCurrentLimit(frontLeftMotor);
    setCurrentLimit(rearRightMotor);
    setCurrentLimit(rearLeftMotor);

    setMotionVelAccel(velocity, accel);

    // Configuring the Gyroscope and the PID controller for it
    gyroPID = new PIDController(Constants.gyroPID[0], Constants.gyroPID[1], Constants.gyroPID[2], (PIDSource) gyro,
        turnOutput);
    configPIDController(gyroPID, 0, 360, true, 0.5, 3);

    // Configuring the Vision PID controllers
    visionTurnController = new PIDController(Constants.cameraPID[0], Constants.cameraPID[1], Constants.cameraPID[2],
        visionTurn, visionTurnOutput);
    configPIDController(visionTurnController, Constants.pixyWidth-1, 320, false, 0.5, 10);

    visionDistanceController = new PIDController(Constants.cameraPID[0], Constants.cameraPID[1], Constants.cameraPID[2],
        visionDistance, visionDistanceOutput);
    configPIDController(visionDistanceController, 0, Constants.pixyHeight-1, false, 0.5, 10);

    strafeController = new PIDController(Constants.strafekP, 0, Constants.strafekD, strafeWrapper, strafeOutput);
    configPIDController(strafeController, 0, Constants.pixy2LineWidth, false, 0.3, 5);

    drive = new MecanumDrive(frontLeftMotor, rearLeftMotor, frontRightMotor, rearRightMotor);
    drive.setRightSideInverted(false);
    drive.setSafetyEnabled(false);
  }

  public void talonConfig(TalonSRX talon, boolean left) {
    talon.configFactoryDefault();

    talon.configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, RobotMap.pidIdx, Constants.timeout);
    talon.setInverted(!left);
    talon.setSensorPhase(true);

    talon.setNeutralMode(NeutralMode.Brake);

    talon.configOpenloopRamp(0.2,Constants.timeout);

    talon.configNominalOutputForward(0, Constants.timeout);
    talon.configNominalOutputReverse(0, Constants.timeout);
    talon.configPeakOutputForward(0.95, Constants.timeout);
    talon.configPeakOutputReverse(-0.95, Constants.timeout);

    talon.setStatusFramePeriod(StatusFrameEnhanced.Status_13_Base_PIDF0, 10, Constants.timeout);
    talon.setStatusFramePeriod(StatusFrameEnhanced.Status_10_MotionMagic, 10, Constants.timeout);

    talon.selectProfileSlot(RobotMap.slotIdx, RobotMap.pidIdx);
    talon.config_kF(RobotMap.slotIdx, Constants.driveTrainPID[0], Constants.timeout);
    talon.config_kP(RobotMap.slotIdx, Constants.driveTrainPID[1], Constants.timeout);
    talon.config_kI(RobotMap.slotIdx, Constants.driveTrainPID[2], Constants.timeout);
    talon.config_kD(RobotMap.slotIdx, Constants.driveTrainPID[3], Constants.timeout);
    talon.config_IntegralZone(RobotMap.slotIdx, (int) Constants.driveTrainPID[4], Constants.timeout);

    talon.setSelectedSensorPosition(0, RobotMap.pidIdx, Constants.timeout);
  }

  public void setCurrentLimit(TalonSRX talon) {
    talon.configContinuousCurrentLimit(Constants.continuousCurrentLimit, Constants.timeout);
    talon.configPeakCurrentLimit(Constants.peakCurrentLimit, Constants.timeout);
    talon.configPeakCurrentDuration(Constants.currentDuration, Constants.timeout);
    talon.enableCurrentLimit(true);
  }

  public void configPIDController(PIDController controller, double inputMin, double inputMax, boolean continuous,
      double outputMax, double tolerance) {
    controller.setInputRange(inputMin, inputMax);
    controller.setContinuous(continuous);
    controller.setOutputRange(-outputMax, outputMax);
    controller.setAbsoluteTolerance(tolerance);
  }

  /**
   * Cartesian drive method that specifies speeds in terms of the field
   * longitudinal and lateral directions, using the drive's angle sensor to
   * automatically determine the robot's orientation relative to the field.
   * <p>
   * Using this method, the robot will move away from the drivers when the
   * joystick is pushed forwards, and towards the drivers when it is pulled
   * towards them - regardless of what direction the robot is facing.
   * 
   * @param ySpeed       The speed that the robot should drive left and right.
   *                     [-1.0..1.0]
   * @param xSpeed       The speed that the robot should drive fowards and
   *                     backwards. [-1.0..1.0]
   * @param rotationRate The rate of rotation for the robot that is completely
   *                     independent of the translation. [-1.0..1.0]
   * @param gyroAngle    The angle of the robot relative to the field in degrees
   */
  public void fieldOrientedDrive(double ySpeed, double xSpeed, double rotationRate, double gyroAngle) {
    drive.driveCartesian(ySpeed, xSpeed, rotationRate, -gyroAngle);
  }

  /**
   * Cartesian drive method that specifies speeds in terms of the field
   * longitudinal and lateral directions, using the drive's angle sensor to
   * automatically determine the robot's orientation relative to the field.
   * <p>
   * Using this method, the robot will move away from the drivers when the
   * joystick is pushed forwards, and towards the drivers when it is pulled
   * towards them - regardless of what direction the robot is facing.
   * 
   * @param strafeSpeed       The speed that the robot should drive left and right.
   *                     [-1.0..1.0]
   * @param forwardSpeed       The speed that the robot should drive fowards and
   *                     backwards. [-1.0..1.0]
   * @param rotationRate The rate of rotation for the robot that is completely
   *                     independent of the translation. [-1.0..1.0]
   */
  public void robotOrientedDrive(double strafeSpeed, double forwardSpeed, double rotation) {
    drive.driveCartesian(strafeSpeed, forwardSpeed, rotation);
  }

  /**
   * Polar drive method that specifies speeds in terms of magnitude and direction.
   * This method does not use the drive's angle sensor.
   * 
   * @param magnitude    The speed that the robot should drive in a given
   *                     direction.
   * @param direction    The direction the robot should drive in degrees. The
   *                     direction and magnitude are independent of the rotation
   *                     rate.
   * @param rotationRate The rate of rotation for the robot that is completely
   *                     independent of the magnitude or direction. [-1.0..1.0]
   */
  public void polarDrive(double magnitude, double direction, double rotationRate) {
    drive.drivePolar(magnitude, direction, rotationRate);
  }

  public void tankDrive(double leftSpeed, double rightSpeed, boolean squareInputs) {
    if (squareInputs) {
      if (leftSpeed < 0)
        leftSpeed = -Math.pow(leftSpeed, 2);
      if (rightSpeed < 0)
        rightSpeed = -Math.pow(rightSpeed, 2);
      leftSpeed = Math.pow(leftSpeed, 2);
      rightSpeed = Math.pow(rightSpeed, 2);
    }
    driveFrontLeft(leftSpeed);
    driveRearLeft(leftSpeed);
    driveFrontRight(rightSpeed);
    driveRearRight(rightSpeed);
  }

  public void driveRearLeft(double percentage) {
    rearLeftMotor.set(percentage);
  }

  public void driveRearRight(double percentage) {
    rearRightMotor.set(percentage);
  }

  public void driveFrontLeft(double percentage) {
    frontLeftMotor.set(percentage);
  }

  public void driveFrontRight(double percentage) {
    frontRightMotor.set(percentage);
  }

  public void driveStraightGyro(double power, double targetedAngle, double kP) {
    double error = getAngle() - targetedAngle;
    double turnPower = error * kP;
    robotOrientedDrive(0, power, turnPower);
  }

  public void driveMotionMagic(double targetEncPosition) {
    setMotionVelAccel(Constants.driveVel, Constants.driveAccel);
    frontLeftMotor.set(ControlMode.MotionMagic, targetEncPosition);
    frontRightMotor.set(ControlMode.MotionMagic, targetEncPosition);
    rearLeftMotor.set(ControlMode.MotionMagic, targetEncPosition);
    rearRightMotor.set(ControlMode.MotionMagic, targetEncPosition);
  }

  public void strafeMotionMagic(double targetEncPosition) {
    setMotionVelAccel(Constants.driveVel, Constants.driveAccel);
    frontLeftMotor.set(ControlMode.MotionMagic, targetEncPosition);
    frontRightMotor.set(ControlMode.MotionMagic, -targetEncPosition);
    rearLeftMotor.set(ControlMode.MotionMagic, -targetEncPosition);
    rearRightMotor.set(ControlMode.MotionMagic, targetEncPosition);

  }

  public void calibrateGyro() {
    gyro.calibrate();
  }

  public void resetGyro() {
    gyro.reset();
  }

  public int getFrontLeftEncoder() {
    return frontLeftMotor.getSelectedSensorPosition();
  }

  public int getFrontRightEncoder() {
    return frontRightMotor.getSelectedSensorPosition();
  }

  public int getRearLeftEncoder() {
    return rearLeftMotor.getSelectedSensorPosition();
  }

  public int getRearRightEncoder() {
    return rearRightMotor.getSelectedSensorPosition();
  }

  public double getFrontLeftEncoderSpeed() {
    return frontLeftMotor.getSelectedSensorVelocity();
  }

  public double getFrontRightEncoderSpeed() {
    return frontRightMotor.getSelectedSensorVelocity();
  }

  public double getRearLeftEncoderSpeed() {
    return rearLeftMotor.getSelectedSensorVelocity();
  }

  public double getRearRightEncoderSpeed() {
    return rearRightMotor.getSelectedSensorVelocity();
  }

  public void zeroEncoders() {
    frontLeftMotor.setSelectedSensorPosition(0, RobotMap.pidIdx, Constants.timeout);
    rearLeftMotor.setSelectedSensorPosition(0, RobotMap.pidIdx, Constants.timeout);
    frontRightMotor.setSelectedSensorPosition(0, RobotMap.pidIdx, Constants.timeout);
    rearRightMotor.setSelectedSensorPosition(0, RobotMap.pidIdx, Constants.timeout);
  }

  public void stop() {
    drive.driveCartesian(0, 0, 0);
  }

  public void enableTurnPID() {
    gyroPID.reset();
    gyroPID.enable();
  }

  public void disableTurnPID() {
    gyroPID.disable();
  }

  public void setTurnAngle(double angle) {
    gyroPID.setSetpoint(angle);
  }

  public double getAngle() {
    return gyro.getAngle();
  }

  public double getAngleNormalized() {
    double angle = gyro.getAngle();
    if(angle<0){
      while(angle<0){
        angle+=360;
      }
    } else if(angle>=360){
      while(angle>=360){
        angle-=360;
      }
    }
    return angle;
  }

  public void setMotionVelAccel(int velocity, int accel) {
    frontLeftMotor.configMotionCruiseVelocity(velocity, Constants.timeout);
    frontLeftMotor.configMotionAcceleration(accel, Constants.timeout);

    frontRightMotor.configMotionCruiseVelocity(velocity, Constants.timeout);
    frontRightMotor.configMotionAcceleration(accel, Constants.timeout);

    rearLeftMotor.configMotionCruiseVelocity(velocity, Constants.timeout);
    rearLeftMotor.configMotionAcceleration(accel, Constants.timeout);

    rearRightMotor.configMotionCruiseVelocity(velocity, Constants.timeout);
    rearRightMotor.configMotionAcceleration(accel, Constants.timeout);
  }

  public void enablePIDController(PIDController controller) {
    controller.reset();
    controller.enable();
  }

  public void disablePIDController(PIDController controller) {
    controller.disable();
  }

  public void setTarget(PIDController controller, double target) {
    controller.setSetpoint(target);
  }

  public boolean getPIDDone(PIDController controller) {
    return controller.onTarget();
  }

  public double getPIDError(PIDController controller) {
    return controller.getError();
  }

  public double getTurnOutput() {
    return turnOutput.getOutput();
  }

  public double getVisionTurnOutput() {
    return visionTurnOutput.getOutput();
  }

  public double getVisionDistanceOutput() {
    return visionDistanceOutput.getOutput();
  }

  public double getStrafeOutput() {
    return strafeOutput.getOutput();
  }

  public void updateVisionTurnInput(double input){
    visionTurn.setInput(input);
  }

  public void updateVisionDriveInput(double input){
    visionDistance.setInput(input);
  }

  public void updateStrafeInput(double input){
    strafeWrapper.setInput(input);
  }

  public PIDController getGyroController() {
    return gyroPID;
  }

  public PIDController getVisionTurnController() {
    return visionTurnController;
  }

  public PIDController getVisionDistanceController() {
    return visionDistanceController;
  }

  public PIDController getStrafeController() {
    return strafeController;
  }

  public Gyro getGyro() {
    return gyro;
  }

  public double getFrontDistance() {
    return frontUltrasonic.getRangeInches();
  }

  public double getRearDistance() {
    return rearUltrasonic.getRangeInches();
  }
}
