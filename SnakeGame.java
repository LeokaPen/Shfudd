import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.File;
import java.io.IOException;




public class SnakeGame extends JPanel implements ActionListener, KeyListener {
    private class Tile {
        int x;
        int y;

        Tile(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    enum SnakeType { 
        GREEN, BLUE, YELLOW 
    }

    enum Difficulty {
        EASY(150), MEDIUM(100), HARD(50);

        int speed;

        Difficulty(int speed) {
            this.speed = speed;
        }
    }

    int boardWidth;
    int boardHeight;
    int tileSize = 25;

    // Snake
    Tile snakeHead;
    ArrayList<Tile> snakeBody;
    Color snakeColor;
    SnakeType snakeType;
    boolean snakeTypeSelected = false;

    // Food
    Tile food;
    Tile specialFood;
    Random random;

    // Game logic
    int velocityX;
    int velocityY;
    Timer gameLoop;
    long lastMoveTime = 0; // Delay between key presses

    boolean gameOver = false;
    boolean paused = false;
    boolean gameStarted = false;

    // Scoreboard
    int score = 0;

    // Difficulty
    Difficulty difficulty;
    boolean difficultySelected = false;

    // Music
    Clip backgroundMusic;
    Clip eatSoundEffect; // Added for sound effect

    // Background Images
    Image mainMenuBackground;
    Image gameBackground;

    // Full screen mode
    boolean isFullScreen = false;
    JFrame frame;

    SnakeGame(int boardWidth, int boardHeight, JFrame frame) {
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        this.frame = frame;
        setPreferredSize(new Dimension(this.boardWidth, this.boardHeight));
        setBackground(Color.darkGray);
        addKeyListener(this);
        setFocusable(true);

        // Load background images
        mainMenuBackground = loadImage("image/background.jpg"); // Add the path to your main menu background image
        gameBackground = loadImage("image/game.jpg"); // Add the path to your game background image

        // Default settings
        snakeType = SnakeType.GREEN;
        difficulty = Difficulty.MEDIUM;
        setSnakeAttributes(snakeType);

        food = new Tile(10, 10);
        specialFood = new Tile(-1, -1); // Start offscreen
        random = new Random();
        placeFood();

        velocityX = 1;
        velocityY = 0;

        // Game timer based on difficulty
        gameLoop = new Timer(difficulty.speed, this);

        // Load and start background music
        loadMusic("audio/gameplay.wav");  // Replace with the path to your music file
        playMusic();

        // Load sound effect
        eatSoundEffect = loadSoundEffect("audio/eat.wav.wav"); // Replace with the path to your sound effect file
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawBackground(g); // Draw background for both game and start screen
        if (gameStarted) {
            draw(g);
        } else {
            drawStartScreen(g);
        }
    }

    // Draw a simple gradient background
    public void drawBackground(Graphics g) {
        // Use game background if the game has started
        if (gameStarted) {
            g.drawImage(gameBackground, 0, 0, boardWidth, boardHeight, null);
        } else {
            g.drawImage(mainMenuBackground, 0, 0, boardWidth, boardHeight, null);
        }
    }

    public void drawStartScreen(Graphics g) {
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.BOLD, 36));
        g.drawString("Select Your Snake ", boardWidth / 2 - 160, boardHeight / 4);

        g.setFont(new Font("Arial", Font.PLAIN, 24));

        // Draw boxes around each snake option
        g.drawRect(boardWidth / 2 - 100, boardHeight / 2 - 60, 200, 40);
        g.drawRect(boardWidth / 2 - 100, boardHeight / 2 - 20, 200, 40);
        g.drawRect(boardWidth / 2 - 100, boardHeight / 2 + 20, 200, 40);

        // Draw snake options text inside boxes
        g.drawString("1. Green Snake", boardWidth / 2 - 80, boardHeight / 2 - 40);
        g.drawString("2. Blue Snake", boardWidth / 2 - 80, boardHeight / 2);
        g.drawString("3. Yellow Snake", boardWidth / 2 - 80, boardHeight / 2 + 40);

        if (snakeTypeSelected) {
            g.drawString("Select Difficulty", boardWidth / 2 - 120, boardHeight / 2 + 100);

            // Draw boxes around each difficulty option
            g.drawRect(boardWidth / 2 - 100, boardHeight / 2 + 120, 200, 40);
            g.drawRect(boardWidth / 2 - 100, boardHeight / 2 + 160, 200, 40);
            g.drawRect(boardWidth / 2 - 100, boardHeight / 2 + 200, 200, 40);

            // Draw difficulty options text inside boxes
            g.drawString("E. Easy", boardWidth / 2 - 80, boardHeight / 2 + 140);
            g.drawString("M. Medium", boardWidth / 2 - 80, boardHeight / 2 + 180);
            g.drawString("H. Hard", boardWidth / 2 - 80, boardHeight / 2 + 220);
            g.drawString("Press E, M, or H to choose Difficulty.", boardWidth / 2 - 160, boardHeight / 2 + 300);
        } else {
            g.drawString("Press 1, 2, or 3 to choose Snake.", boardWidth / 2 - 140, boardHeight / 2 + 260);
        }
    }

    public void setSnakeAttributes(SnakeType type) {
        switch (type) {
            case GREEN:
                snakeColor = Color.green;
                break;
            case BLUE:
                snakeColor = Color.blue;
                break;
            case YELLOW:
                snakeColor = Color.yellow;
                break;
        }
    }

    public void startGame() {
        snakeHead = new Tile(5, 5);
        snakeBody = new ArrayList<Tile>();
        gameStarted = true;
        gameLoop.setDelay(difficulty.speed);
        gameLoop.start();
    }

    public void draw(Graphics g) {
        // Grid Lines
        for (int i = 0; i < boardWidth / tileSize; i++) {
            g.drawLine(i * tileSize, 0, i * tileSize, boardHeight);
            g.drawLine(0, i * tileSize, boardWidth, i * tileSize);
            g.setColor(Color.gray);
        }

        // Food
        g.setColor(Color.red);
        g.fill3DRect(food.x * tileSize, food.y * tileSize, tileSize, tileSize, true);

        // Special Food
        if (specialFood.x != -1 && specialFood.y != -1) {
            g.setColor(Color.orange);
            g.fill3DRect(specialFood.x * tileSize, specialFood.y * tileSize, tileSize, tileSize, true);
        }

        // Snake Head
        g.setColor(snakeColor);
        g.fill3DRect(snakeHead.x * tileSize, snakeHead.y * tileSize, tileSize, tileSize, true);

        // Snake Body
        for (Tile snakePart : snakeBody) {
            g.fill3DRect(snakePart.x * tileSize, snakePart.y * tileSize, tileSize, tileSize, true);
        }

        // Score
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        g.setColor(Color.white);
        g.drawString("Score: " + score, 10, 20);

        if (paused) {
            g.setColor(Color.yellow);
            g.setFont(new Font("Arial", Font.BOLD, 36));
            g.drawString("Paused", boardWidth / 2 - 70, boardHeight / 2);
        }

        if (gameOver) {
            g.setColor(Color.red);
            g.setFont(new Font("Arial", Font.BOLD, 36));
            g.drawString("Game Over", boardWidth / 2 - 100, boardHeight / 2);
            g.setFont(new Font("Arial", Font.PLAIN, 18));
            g.drawString("Press 'R' to Restart or 'M' for Main Menu", boardWidth / 2 - 150, boardHeight / 2 + 40);
            gameLoop.stop();
        }
    }

    public void placeFood() {
        food.x = random.nextInt(boardWidth / tileSize);
        food.y = random.nextInt(boardHeight / tileSize);
    }

    public void placeSpecialFood() {
        specialFood.x = random.nextInt(boardWidth / tileSize);
        specialFood.y = random.nextInt(boardHeight / tileSize);
    }

    public void moveSnake() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMoveTime < 100) return; // Prevent moving too fast
        lastMoveTime = currentTime;

        // Move snake body
        snakeBody.add(0, new Tile(snakeHead.x, snakeHead.y)); // Add new head
        snakeHead.x += velocityX;
        snakeHead.y += velocityY;
        if (snakeBody.size() > score) {
            snakeBody.remove(snakeBody.size() - 1); // Remove last segment
        }
    }

    public void checkCollisions() {
        // Check wall collisions
        if (snakeHead.x < 0 || snakeHead.x >= boardWidth / tileSize || 
            snakeHead.y < 0 || snakeHead.y >= boardHeight / tileSize) {
            gameOver = true;
        }

        // Check self collisions
        for (Tile tile : snakeBody) {
            if (tile.x == snakeHead.x && tile.y == snakeHead.y) {
                gameOver = true;
                break;
            }
        }
    }

    public void checkFoodCollision() {
        if (snakeHead.x == food.x && snakeHead.y == food.y) {
            score++;
            placeFood();
            playSoundEffect(eatSoundEffect); // Play sound effect when eating food
        }

        // Check for special food
        if (specialFood.x == -1 && specialFood.y == -1 && random.nextInt(10) < 2) { // 20% chance to spawn special food
            placeSpecialFood();
        }

        if (snakeHead.x == specialFood.x && snakeHead.y == specialFood.y) {
            score += 5; // Increase score more for special food
            specialFood.x = -1; // Remove special food after eating
            specialFood.y = -1;
            playSoundEffect(eatSoundEffect); // Play sound effect when eating special food
        }
    }

    public Image loadImage(String filePath) {
        Image img = null;
        try {
            img = Toolkit.getDefaultToolkit().getImage(filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return img;
    }

    public void loadMusic(String filePath) {
        try {
            File musicFile = new File(filePath);
            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(AudioSystem.getAudioInputStream(musicFile));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public Clip loadSoundEffect(String filePath) {
        Clip clip = null;
        try {
            File soundFile = new File(filePath);
            clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(soundFile));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return clip;
    }

    public void playSoundEffect(Clip clip) {
        if (clip != null) {
            clip.setFramePosition(0);
            clip.start();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!paused && gameStarted) {
            moveSnake();
            checkCollisions();
            checkFoodCollision();
            repaint();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) {
            if (e.getKeyCode() == KeyEvent.VK_R) {
                restartGame();
            } else if (e.getKeyCode() == KeyEvent.VK_M) {
                showMainMenu();
            }
        } else if (gameStarted) {
            handleArrowKeys(e);
        } else {
            handleStartMenuKeys(e);
        }
    }

    public void handleStartMenuKeys(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_1) {
            snakeType = SnakeType.GREEN;
            snakeTypeSelected = true;
            setSnakeAttributes(snakeType);
            repaint();
        } else if (e.getKeyCode() == KeyEvent.VK_2) {
            snakeType = SnakeType.BLUE;
            snakeTypeSelected = true;
            setSnakeAttributes(snakeType);
            repaint();
        } else if (e.getKeyCode() == KeyEvent.VK_3) {
            snakeType = SnakeType.YELLOW;
            snakeTypeSelected = true;
            setSnakeAttributes(snakeType);
            repaint();
        }

        if (snakeTypeSelected) {
            if (e.getKeyCode() == KeyEvent.VK_E) {
                difficulty = Difficulty.EASY;
                startGame();
            } else if (e.getKeyCode() == KeyEvent.VK_M) {
                difficulty = Difficulty.MEDIUM;
                startGame();
            } else if (e.getKeyCode() == KeyEvent.VK_H) {
                difficulty = Difficulty.HARD;
                startGame();
            }
        }
    }

    public void handleArrowKeys(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_UP && velocityY == 0) {
            velocityX = 0;
            velocityY = -1;
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN && velocityY == 0) {
            velocityX = 0;
            velocityY = 1;
        } else if (e.getKeyCode() == KeyEvent.VK_LEFT && velocityX == 0) {
            velocityX = -1;
            velocityY = 0;
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT && velocityX == 0) {
            velocityX = 1;
            velocityY = 0;
        }
    }

    public void restartGame() {
        score = 0;
        snakeBody.clear();
        snakeHead = new Tile(5, 5);
        placeFood();
        specialFood.x = -1; // Reset special food
        specialFood.y = -1;
        gameOver = false;
        gameLoop.start();
        repaint();
    }

   public void showMainMenu() {
    gameStarted = false;
    gameOver = false;
    snakeTypeSelected = false;
    difficultySelected = false;

    // Reset the snake attributes
    snakeBody.clear(); // Clear the body of the snake
    snakeHead = new Tile(5, 5); // Reset snake head position
    score = 0; // Reset score
    velocityX = 1; // Reset velocity
    velocityY = 0; // Reset velocity

    repaint();
}


    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        JFrame frame = new JFrame("Snake Game");
        SnakeGame snakeGame = new SnakeGame(800, 600, frame);
        frame.add(snakeGame);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setResizable(false);
        frame.setVisible(true);
    }
}


