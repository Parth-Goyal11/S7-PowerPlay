package org.firstinspires.ftc.teamcode.T2_2022.Modules;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;
import java.util.ArrayList;
import java.util.List;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.teamcode.T2_2022.Base;
import org.firstinspires.ftc.teamcode.Utils.Angle;
import org.firstinspires.ftc.teamcode.Utils.Motor;
import org.firstinspires.ftc.teamcode.Utils.PathGenerator;
import org.firstinspires.ftc.teamcode.Utils.Point;

public class Drive extends Base {

  protected Motor fLeftMotor, bLeftMotor, fRightMotor, bRightMotor;
  protected BNO055IMU gyro;
  protected OpMode opMode;
  protected List<LynxModule> allHubs;
  ElapsedTime time = new ElapsedTime();
  double prevTime = 0;
  public double yEncPos = 0, xEncPosv = 0;

  public Drive(
      Motor fLeftMotor,
      Motor bLeftMotor,
      Motor fRightMotor,
      Motor bRightMotor,
      BNO055IMU gyro,
      OpMode m,
      int xPos,
      int yPos,
      int angle,
      List<LynxModule> allHubs) {

    this.fLeftMotor = fLeftMotor;
    this.fRightMotor = fRightMotor;
    this.bLeftMotor = bLeftMotor;
    this.bRightMotor = bRightMotor;
    this.gyro = gyro;
    this.opMode = m;
    this.allHubs = allHubs;
  }
  // Localization
  public void updatePosition() {
    // apply mecanum kinematic model
    // (https://research.ijcaonline.org/volume113/number3/pxc3901586.pdf)
    double xV =
        (fLeftMotor.retMotorEx().getVelocity()
                + fRightMotor.retMotorEx().getVelocity()
                + bLeftMotor.retMotorEx().getVelocity()
                + bRightMotor.retMotorEx().getVelocity())
            * 0.5;
    double yV =
        (-fLeftMotor.retMotorEx().getVelocity()
                + fRightMotor.retMotorEx().getVelocity()
                + bLeftMotor.retMotorEx().getVelocity()
                - bRightMotor.retMotorEx().getVelocity())
            * 0.5;

    // rotate the vector
    double nx =
        (xV * Math.cos(Math.toRadians(getAngle()))) - (yV * Math.sin(Math.toRadians(getAngle())));
    double nY =
        (xV * Math.sin(Math.toRadians(getAngle()))) + (yV * Math.cos(Math.toRadians(getAngle())));
    xV = nx;
    yV = nY;

    // integrate velocity over time
    yEncPos += (yV * (time.seconds() - prevTime)) / 43.708; // <-- Tick to inch conversion factor
    xEncPosv += (xV * (time.seconds() - prevTime)) / 43.708;
    prevTime = time.seconds();
  }

  public double getX() {
    return xEncPosv;
  }

  public double getY() {
    return yEncPos;
  }

  public Point getCurrentPosition() {
    updatePosition();
    return new Point(getX(), getY(), getAngle());
  }

  public double getAngle() {
    Orientation angles =
        gyro.getAngularOrientation(
            AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES); // ZYX is Original
    return Angle.normalize(angles.firstAngle + initAng);
  }

  public double getRobotDistanceFromPoint(Point p2) {
    return Math.sqrt((p2.yP - getY()) * (p2.yP - getY()) + (p2.xP - getX()) * (p2.xP - getX()));
  }

  public double getFleftVelo() {
    return fLeftMotor.retMotorEx().getVelocity();
  }

  public double getFLeftTicks(){
    return fLeftMotor.retMotorEx().getCurrentPosition();
  }

  // Path Traversal

  // Kinda Like:
  // https://www.ri.cmu.edu/pub_files/pub3/coulter_r_craig_1992_1/coulter_r_craig_1992_1.pdf
  // Helpful explanation:
  // https://www.chiefdelphi.com/t/paper-implementation-of-the-adaptive-pure-pursuit-controller/166552
  public void traversePath(
      ArrayList<Point> wp,
      double heading,
      double driveSpeedCap,
      boolean limitPower,
      double powerLowerBound,
      double xError,
      double yError,
      double angleError,
      int lookAheadDist,
      double timeout) {
    ElapsedTime time = new ElapsedTime();
    int lastLhInd = 0;
    time.reset();
    while ((lastLhInd < wp.size() - 1
            || (Math.abs(getX() - wp.get(wp.size() - 1).xP) > xError
                || Math.abs(getY() - wp.get(wp.size() - 1).yP) > yError
                || Math.abs(heading - getAngle()) > angleError))
        && time.milliseconds() < timeout) {
      resetCache();
      updatePosition();
      double x = getX();
      double y = getY();
      double theta = getAngle();

      // find point which fits the look ahead criteria
      Point nxtP = null;
      int i = 0, cnt = 0, possInd = -1;
      double maxDist = -1;
      for (Point p : wp) {
        double ptDist = getRobotDistanceFromPoint(p);
        if (Math.abs(ptDist) <= lookAheadDist
            && i > lastLhInd
            && Math.abs(ptDist) > maxDist
            && Math.abs(i - lastLhInd) < 5) {
          nxtP = p;
          possInd = i;
          maxDist = Math.abs(ptDist);
        }
        i++;
      }

      if (possInd == -1) {
        possInd = lastLhInd;
        nxtP = wp.get(lastLhInd);
      }
      if (nxtP == null) {
        stop();
        break;
      }

      // assign powers to follow the look-ahead point
      double xDiff = nxtP.xP - x;
      double yDiff = nxtP.yP - y;
      double angDiff, splineAngle;

      splineAngle = Math.atan2(yDiff, xDiff);
      if (heading == Double.MAX_VALUE) {
        angDiff = theta - Angle.normalize(Math.toDegrees(splineAngle));
      } else {
        angDiff = theta - heading;
      }

      if (Math.abs(angDiff) < angleError) angDiff = 0;

      double dist = getRobotDistanceFromPoint(nxtP); // mtp 2.0
      double relAngToP =
          Angle.normalizeRadians(
              splineAngle - (Math.toRadians(theta) - Math.toRadians(90))); // mtp 2.0
      double relX = Math.sin(relAngToP) * dist, relY = Math.cos(relAngToP) * dist;
      double xPow = (relX / (Math.abs(relY) + Math.abs(relX))) * driveSpeedCap,
          yPow = (relY / (Math.abs(relX) + Math.abs(relY))) * driveSpeedCap;

      if (limitPower) {
        if (Math.abs(yDiff) > 5) { // raise if too unstable
          if (yPow < 0) {
            yPow = Math.min(-powerLowerBound, yPow);
          } else {
            yPow = Math.max(powerLowerBound, yPow);
          }
        }
        if (Math.abs(xDiff) > 5) {
          if (xPow < 0) {
            xPow = Math.min(-powerLowerBound, xPow);
          } else {
            xPow = Math.max(powerLowerBound, xPow);
          }
        }
      }
      System.out.println(xPow + " " + yPow);
      driveFieldCentric(yPow, 0.05 * angDiff, xPow);
      lastLhInd = possInd;
    }
    stopDrive();
  }

  public void traversePath(
      ArrayList<Point> wp,
      double heading,
      double driveSpeedCap,
      double powLb,
      double xError,
      double yError,
      double angleError,
      int lookAheadDist,
      double timeout) {
    traversePath(
        wp,
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

  public void traversePath(
      ArrayList<Point> wp,
      double heading,
      double xError,
      double yError,
      double angleError,
      int lookAheadDist,
      double timeout) {
    traversePath(wp, heading, 1, false, -1, xError, yError, angleError, lookAheadDist, timeout);
  }

  public void moveToPosition(
      double targetXPos,
      double targetYPos,
      double targetAngle,
      double xAccuracy,
      double yAccuracy,
      double angleAccuracy,
      double timeout,
      double powerlB) {
    ArrayList<Point> pt = new ArrayList<>();
    pt.add(getCurrentPosition());
    pt.add(new Point(targetXPos, targetYPos));
    ArrayList<Point> wps = PathGenerator.generateLinearSpline(pt);
    traversePath(wps, targetAngle, 1, powerlB, xAccuracy, yAccuracy, angleAccuracy, 10, timeout);
    stopDrive();
  }

  public void turnTo(double targetAngle, long timeout, double powerCap, double minDifference) {
    // GM0
    double currAngle = getAngle();
    ElapsedTime time = new ElapsedTime();
    while (Math.abs(currAngle - targetAngle) > minDifference
        && time.milliseconds() < timeout
        && ((LinearOpMode) opMode).opModeIsActive()) {
      resetCache();
      updatePosition();
      currAngle = getAngle();
      double angleDiff = Angle.normalize(currAngle - targetAngle);
      double calcP = Range.clip(angleDiff * 0.01, -powerCap, powerCap);
      setDrivePowers(calcP, calcP, calcP, calcP);
    }
    stopDrive();
  }

  // Driving
  public void driveFieldCentric(double drive, double turn, double strafe) {
    // https://gm0.org/en/latest/docs/software/tutorials/mecanum-drive.html#field-centric
    double fRightPow, bRightPow, fLeftPow, bLeftPow;
    double botHeading = -Math.toRadians(gyro.getAngularOrientation().firstAngle);

    System.out.println(drive + " " + turn + " " + strafe);

    double rotX = drive * Math.cos(botHeading) - strafe * Math.sin(botHeading);
    double rotY = drive * Math.sin(botHeading) + strafe * Math.cos(botHeading);

    double denominator = Math.max(Math.abs(strafe) + Math.abs(drive) + Math.abs(turn), 1);
    fLeftPow = (rotY + rotX + turn) / denominator;
    bLeftPow = (rotY - rotX + turn) / denominator;
    fRightPow = (rotY - rotX - turn) / denominator;
    bRightPow = (rotY + rotX - turn) / denominator;

    setDrivePowers(bLeftPow, fLeftPow, bRightPow, fRightPow);
  }

  public void driveFieldCentric(double drive, double angle, double strafe, double diff) {
    // https://gm0.org/en/latest/docs/software/tutorials/mecanum-drive.html#field-centric
    double fRightPow, bRightPow, fLeftPow, bLeftPow;
    double botHeading = -Math.toRadians(gyro.getAngularOrientation().firstAngle);

    double currAngle = getAngle();
    double angleDiff = Angle.normalize(currAngle - angle);
    double calcP = Range.clip(angleDiff * 0.01, -1, 1);
    double turn = calcP;

    double rotX = drive * Math.cos(botHeading) - strafe * Math.sin(botHeading);
    double rotY = drive * Math.sin(botHeading) + strafe * Math.cos(botHeading);

    double denominator = Math.max(Math.abs(strafe) + Math.abs(drive) + Math.abs(turn), 1);
    fLeftPow = (rotY + rotX + turn) / denominator;
    bLeftPow = (rotY - rotX + turn) / denominator;
    fRightPow = (rotY - rotX - turn) / denominator;
    bRightPow = (rotY + rotX - turn) / denominator;

    setDrivePowers(bLeftPow, fLeftPow, bRightPow, fRightPow);
  }

  public void driveRobotCentric(double drive, double turn, double strafe) {
    // https://gm0.org/en/latest/docs/software/tutorials/mecanum-drive.html#robot-centric-final-sample-code

    double fRightPow = 0, bRightPow = 0, fLeftPow = 0, bLeftPow = 0;

    fLeftPow = -drive + turn - strafe;
    bLeftPow = -drive + turn + strafe;
    fRightPow = drive + turn - strafe;
    bRightPow = drive + turn + strafe;

    double[] calculatedPower = scalePowers(bLeftPow, fLeftPow, bRightPow, fRightPow);
    fLeftPow = calculatedPower[0];
    bLeftPow = calculatedPower[1];
    fRightPow = calculatedPower[2];
    bRightPow = calculatedPower[3];

    setDrivePowers(bLeftPow, fLeftPow, bRightPow, fRightPow);
  }

  public void setDrivePowers(double bLeftPow, double fLeftPow, double bRightPow, double fRightPow) {
    bLeftMotor.setPower(bLeftPow);
    fLeftMotor.setPower(fLeftPow);
    bRightMotor.setPower(bRightPow);
    fRightMotor.setPower(fRightPow);
  }

  public void stopDrive() {
    setDrivePowers(0, 0, 0, 0);
  }

  public double[] scalePowers(
      double bLeftPow, double fLeftPow, double bRightPow, double fRightPow) {
    // Normalizes powers such that they are all all less than or equal to 1
    double maxPow =
        Math.max(
            Math.max(Math.abs(fLeftPow), Math.abs(bLeftPow)),
            Math.max(Math.abs(fRightPow), Math.abs(bRightPow)));
    if (maxPow > 1) {
      fLeftPow /= maxPow;
      bLeftPow /= maxPow;
      fRightPow /= maxPow;
      bRightPow /= maxPow;
    }

    return new double[] {fLeftPow, bLeftPow, fRightPow, bRightPow};
  }

  // Misc. Functions
  public void resetCache() {
    // Clears cache of all hubs
    for (LynxModule hub : allHubs) {
      hub.clearBulkCache();
    }
  }

  @Override
  public void runOpMode() throws InterruptedException {}
}
