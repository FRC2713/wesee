package org.iraiders.wesee;

import edu.wpi.first.wpilibj.networktables.NetworkTable;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.util.ArrayList;
import java.util.List;

public class WeSee {
    private static final int FRAME_WIDTH = 640;
    private static final int FRAME_HEIGHT = 480;

    private static final int MINIMUM_CONTOUR_AREA = 500;
    private static final int MINIMUM_CONTOUR_BB_WIDTH = 15;

    private static final double TARGET_WIDTH_INCHES = 10.25D;
    private static final double CAMERA_HORIZ_VIEW_ANGLE = 55.1D;

    private static final double MANUAL_EXPOSURE = 0.25D;

    private static final Scalar[] HLS_THRESHOLD = new Scalar[] {
            new Scalar(71D, 56D, 151D),
            new Scalar(90D, 103D, 214D)
    };

    private MjpegServer server;

    static {
        System.load("/usr/local/share/OpenCV/java/libopencv_java320.so");
        System.loadLibrary("v4l2jni");
    }

    private VideoCapture capture;

    public WeSee(int camId) {
        capture = new VideoCapture(camId);

        setWhitebalance(camId);
        capture.set(Videoio.CV_CAP_PROP_FRAME_WIDTH, FRAME_WIDTH);
        capture.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, FRAME_HEIGHT);
        capture.set(Videoio.CAP_PROP_AUTO_EXPOSURE, MANUAL_EXPOSURE);
        capture.set(Videoio.CAP_PROP_EXPOSURE, 0);
        capture.set(Videoio.CAP_PROP_AUTOFOCUS, 0);
        capture.set(Videoio.CAP_PROP_FOCUS, 0);
        capture.set(Videoio.CAP_PROP_SATURATION, 1);

        server = new MjpegServer(1189);
        new Thread(server).start();

        NetworkTable.setClientMode();
        NetworkTable.setTeam(2713);
        NetworkTable.setIPAddress(new String[] {"roborio-2713-frc.local", "10.27.13.103"});
        NetworkTable.initialize();
    }

    public static void main(String[] args) {
        new WeSee(0).loop();
    }

    private native void setWhitebalance(int camId);

    public void loop() {
        NetworkTable table = NetworkTable.getTable("VisionProcessing");
        while (true) {
            Thread.yield();

            /* First, read from the camera into the matrix named frame. */
            Mat frame = new Mat();

            boolean doProcess = false;
            if (table.getNumber("status", 0) != 1) {
                capture.set(Videoio.CAP_PROP_EXPOSURE, 0.072D);
                capture.set(Videoio.CAP_PROP_SATURATION, 0.5D);
            } else {
                capture.set(Videoio.CAP_PROP_EXPOSURE, 0D);
                capture.set(Videoio.CAP_PROP_SATURATION, 1);
                doProcess = true;
            }

            if (!capture.read(frame)) { // If nothing is read,
                frame.release(); // clear the memory...
                continue; // and keep trying.
            }

            MatOfByte jpgEncoded = new MatOfByte();
            Imgcodecs.imencode(".jpg", frame, jpgEncoded);
            server.pushFrame(jpgEncoded.toArray());
            jpgEncoded.release();

            if (!doProcess) {
                continue;
            }

            capture.set(Videoio.CAP_PROP_EXPOSURE, 0D);

            /* Next, convert the color space from BGR to HLS for thresholding. */
            Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2HLS);
            Core.inRange(frame, HLS_THRESHOLD[0], HLS_THRESHOLD[1], frame); // Threshold color values. Store in frame.

            /*
             * In order to combat noise and "holes" in our contour,
             * apply the closing transformation to close gaps in the
             * thresholded image.
             *
             * Blurring will also reduce noise but will create a larger contour.
             */
            //Imgproc.GaussianBlur(frame, frame, new Size(13, 13), 2D);
            Mat kernel = new Mat();
            Imgproc.morphologyEx(frame, frame, Imgproc.MORPH_CLOSE, kernel, new Point(-1, -1), 10);
            kernel.release();

            /* Find the contours. We only find external contours to combat noise. */
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(frame, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            hierarchy.release();

            /*
             * Filter the contours based off of area and bounding box width.
             *
             * This will prevent small errors being detected as the contours we are looking for.
             */
            List<Point> points = new ArrayList<>();
            for (MatOfPoint contour : contours) {
                Rect bb = Imgproc.boundingRect(contour);
                if (bb.width >= MINIMUM_CONTOUR_BB_WIDTH
                        && Imgproc.contourArea(contour) >= MINIMUM_CONTOUR_AREA) {
                    points.addAll(contour.toList());
                }
                contour.release();
            }

            if (points.isEmpty()) { // Pointless to continue if we have no useful contours.
                frame.release();
                continue;
            }

            MatOfPoint mixedContours = new MatOfPoint();
            mixedContours.fromList(points);
            Rect boundingRect = Imgproc.boundingRect(mixedContours);
            mixedContours.release();

            int center = boundingRect.x + boundingRect.width/2;
            int displacement = (FRAME_WIDTH/2 - 1) - center;
            double angle = CAMERA_HORIZ_VIEW_ANGLE * displacement/(double) FRAME_WIDTH;
            double distance =
                    (TARGET_WIDTH_INCHES * FRAME_WIDTH)/(2 * boundingRect.width * Math.tan(CAMERA_HORIZ_VIEW_ANGLE/2 * Math.PI/180));

            table.putNumber("correctionAngle", angle);
            table.putNumber("approxDistance", distance);
            table.putNumber("status", 2);

            frame.release();
        }
    }
}
