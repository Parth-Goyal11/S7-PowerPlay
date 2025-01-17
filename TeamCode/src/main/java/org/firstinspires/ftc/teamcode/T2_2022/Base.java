package org.firstinspires.ftc.teamcode.T2_2022;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.bosch.JustLoggingAccelerationIntegrator;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.TouchSensor;
import com.qualcomm.robotcore.util.ElapsedTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.T2_2022.Modules.Camera.Camera;
import org.firstinspires.ftc.teamcode.T2_2022.Modules.Drive;
import org.firstinspires.ftc.teamcode.T2_2022.Modules.Grabber;
import org.firstinspires.ftc.teamcode.Utils.Angle;
import org.firstinspires.ftc.teamcode.Utils.Motor;
import org.firstinspires.ftc.teamcode.Utils.PathGenerator;
import org.firstinspires.ftc.teamcode.Utils.Point;

public abstract class Base extends LinearOpMode {
  // Sleep Times
  public ElapsedTime matchTime = new ElapsedTime();

  // Gyro and Angles
  public BNO055IMU gyro;

  public Drive dt = null;
  public Grabber grabber;
  public Camera camera;

  // Constants and Conversions
  public double targetAngle, currAngle, drive, turn, strafe, multiplier = 1;
  public double initAng = 0;
  public String driveType;
  public String armStat;

  // Positions and Bounds
  public double dpadTurnSpeed = 0.175, dpadDriveSpeed = 0.6;

  // Button Variables
  public boolean yP = false, yLP = false;
  public boolean aP2 = false, aLP2 = false;
  public boolean rP2 = false, rLP2 = false;
  public boolean rP = false, rLP = false;
  public boolean lP2 = false, lLP2 = false;
  public boolean yP2 = false, yLP2 = false;
  public boolean rSP2 = false, rSLP2 = false;
  public boolean bP2 = false, bLP2 = false;
  public boolean dpU2 = false, dpUL2 = false;
  public boolean dpD2 = false, dpDL2 = false;
  public boolean dpL2 = false, dpLL2 = false;
  public boolean dpR2 = false, dpRL2 = false;
  public boolean sPL2 = false, spLL2 = false;
  public boolean lb2 = false, lbl2 = false;
  public boolean slowDrive = false, fastDrive = false;
  public boolean basicDrive = false;

  public void initHardware(int angle, OpMode m) throws InterruptedException {
    initHardware(0, 0, angle, m);

    initAng = angle;
  }

  public void initHardware(int xPos, int yPos, int angle, OpMode m) throws InterruptedException {
    // Hubs
    List<LynxModule> allHubs;
    allHubs = hardwareMap.getAll(LynxModule.class);

    for (LynxModule hub : allHubs) {
      hub.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);
    }

    // Motors
    Motor fLeftMotor = new Motor(hardwareMap, "front_left_motor");
    Motor bLeftMotor = new Motor(hardwareMap, "back_left_motor");
    Motor fRightMotor = new Motor(hardwareMap, "front_right_motor");
    Motor bRightMotor = new Motor(hardwareMap, "back_right_motor");

    Motor ls = new Motor(hardwareMap, "leftSlide"),
        rs = new Motor(hardwareMap, "rightSlide"),
        v = new Motor(hardwareMap, "v4b");

    // Reverse the right side motors
    // Reverse left motors if you are using NeveRests
    fRightMotor.setDirection(DcMotorSimple.Direction.REVERSE);
    bRightMotor.setDirection(DcMotorSimple.Direction.REVERSE);

    // Servo
    Servo s = hardwareMap.get(Servo.class, "claw");

    // Gyro
    BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
    parameters.angleUnit = BNO055IMU.AngleUnit.DEGREES;
    parameters.accelUnit = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
    parameters.calibrationDataFile =
        "BNO055IMUCalibration.json"; // see the calibration sample opmode
    parameters.loggingEnabled = true;
    parameters.loggingTag = "IMU";
    parameters.accelerationIntegrationAlgorithm = new JustLoggingAccelerationIntegrator();

    camera = new Camera(hardwareMap);
    camera.switchToAprilTagDetection();

    gyro = hardwareMap.get(BNO055IMU.class, "imu");
    gyro.initialize(parameters);

    // Sensors
    TouchSensor t = hardwareMap.get(TouchSensor.class, "touch_sensor");

    // Modules
    dt =
        new Drive(
            fLeftMotor, bLeftMotor, fRightMotor, bRightMotor, gyro, m, xPos, yPos, angle, allHubs);

    grabber = new Grabber(ls, rs, v, s, t);

    // reset constants
    targetAngle = currAngle = drive = turn = strafe = multiplier = 1;
    dpadTurnSpeed = 0.175;
    dpadDriveSpeed = 0.2;
    initAng = angle;
    yP = false;
    yLP = false;
    aP2 = false;
    aLP2 = false;
    rP2 = false;
    rLP2 = false;
    lP2 = false;
    lLP2 = false;
    yP2 = false;
    yLP2 = false;
    rSP2 = false;
    rSLP2 = false;
    rP = false;
    rLP = false;
    bP2 = false;
    bLP2 = false;
    dpU2 = false;
    dpUL2 = false;
    dpD2 = false;
    dpDL2 = false;
    dpL2 = false;
    dpLL2 = false;
    dpR2 = false;
    dpRL2 = false;
    slowDrive = false;
    fastDrive = false;
    basicDrive = false;
    sPL2 = false;
    spLL2 = false;
    armStat = "rest";
  }

  public void initHardware(OpMode m) throws InterruptedException {
    initHardware(0, m);
  }

  // Autonomous Movement (Note that you do not have to insert the current position into any of the
  // weighpoints)
  public void SplinePathConstantHeading(
      ArrayList<Point> pts,
      double heading,
      double driveSpeedCap,
      double xError,
      double yError,
      double angleError,
      int lookAheadDist,
      double timeout) {
    Point curLoc = dt.getCurrentPosition();
    ArrayList<Point> wps = PathGenerator.interpSplinePath(pts, curLoc);
    dt.traversePath(
        wps, heading, driveSpeedCap, false, -1, xError, yError, angleError, lookAheadDist, timeout);
  }

  public void SplinePathConstantHeading(
      ArrayList<Point> pts,
      double heading,
      double driveSpeedCap,
      double powLb,
      double xError,
      double yError,
      double angleError,
      int lookAheadDist,
      double timeout) {
    Point curLoc = dt.getCurrentPosition();
    ArrayList<Point> wps = PathGenerator.interpSplinePath(pts, curLoc);
    dt.traversePath(
        wps,
        heading,
        driveSpeedCap,
        true,
        powLb,
        xError,
        yError,
        angleError,
        lookAheadDist,
        timeout);
  }

  public void LinearPathConstantHeading(
      ArrayList<Point> pts,
      double heading,
      double driveSpeedCap,
      double powLb,
      double xError,
      double yError,
      double angleError,
      int lookAheadDist,
      double timeout) {
    Point curLoc = dt.getCurrentPosition();
    ArrayList<Point> wps = new ArrayList<>();
    wps.add(curLoc);
    wps.addAll(pts);
    wps = PathGenerator.generateLinearSpline(wps);
    dt.traversePath(
        wps, heading, driveSpeedCap, powLb, xError, yError, angleError, lookAheadDist, timeout);
  }

  public void turnTo(double targetAngle, long timeout, double powerCap, double minDifference) {
    dt.turnTo(targetAngle, timeout, powerCap, minDifference);
  }

  public void turnTo(double targetAngle, long timeout, double powerCap) {
    dt.turnTo(targetAngle, timeout, powerCap, 2);
  }

  public void turnTo(double targetAngle, long timeout) {
    turnTo(targetAngle, timeout, 0.7);
  }

  public void moveToPosition(
      double targetXPos,
      double targetYPos,
      double targetAngle,
      double posAccuracy,
      double angleAccuracy,
      double timeout,
      double powerLb) {
    dt.moveToPosition(
        targetXPos,
        targetYPos,
        targetAngle,
        posAccuracy,
        posAccuracy,
        angleAccuracy,
        timeout,
        powerLb);
  }

  public void moveToPosition(
      double targetXPos, double targetYPos, double targetAngle, double timeout) {
    moveToPosition(targetXPos, targetYPos, targetAngle, 2, 2, timeout, 0.1);
  }

  public void moveToPosition(
      double targetXPos, double targetYPos, double targetAngle, double timeout, double powerLb) {
    moveToPosition(targetXPos, targetYPos, targetAngle, 2, 2, timeout, powerLb);
  }

  public void moveToPosition(
      double targetXPos,
      double targetYPos,
      double targetAngle,
      double posAccuracy,
      double timeout,
      double powerLb) {
    moveToPosition(targetXPos, targetYPos, targetAngle, posAccuracy, 2, timeout, powerLb);
  }

  // Function implementing Points
  public void moveToPosition(
      Point p, double xAccuracy, double yAccuracy, double angleAccuracy, double timeout) {
    moveToPosition(p.xP, p.yP, p.ang, xAccuracy, yAccuracy, angleAccuracy, timeout);
  }

  public void moveToPosition(Point p, double posAccuracy, double angleAccuracy, double timeout) {
    moveToPosition(p.xP, p.yP, p.ang, posAccuracy, posAccuracy, angleAccuracy, timeout);
  }

  public void moveToPosition(Point p, double timeout) {
    moveToPosition(p.xP, p.yP, p.ang, 2, 2, timeout);
  }

  public void moveToPosition(Point p, double posAccuracy, double timeout) {
    moveToPosition(p.xP, p.yP, p.ang, posAccuracy, 2, timeout);
  }

  // Driver Controlled Movemement
  public void computeDrivePowers(Gamepad gamepad) {
    currAngle = 0;
    if (basicDrive) {
      driveType = "Robot Centric";

      if (gamepad.dpad_right) {
        dt.driveRobotCentric(0, dpadTurnSpeed, 0);
      } else if (gamepad.dpad_left) {
        dt.driveRobotCentric(0, -dpadTurnSpeed, 0);
      } else if (gamepad.dpad_up) {
        dt.driveFieldCentric(0, 0.01 * Angle.angleDifference(currAngle, targetAngle + 90), 0);
      } else {
        dt.driveRobotCentric(drive, turn, strafe);
      }
    } else {
      driveType = "Field Centric";

      if (gamepad.dpad_right) {
        dt.driveFieldCentric(dpadDriveSpeed, 0, 0);
      } else if (gamepad.dpad_left) {
        dt.driveFieldCentric(-dpadDriveSpeed, 0, 0);
      } else if (gamepad.dpad_up) {
        dt.driveFieldCentric(0, 0, dpadDriveSpeed);
      } else if (gamepad.dpad_down) {
        dt.driveFieldCentric(0, 0, -dpadDriveSpeed);
      } else {
        dt.driveFieldCentric(drive, turn, strafe);
      }
    }
  }

  // Misc Utility Functions
  public String formatDegrees(double degrees) {
    return String.format(Locale.getDefault(), "%.1f", AngleUnit.DEGREES.normalize(degrees));
  }

  public double floor(double rawInput) {
    if (slowDrive) {
      return ((int) (rawInput * 5.5)) / 11.0;
    } else if (fastDrive) {
      return rawInput;
    }
    return ((int) (rawInput * 5.5)) / 11.0; // at slow
  }

  public double turnFloor(double rawInput) {
    return ((int) (rawInput * 15)) / 20.0;
  }

  public String formatAngle(AngleUnit angleUnit, double angle) {
    return formatDegrees(AngleUnit.DEGREES.fromUnit(angleUnit, angle));
  }

  // Other Functions
  public double normalizeThreeDigits(double d) {
    return (int) (d * 1000) / 1000.;
  }

  @Override
  public abstract void runOpMode() throws InterruptedException;
}
