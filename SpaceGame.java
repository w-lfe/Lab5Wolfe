/** Project: Solo Lab 5 Assignment
 * Purpose Details: Completed Space Game
 * Course: IST 242
 * Author: Kadin
 * Date Developed: 6/20
 * Last Date Changed:
 * Rev:

 */

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SpaceGame extends JFrame implements KeyListener {
    private static final int WIDTH = 500;
    private static final int HEIGHT = 500;
    private static final int PLAYER_WIDTH = 50;
    private static final int PLAYER_HEIGHT = 50;
    private static final int OBSTACLE_WIDTH = 50;
    private static final int OBSTACLE_HEIGHT = 50;
    private static final int PROJECTILE_WIDTH = 5;
    private static final int PROJECTILE_HEIGHT = 10;
    private static final int PLAYER_SPEED = 5;
    private static final int OBSTACLE_SPEED = 3;
    private static final int PROJECTILE_SPEED = 10;
    private static final int STAR_COUNT = 100;  // Number of stars in the background
    private static final int SHIELD_DURATION_MS = 5000;  // Shield duration in milliseconds
    private static final int PLAYER_MAX_HEALTH = 100;  // Player maximum health
    private static final int GAME_DURATION_SEC = 60;  // Game duration in seconds
    private static final int MOTHERSHIP_HEALTH = 100; // Mothership maximum health

    private int score = 0;
    private int playerHealth = PLAYER_MAX_HEALTH;
    private int mothershipHealth = MOTHERSHIP_HEALTH;
    private int timeLeft = GAME_DURATION_SEC;
    private boolean isChallengeMode = false;
    private boolean isWinner = false;

    private JPanel gamePanel;
    private JLabel scoreLabel;
    private JLabel healthLabel;
    private JLabel mothershipHealthLabel;
    private JLabel timerLabel;
    private Timer timer;
    private Timer shieldTimer;
    private Timer gameTimer;
    private boolean isGameOver;
    private boolean isShieldActive;
    private int playerX, playerY;
    private int projectileX, projectileY;
    private boolean isProjectileVisible;
    private boolean isFiring;
    private List<Point> obstacles;
    private List<Point> powerUps; // List to hold power-up positions
    private List<Point> stars;  // List to hold star positions
    private List<Color> starColors;  // List to hold star colors

    private BufferedImage playerImage;
    private BufferedImage obstacleImage;
    private BufferedImage mothershipImage;
    private Clip fireClip;
    private Clip collisionClip;
    private Random random;  // Random generator for star positions and colors
    private int mothershipX, mothershipY;
    private boolean mothershipMovingRight = true; // Mothership movement direction


    public SpaceGame() {
        setTitle("Space Game");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        random = new Random();
        stars = new ArrayList<>();
        starColors = new ArrayList<>();
        powerUps = new ArrayList<>();

        gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                draw(g);
            }
        };

        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setForeground(Color.BLUE);
        scoreLabel.setBounds(10, 10, 100, 20);
        gamePanel.add(scoreLabel);
   healthLabel = new JLabel("Health: " + PLAYER_MAX_HEALTH);
        healthLabel.setForeground(Color.GREEN);
        healthLabel.setBounds(400, 450, 100, 20);
        gamePanel.add(healthLabel);

        mothershipHealthLabel = new JLabel("");
        mothershipHealthLabel.setForeground(Color.RED);
        mothershipHealthLabel.setBounds(400, 470, 100, 20);
        gamePanel.add(mothershipHealthLabel);

        timerLabel = new JLabel("Time: " + timeLeft);
        timerLabel.setForeground(Color.WHITE);
        timerLabel.setBounds(10, 30, 100, 20);
        gamePanel.add(timerLabel);

        add(gamePanel);
        gamePanel.setFocusable(true);
        gamePanel.addKeyListener(this);

        playerX = WIDTH / 2 - PLAYER_WIDTH / 2;
        playerY = HEIGHT - PLAYER_HEIGHT - 20;
        projectileX = playerX + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2;
        projectileY = playerY;
        isProjectileVisible = false;
        isGameOver = false;
        isShieldActive = false;
        isFiring = false;
        obstacles = new ArrayList<>();

        loadImages();
        loadAudio();
        initializeStars();

        timer = new Timer(20, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isGameOver) {
                    update();
                    gamePanel.repaint();
                }
            }
        });
        timer.start();

        gameTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timeLeft--;
                timerLabel.setText("Time: " + timeLeft);
                if (timeLeft <= 0) {
                    if (isChallengeMode) {
                        if (mothershipHealth <= 0) {
                            isWinner = true;
                            isGameOver = true;
                        } else {
                            isGameOver = true;
                        }
                    } else {
                        if (score < 100) {
                            isGameOver = true;
                        } else {
                            startChallengeMode();
                        }
                    }
                }
                if (isGameOver) {
                    if (isWinner) {
                        displayMessage("WINNER!!");
                    } else {
                        displayMessage("Game Over!");
                    }
                }
            }
        });
        gameTimer.start();
    }


    private void loadImages() {
        try {
            playerImage = ImageIO.read(getClass().getResource("/OAKESCRUISER.gif"));
            obstacleImage = ImageIO.read(getClass().getResource("/ASTEROID.gif"));
            mothershipImage = ImageIO.read(getClass().getResource("/MOTHERSHIP.gif"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadAudio() {
        try {
            URL fireUrl = getClass().getResource("/fire.wav");
            fireClip = AudioSystem.getClip();
            AudioInputStream fireAudioStream = AudioSystem.getAudioInputStream(fireUrl);
            fireClip.open(fireAudioStream);

            URL collisionUrl = getClass().getResource("/collision.wav");
            collisionClip = AudioSystem.getClip();
            AudioInputStream collisionAudioStream = AudioSystem.getAudioInputStream(collisionUrl);
            collisionClip.open(collisionAudioStream);
        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException e) {
            e.printStackTrace();
        }
    }

    private void initializeStars() {
        for (int i = 0; i < STAR_COUNT; i++) {
            stars.add(new Point(random.nextInt(WIDTH), random.nextInt(HEIGHT)));
            starColors.add(new Color(random.nextFloat(), random.nextFloat(), random.nextFloat()));
        }
    }

    private void draw(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw stars
        for (int i = 0; i < stars.size(); i++) {
            g.setColor(starColors.get(i));
            g.fillRect(stars.get(i).x, stars.get(i).y, 2, 2);
        }

        g.drawImage(playerImage, playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT, null);

        if (isProjectileVisible) {
            g.setColor(Color.GREEN);
            g.fillRect(projectileX, projectileY, PROJECTILE_WIDTH, PROJECTILE_HEIGHT);
        }

        for (Point obstacle : obstacles) {
            g.drawImage(obstacleImage, obstacle.x, obstacle.y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT, null);
        }

        // Draw power-ups
        g.setColor(Color.RED);
        for (Point powerUp : powerUps) {
            g.fillRect(powerUp.x, powerUp.y, 15, 15);
        }

        if (isShieldActive) {
            g.setColor(new Color(0, 0, 255, 100));
            g.fillOval(playerX - 10, playerY - 10, PLAYER_WIDTH + 20, PLAYER_HEIGHT + 20);
        }

        if (isChallengeMode) {
            g.drawImage(mothershipImage, mothershipX, mothershipY, PLAYER_WIDTH, PLAYER_HEIGHT, null);
        }

        if (isGameOver) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("Game Over!", WIDTH / 2 - 80, HEIGHT / 2);
        }
    }

    private void update() {
        if (!isGameOver) {
            // Move obstacles
            for (int i = 0; i < obstacles.size(); i++) {
                obstacles.get(i).y += isChallengeMode ? (int)(OBSTACLE_SPEED * 1.5) : OBSTACLE_SPEED;
                if (obstacles.get(i).y > HEIGHT) {
                    obstacles.remove(i);
                    i--;
                }
            }

            // Generate new obstacles
            if (Math.random() < 0.02) {
                int obstacleX = (int) (Math.random() * (WIDTH - OBSTACLE_WIDTH));
                obstacles.add(new Point(obstacleX, 0));
            }

            // Move power-ups
            for (int i = 0; i < powerUps.size(); i++) {
                powerUps.get(i).y += OBSTACLE_SPEED;
                if (powerUps.get(i).y > HEIGHT) {
                    powerUps.remove(i);
                    i--;
                }
            }

            // Generate new power-ups
            if (Math.random() < 0.01) {
                int powerUpX = (int) (Math.random() * (WIDTH - 15));
                powerUps.add(new Point(powerUpX, 0));
            }

            // Move projectile
            if (isProjectileVisible) {
                projectileY -= PROJECTILE_SPEED;
                if (projectileY < 0) {
                    isProjectileVisible = false;
                }
            }

            // Check collision with player
            Rectangle playerRect = new Rectangle(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);
            for (Point obstacle : obstacles) {
                Rectangle obstacleRect = new Rectangle(obstacle.x, obstacle.y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
                if (playerRect.intersects(obstacleRect)) {
                    if (!isShieldActive) {
                        collisionClip.setFramePosition(0);
                        collisionClip.start();
                        playerHealth -= 10;
                        if (playerHealth <= 0) {
                            isGameOver = true;
                        }
                        healthLabel.setText("Health: " + playerHealth);
                    }
                    obstacles.remove(obstacle);
                    break;
                }
            }

            // Check collision with power-ups
            for (int i = 0; i < powerUps.size(); i++) {
                Rectangle powerUpRect = new Rectangle(powerUps.get(i).x, powerUps.get(i).y, 15, 15);
                if (playerRect.intersects(powerUpRect)) {
                    if (playerHealth < PLAYER_MAX_HEALTH) {
                        playerHealth = Math.min(PLAYER_MAX_HEALTH, playerHealth + 10);
                        healthLabel.setText("Health: " + playerHealth);
                    }
                    powerUps.remove(i);
                    i--;
                }
            }

            // Check collision with obstacles using projectiles
            Rectangle projectileRect = new Rectangle(projectileX, projectileY, PROJECTILE_WIDTH, PROJECTILE_HEIGHT);
            for (int i = 0; i < obstacles.size(); i++) {
                Rectangle obstacleRect = new Rectangle(obstacles.get(i).x, obstacles.get(i).y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
                if (projectileRect.intersects(obstacleRect)) {
                    obstacles.remove(i);
                    score += 10;
                    isProjectileVisible = false;
                    collisionClip.setFramePosition(0);
                    collisionClip.start();
                    break;
                }
            }

            if (isChallengeMode) {
                // Move mothership
                if (mothershipMovingRight) {
                    mothershipX += PLAYER_SPEED;
                    if (mothershipX + PLAYER_WIDTH >= WIDTH) {
                        mothershipMovingRight = false;
                    }
                } else {
                    mothershipX -= PLAYER_SPEED;
                    if (mothershipX <= 0) {
                        mothershipMovingRight = true;
                    }
                }

                // Check collision with mothership using projectiles
                Rectangle mothershipRect = new Rectangle(mothershipX, mothershipY, PLAYER_WIDTH, PLAYER_HEIGHT);
                if (projectileRect.intersects(mothershipRect)) {
                    mothershipHealth -= 10;
                    isProjectileVisible = false;
                    mothershipHealthLabel.setText("Mothership: " + mothershipHealth);
                    if (mothershipHealth <= 0) {
                        isWinner = true;
                        isGameOver = true;
                    }
                }
            }

            scoreLabel.setText("Score: " + score);
        }
    }

    private void resetGame() {
        score = 0;
        playerHealth = PLAYER_MAX_HEALTH;
        mothershipHealth = MOTHERSHIP_HEALTH;
        timeLeft = GAME_DURATION_SEC;
        obstacles.clear();
        powerUps.clear();
        isGameOver = false;
        isShieldActive = false;
        isProjectileVisible = false;
        isChallengeMode = false;
        playerX = WIDTH / 2 - PLAYER_WIDTH / 2;
        playerY = HEIGHT - PLAYER_HEIGHT - 20;
        projectileX = playerX + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2;
        projectileY = playerY;

        scoreLabel.setText("Score: 0");
        healthLabel.setText("Health: " + PLAYER_MAX_HEALTH);
        mothershipHealthLabel.setText("");
        timerLabel.setText("Time: " + timeLeft);
        gameTimer.restart();
    }

    private void startChallengeMode() {
        isChallengeMode = true;
        mothershipHealth = MOTHERSHIP_HEALTH;
        mothershipX = WIDTH / 2 - PLAYER_WIDTH / 2;
        mothershipY = 30;
        mothershipHealthLabel.setText("Mothership: " + mothershipHealth);
        timeLeft = GAME_DURATION_SEC;
        timerLabel.setText("Time: " + timeLeft);
        gameTimer.restart();
    }

    private void displayMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
        resetGame();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_A && playerX > 0) {
            playerX -= PLAYER_SPEED;
        } else if (keyCode == KeyEvent.VK_D && playerX < WIDTH - PLAYER_WIDTH) {
            playerX += PLAYER_SPEED;
        } else if (keyCode == KeyEvent.VK_W && playerY > 0) {
            playerY -= PLAYER_SPEED;
        } else if (keyCode == KeyEvent.VK_S && playerY < HEIGHT - PLAYER_HEIGHT) {
            playerY += PLAYER_SPEED;
        } else if (keyCode == KeyEvent.VK_SPACE && !isFiring) {
            isFiring = true;
            projectileX = playerX + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2;
            projectileY = playerY;
            isProjectileVisible = true;
            fireClip.setFramePosition(0);
            fireClip.start();
            new Thread(() -> {
                try {
                    Thread.sleep(500); // Limit firing rate
                    isFiring = false;
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }).start();
        } else if (keyCode == KeyEvent.VK_CONTROL && !isShieldActive) {
            isShieldActive = true;
            shieldTimer = new Timer(SHIELD_DURATION_MS, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    isShieldActive = false;
                }
            });
            shieldTimer.setRepeats(false);
            shieldTimer.start();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SpaceGame().setVisible(true));
    }
}


