package com.binge;

import com.binge.LaserObstacle;
import com.binge.LaserObstacle.LaserOrientation;
import com.binge.SpinningLaserObstacle;
import com.binge.TrackingLaserObstacle;
import com.binge.HomingMissileLauncherObstacle;
import java.io.*;
import java.util.Random;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import static com.binge.Main.*;

public class PageLoader {

    public static void loadMainPage() {
        pane.getChildren().clear();

        Text title = new Text("Ball");
        title.setLayoutX(300);
        title.setLayoutY(300);
        title.setFont(new Font(80));
        pane.getChildren().add(title);

        Button selectLevels = new Button("Play");
        selectLevels.setLayoutX(400);
        selectLevels.setLayoutY(400);
        pane.getChildren().add(selectLevels);

        selectLevels.setOnAction(event1 -> {
            pane.getChildren().removeAll();
            loadSelectStage();
        });

        Button customLevel = new Button("Custom level");
        customLevel.setLayoutX(380);
        customLevel.setLayoutY(440);
        pane.getChildren().add(customLevel);
        customLevel.setOnAction(event2 -> {
            loadCustomStage();
        });

        if (Main.scene==null) Main.scene = new Scene(pane, 1200, 800);
        else Main.scene.setRoot(pane);
    }

    public static void loadSelectStage() {
        pane.getChildren().clear();

        Button mainPage = new Button("main page");
        mainPage.setLayoutX(40);
        mainPage.setLayoutY(40);
        pane.getChildren().add(mainPage);

        mainPage.setOnAction(actionEvent -> {
            loadMainPage();
        });

        Button stageBtn = new Button("Stage 1");
        stageBtn.setLayoutX(450);
        stageBtn.setLayoutY(400);
        pane.getChildren().add(stageBtn);

        stageBtn.setOnAction(e -> {
            character.inGame = true;
            character.levelNum = 1;
            character.sublevelNum = 1;
            loadStage(1);
        });

        Main.scene.setRoot(pane);
    }

    public static void loadStage(int n) {
        Level level = new Level(n);
        String path = "src/com/binge/Stages/stage" + n + "/";
        File dir = new File(path);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                Sublevel sublevel = loadStageFromFile(path + child.getName(), level.levelLength+1);
                level.sublevels.add(sublevel);
                level.checkpoints.add(sublevel.checkpoint);
                if (sublevel.checkpoint != null) sublevel.checkpoint.substageNum = level.sublevels.size();
                level.levelLength += 1;
            }
        }
        character.levelNum = n;
        character.sublevelNum = 1;
        pane = level.sublevels.getFirst().pane;
        pane.getChildren().add(canvas);
        Main.scene.setRoot(pane);

        Main.currentLevel = level;
        Main.currentSublevel = level.sublevels.getFirst();
    }

    public static Sublevel loadStageFromFile(String filename, int n) {
        Sublevel sublevel = new Sublevel(n);

//        Main.obstacles.clear();
//        Main.items.clear();
//        Main.displacers.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
//            pane.getChildren().clear();
//
//            stage.getChildren().add(Main.canvas);

            String line;
            String section = "";
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("stage")) {
                    continue;
                } else if (line.equals("initial position") ||line.equals("CircleObstacle") ||
                        line.equals("RectangleObstacle") || line.equals("Coin") ||
                        line.equals("SizeShifter") || line.equals("GrapplePoint") || line.equals("Checkpoint") ||
                        line.equals("CircleTrap") || line.equals("Goal") || line.equals("Lock") || line.equals("LaserObstacle") || line.equals("VerticalLaserObstacle") || line.equals("SpinningLaserObstacle") || line.equals("TrackingLaserObstacle") || line.equals("HomingMissileLauncherObstacle")) {
                    section = line;
                } else {
                    String[] tokens = line.split("\\s+");
                    switch (section) {
                        case "initial position":
                            if (tokens.length >= 2) {
                                double x = Double.parseDouble(tokens[0]);
                                double y = Double.parseDouble(tokens[1]);
//                                character = new Character(x, y, 20, Color.rgb(255,241,204));
                                character.pos.setX(x);
                                character.pos.setY(y);
                                sublevel.pane.getChildren().add(character.body);
                            }
                            break;
                        case "CircleObstacle":
                            if (tokens.length >= 3) {
                                double x = Double.parseDouble(tokens[0]);
                                double y = Double.parseDouble(tokens[1]);
                                int radius = Integer.parseInt(tokens[2]);
                                boolean fatal = false;
                                if (tokens.length >= 4) {
                                    fatal = Boolean.parseBoolean(tokens[3]);
                                }
                                CircleObstacle co = new CircleObstacle(sublevel.pane, x, y, radius, Color.GRAY, fatal);
                                sublevel.obstacles.add(co);
                            }

                            break;
                        case "RectangleObstacle":
                            if (tokens.length >= 5) {
                                double cx = Double.parseDouble(tokens[0]);
                                double cy = Double.parseDouble(tokens[1]);
                                double width = Double.parseDouble(tokens[2]);
                                double height = Double.parseDouble(tokens[3]);
                                double angle = Double.parseDouble(tokens[4]);
                                boolean fatal = false;
                                if (tokens.length >= 6) {
                                    fatal = Boolean.parseBoolean(tokens[5]);
                                }
                                RectangleObstacle ro = new RectangleObstacle(sublevel.pane, cx, cy, width, height, angle, Color.GRAY, fatal);
                                sublevel.obstacles.add(ro);
                            }
                            break;
                        case "Checkpoint":
                            if (tokens.length >= 2) {
                                double x = Double.parseDouble(tokens[0]);
                                double y = Double.parseDouble(tokens[1]);
                                sublevel.checkpoint = new Checkpoint(sublevel.pane, x, y);
                            }
                            break;
                        case "Coin":
                            if (tokens.length >= 4) {
                                double x = Double.parseDouble(tokens[0]);
                                double y = Double.parseDouble(tokens[1]);
                                int radius = Integer.parseInt(tokens[2]);
                                int value = Integer.parseInt(tokens[3]);
                                Coin coin = new Coin(sublevel.pane, x, y, radius, value);
                                sublevel.items.add(coin);
                            }
                            break;
                        case "Lock":
                            if (tokens.length >= 4) {
                                double lockX = Double.parseDouble(tokens[0]);
                                double lockY = Double.parseDouble(tokens[1]);
                                double keyX = Double.parseDouble(tokens[2]);
                                double keyY = Double.parseDouble(tokens[3]);
                                Random rand = new Random();
                                Color color = new Color(rand.nextDouble(), rand.nextDouble(), rand.nextDouble(), 1.0); // 1.0 is full opacity

                                Lock lock = new Lock(sublevel.pane, lockX, lockY, 30, 50, color,
                                        keyX, keyY);
                                sublevel.locks.add(lock);
                            }
                            break;
                        case "SizeShifter":
                            if (tokens.length >= 4) {
                                double x = Double.parseDouble(tokens[0]);
                                double y = Double.parseDouble(tokens[1]);
                                int radius = Integer.parseInt(tokens[2]);
                                int increment = Integer.parseInt(tokens[3]);
                                SizeShifter ss = new SizeShifter(sublevel.pane, x, y, radius, increment);
                                sublevel.items.add(ss);
                            }
                            break;
                        case "GrapplePoint":
                            if (tokens.length >= 3) {
                                double x = Double.parseDouble(tokens[0]);
                                double y = Double.parseDouble(tokens[1]);
                                int radius = Integer.parseInt(tokens[2]);
                                GrapplePoint gp = new GrapplePoint(sublevel.pane, x, y, radius);
                                sublevel.displacers.add(gp);
                            }
                            break;
                        case "Goal":
                            if (tokens.length >= 1) {
                                double x = Double.parseDouble(tokens[0]);
                                sublevel.goal = new Goal(sublevel.pane, x);
                            }
                            break;
                        case "LaserObstacle":
                            if (tokens.length >= 3) {
                                double yPos = Double.parseDouble(tokens[0]);
                                double startX = Double.parseDouble(tokens[1]);
                                double endX = Double.parseDouble(tokens[2]);
                                double initialTimerOffset = 0.0; // Default value
                                if (tokens.length >= 4) {
                                    initialTimerOffset = Double.parseDouble(tokens[3]);
                                }
                                // Calculate initiallyOn based on offset, consistent with LaserObstacle constructor
                                boolean initiallyOn = (initialTimerOffset % 4.0) < 2.0; // Assuming cycleDuration=4.0, onDuration=2.0

                                boolean isPulsing = false;
                                double minThickness = 3.0; // DEFAULT_LASER_THICKNESS
                                double maxThickness = 3.0; // DEFAULT_LASER_THICKNESS
                                double pulseDuration = 1.0;

                                // tokens[4] is isPulsing
                                if (tokens.length > 4) {
                                    isPulsing = Integer.parseInt(tokens[4]) == 1;
                                    if (isPulsing) {
                                        // tokens[5] is minThickness, tokens[6] is maxThickness, tokens[7] is pulseDuration
                                        if (tokens.length > 7) {
                                            minThickness = Double.parseDouble(tokens[5]);
                                            maxThickness = Double.parseDouble(tokens[6]);
                                            pulseDuration = Double.parseDouble(tokens[7]);
                                        } else { // isPulsing is true, but not all 3 specific params given
                                            minThickness = 1.0;
                                            maxThickness = 5.0;
                                        }
                                    }
                                }

                                LaserObstacle laser = new LaserObstacle(
                                        sublevel.pane,
                                        LaserOrientation.HORIZONTAL,
                                        yPos, startX, endX,
                                        initiallyOn, initialTimerOffset,
                                        isPulsing, minThickness, maxThickness, pulseDuration // New params
                                );
                                sublevel.obstacles.add(laser);
                            }
                            break;
                        case "VerticalLaserObstacle":
                            if (tokens.length >= 3) {
                                double xPos = Double.parseDouble(tokens[0]);     // primaryAxisPos (x for vertical)
                                double startY = Double.parseDouble(tokens[1]);  // startSecondaryAxis (startY for vertical)
                                double endY = Double.parseDouble(tokens[2]);    // endSecondaryAxis (endY for vertical)
                                double initialTimerOffset = 0.0;
                                if (tokens.length >= 4) {
                                    initialTimerOffset = Double.parseDouble(tokens[3]);
                                }
                                boolean initiallyOn = (initialTimerOffset % 4.0) < 2.0; // Consistent calculation

                                boolean isPulsing = false;
                                double minThickness = 3.0; // DEFAULT_LASER_THICKNESS
                                double maxThickness = 3.0; // DEFAULT_LASER_THICKNESS
                                double pulseDuration = 1.0;

                                // tokens[4] is isPulsing
                                if (tokens.length > 4) {
                                    isPulsing = Integer.parseInt(tokens[4]) == 1;
                                    if (isPulsing) {
                                        // tokens[5] is minThickness, tokens[6] is maxThickness, tokens[7] is pulseDuration
                                        if (tokens.length > 7) {
                                            minThickness = Double.parseDouble(tokens[5]);
                                            maxThickness = Double.parseDouble(tokens[6]);
                                            pulseDuration = Double.parseDouble(tokens[7]);
                                        } else { // isPulsing is true, but not all 3 specific params given
                                            minThickness = 1.0;
                                            maxThickness = 5.0;
                                        }
                                    }
                                }

                                LaserObstacle verticalLaser = new LaserObstacle(
                                        sublevel.pane,
                                        LaserOrientation.VERTICAL,
                                        xPos, startY, endY,
                                        initiallyOn, initialTimerOffset,
                                        isPulsing, minThickness, maxThickness, pulseDuration // New params
                                );
                                sublevel.obstacles.add(verticalLaser);
                            }
                            break;
                        case "SpinningLaserObstacle":
                            // Expected format: pivotX pivotY length initialAngleDeg rotationSpeedDegPerSec [timerOffset] [isPulsing] [minThick] [maxThick] [pulseDur]
                            if (tokens.length >= 5) {
                                double pivotX = Double.parseDouble(tokens[0]);
                                double pivotY = Double.parseDouble(tokens[1]);
                                double length = Double.parseDouble(tokens[2]);
                                double initialAngleDegrees = Double.parseDouble(tokens[3]);
                                double rotationSpeedDegrees = Double.parseDouble(tokens[4]);
                                double initialTimerOffsetSpin = 0.0; // Default
                                if (tokens.length >= 6) {
                                    initialTimerOffsetSpin = Double.parseDouble(tokens[5]);
                                }

                                Point2D pivot = new Point2D(pivotX, pivotY);

                                boolean isPulsingSpin = false;
                                double minThicknessSpin = 3.0; // DEFAULT_LASER_THICKNESS
                                double maxThicknessSpin = 3.0; // DEFAULT_LASER_THICKNESS
                                double pulseDurationSpin = 1.0;

                                // tokens[6] is isPulsing for spinning lasers
                                if (tokens.length > 6) {
                                    isPulsingSpin = Integer.parseInt(tokens[6]) == 1;
                                    if (isPulsingSpin) {
                                        // tokens[7] is minThickness, tokens[8] is maxThickness, tokens[9] is pulseDuration
                                        if (tokens.length > 9) {
                                            minThicknessSpin = Double.parseDouble(tokens[7]);
                                            maxThicknessSpin = Double.parseDouble(tokens[8]);
                                            pulseDurationSpin = Double.parseDouble(tokens[9]);
                                        } else { // isPulsing is true, but not all 3 specific params given
                                            minThicknessSpin = 1.0;
                                            maxThicknessSpin = 5.0;
                                        }
                                    }
                                }

                                SpinningLaserObstacle spinningLaser = new SpinningLaserObstacle(
                                        sublevel.pane,
                                        pivot, length,
                                        initialAngleDegrees, rotationSpeedDegrees,
                                        initialTimerOffsetSpin,
                                        isPulsingSpin, minThicknessSpin, maxThicknessSpin, pulseDurationSpin // New params
                                );
                                sublevel.obstacles.add(spinningLaser);
                            }
                            break;
                        case "TrackingLaserObstacle":
                            // Expected format: emitterX emitterY rotationSpeedDeg detectionRange beamLength chargeSecs fireSecs cooldownSecs [initialAngleDeg]
                            if (tokens.length >= 8) {
                                double emitterX = Double.parseDouble(tokens[0]);
                                double emitterY = Double.parseDouble(tokens[1]);
                                double rotationSpeedDeg = Double.parseDouble(tokens[2]);
                                double detectionRange = Double.parseDouble(tokens[3]);
                                double beamLength = Double.parseDouble(tokens[4]);
                                double chargeSecs = Double.parseDouble(tokens[5]);
                                double fireSecs = Double.parseDouble(tokens[6]);
                                double cooldownSecs = Double.parseDouble(tokens[7]);

                                double initialAngleDeg = 0.0; // Default initial angle
                                if (tokens.length >= 9) {
                                    initialAngleDeg = Double.parseDouble(tokens[8]);
                                }

                                Point2D emitterPos = new Point2D(emitterX, emitterY);

                                TrackingLaserObstacle trackingLaser = new TrackingLaserObstacle(
                                        sublevel.pane,
                                        emitterPos,
                                        rotationSpeedDeg,
                                        detectionRange,
                                        beamLength,
                                        chargeSecs,
                                        fireSecs,
                                        cooldownSecs,
                                        initialAngleDeg
                                );
                                sublevel.obstacles.add(trackingLaser);
                            }
                            break;
                        case "HomingMissileLauncherObstacle":
                            // Expected format: emitterX emitterY rotSpeedDeg detectRange lockonSecs fireInterval volleySize cooldownSecs projSpeed projTurnRateDeg projLifespan [initialAngleDeg]
                            if (tokens.length >= 11) { // 11 mandatory parameters
                                double emitterX = Double.parseDouble(tokens[0]);
                                double emitterY = Double.parseDouble(tokens[1]);
                                double rotSpeedDeg = Double.parseDouble(tokens[2]);
                                double detectRange = Double.parseDouble(tokens[3]);
                                double lockonSecs = Double.parseDouble(tokens[4]);
                                double fireInterval = Double.parseDouble(tokens[5]);
                                int volleySize = Integer.parseInt(tokens[6]);
                                double cooldownSecs = Double.parseDouble(tokens[7]);
                                double projSpeed = Double.parseDouble(tokens[8]);
                                double projTurnRateDeg = Double.parseDouble(tokens[9]);
                                double projLifespan = Double.parseDouble(tokens[10]);

                                double initialAngleDeg = 0.0; // Default initial angle
                                if (tokens.length >= 12) {
                                    initialAngleDeg = Double.parseDouble(tokens[11]);
                                }

                                Point2D emitterPos = new Point2D(emitterX, emitterY);

                                HomingMissileLauncherObstacle launcher = new HomingMissileLauncherObstacle(
                                        sublevel.pane, // Pass the sublevel's pane
                                        emitterPos,
                                        rotSpeedDeg,
                                        detectRange,
                                        lockonSecs,
                                        fireInterval,
                                        volleySize,
                                        cooldownSecs,
                                        projSpeed,
                                        projTurnRateDeg,
                                        projLifespan,
                                        initialAngleDeg
                                );
                                sublevel.obstacles.add(launcher);
                            }
                            break;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + filename);
        }

        return sublevel;
    }

    public static void loadCustomStage() {
        Pane custom = new Pane(canvas);
    }

    public static void loadFinishPage() {
        Pane finishPage = new Pane(canvas);
        Main.scene.setRoot(finishPage);

        Text congratulation = new Text("Congratulations!");
        congratulation.setFont(new Font(60));
        congratulation.setLayoutX(30);
        congratulation.setLayoutY(80);
        finishPage.getChildren().add(congratulation);

        Button nextStage = new Button("Next Stage");
        nextStage.setLayoutX(600);
        nextStage.setLayoutY(480);

        finishPage.getChildren().add(nextStage);

        Button selectStage = new Button("Select Stage");
        selectStage.setLayoutX(600);
        selectStage.setLayoutY(520);
        selectStage.setOnAction(event_selectStage -> {
            loadSelectStage();
        });
        finishPage.getChildren().add(selectStage);

        Button mainPage = new Button("Main Page");
        mainPage.setLayoutX(600);
        mainPage.setLayoutY(560);
        mainPage.setOnAction(event_mainPage -> {
            loadMainPage();
        });
        finishPage.getChildren().add(mainPage);

        Button quit = new Button("quit");
        quit.setLayoutX(600);
        quit.setLayoutY(600);
        quit.setOnAction(event_quit -> {
            Platform.exit();
        });
        finishPage.getChildren().add(quit);
    }

    public static void loadDeathPage() {
        Pane deathPage = new Pane();
        Main.scene.setRoot(deathPage);

        Text youDied = new Text("YOU DIED");
        youDied.setFont(new Font(60));
        youDied.setLayoutX(30);
        youDied.setLayoutY(80);
        deathPage.getChildren().add(youDied);

        Button retry = new Button("Retry");
        retry.setLayoutX(600);
        retry.setLayoutY(480);
        retry.setOnAction(event_retry -> {
            character.inGame = true;
            loadStage(1);
        });
        deathPage.getChildren().add(retry);

        Button selectStage = new Button("Select Stage");
        selectStage.setLayoutX(600);
        selectStage.setLayoutY(520);
        selectStage.setOnAction(event_selectStage -> {
            loadSelectStage();
        });
        deathPage.getChildren().add(selectStage);

        Button mainPage = new Button("Main Page");
        mainPage.setLayoutX(600);
        mainPage.setLayoutY(560);
        mainPage.setOnAction(event_mainPage -> {
            loadMainPage();
        });
        deathPage.getChildren().add(mainPage);

        Button quit = new Button("quit");
        quit.setLayoutX(600);
        quit.setLayoutY(600);
        quit.setOnAction(event_quit -> {
            Platform.exit();
        });
        deathPage.getChildren().add(quit);
    }
}
