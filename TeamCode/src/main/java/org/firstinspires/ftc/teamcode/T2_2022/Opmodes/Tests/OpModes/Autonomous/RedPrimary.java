package org.firstinspires.ftc.teamcode.T2_2022.Opmodes.Tests.OpModes.Autonomous;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.util.ElapsedTime;
import org.firstinspires.ftc.teamcode.T2_2022.Base;
import org.firstinspires.ftc.teamcode.Utils.Point;

import java.util.ArrayList;
import java.util.Arrays;

@Autonomous(name = "Red_Primary", group = "OdomBot")
public class RedPrimary extends Base {
  @Override
  public void runOpMode() throws InterruptedException {
    ElapsedTime timer = new ElapsedTime();
    initHardware(0, this);
    sleep(500);
    int location = 3;
    telemetry.addData("Status", "Initialized");
    telemetry.update();
    sleep(500);
    while(!isStarted() && !isStopRequested()){
      camera.getLatestDetections();
      location = camera.getDetection();
      telemetry.addData("Status: ", "Initialized");
      telemetry.addData("Pos: ", location);
      telemetry.update();
    }


    waitForStart();
    telemetry.addData("Pos:", location);
    telemetry.update();
    matchTime.reset();
    dt.resetCache();
    grabber.grabCone();
    sleep(500);


    ArrayList<Point> path1 = new ArrayList<>();

    path1.addAll(new ArrayList<>(
            Arrays.asList(
                    new Point(14, -7), new Point(35, -12), new Point(57, 10)
            )
    ));
    sleep(500);
    LinearPathConstantHeading(path1, 0, 0.7, 1, 1, 1, 1, 8, 100000);


    /*timer.reset();
    while (timer.milliseconds() <= 525) {
      dt.driveFieldCentric(0.15, 0, 0, 1);
    }

    timer.reset();
    while (timer.milliseconds() <= 1200) {
      dt.driveFieldCentric(0, 0, 0.3, 1);
    }
    dt.stopDrive();
    sleep(300);
    turnTo(0, 2000);
    sleep(300);

    timer.reset();
    while (timer.milliseconds() <= 1100) {
      dt.driveFieldCentric(0.2, 0, 0, 1);
    }
    dt.stopDrive();
    // sleep(900);

    // Raise slide and drop
    grabber.raiseMiddleAuto();
    sleep(2000);

    timer.reset();
    while (timer.milliseconds() <= 850) {
      dt.driveFieldCentric(0, 0, 0.1, 1);
    }
    dt.stopDrive();

    grabber.releaseCone();
    sleep(500);
    grabber.grabCone();

    timer.reset();
    while (timer.milliseconds() <= 900) {
      dt.driveFieldCentric(0, 0, -0.1, 1);
    }
    dt.stopDrive();

    grabber.restArm();
    sleep(1000);

    // park
    timer.reset();
    if (location == 1) {
      while (timer.milliseconds() <= 3000) {
        dt.driveFieldCentric(-0.2, 0, 0, 1);
      }
    } else if (location == 2) {
      while (timer.milliseconds() <= 1000) {
        dt.driveFieldCentric(-0.2, 0, 0, 1);
      }
    } else if(location == 3){
      while (timer.milliseconds() <= 1000) {
        dt.driveFieldCentric(0.2, 0, 0, 1);
      }
    }
    dt.stopDrive();*/
  }
}
