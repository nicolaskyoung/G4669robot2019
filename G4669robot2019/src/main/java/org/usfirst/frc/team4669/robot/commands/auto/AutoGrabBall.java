// /*----------------------------------------------------------------------------*/
// /* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
// /* Open Source Software - may be modified and shared by FRC teams. The code   */
// /* must be accompanied by the FIRST BSD license file in the root directory of */
// /* the project.                                                               */
// /*----------------------------------------------------------------------------*/

// package org.usfirst.frc.team4669.robot.commands.auto;

// import org.usfirst.frc.team4669.robot.commands.arm.ArmToPosition;
// // import org.usfirst.frc.team4669.robot.commands.arm.RetractArm;
// import org.usfirst.frc.team4669.robot.commands.driveTrain.DriveForwardMotionMagic;
// import org.usfirst.frc.team4669.robot.misc.Constants;

// import edu.wpi.first.wpilibj.command.CommandGroup;

// public class AutoGrabBall extends CommandGroup {
//   /**
//    * Add your docs here.
//    */
//   public AutoGrabBall() {
//     // Add Commands here:
//     // e.g. addSequential(new Command1());
//     // addSequential(new Command2());
//     // these will run in order.

//     // To run multiple commands at the same time,
//     // use addParallel()
//     // e.g. addParallel(new Command1());
//     // addSequential(new Command2());
//     // Command1 and Command2 will run in parallel.

//     // A command group will require all of the subsystems that each member
//     // would require.
//     // e.g. if Command1 requires chassis, and Command2 requires arm,
//     // a CommandGroup containing them would require both the chassis and the
//     // arm.

//     addSequential(new BallAlignment());
//     // addParallel(new OpenGrabber());
//     addSequential(new ArmToPosition(Constants.armGrabBallX, Constants.armGrabBallY, 0, false, true));
//     addSequential(new DriveForwardMotionMagic(2));
//     // addSequential(new CloseGrabber());
//     // addSequential(new RetractArm());
//   }
// }
