package com.xc.mygdxgame;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.audio.Sound;
import java.util.Random;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.MathUtils;

// Game state enumeration
enum GameState {
        MENU,
        PLAYING,
    PAUSED,
    GAME_COMPLETE,
    IN_GAME_MENU,
    LEVEL_COMPLETE,
    SHOP,
    GUN_SELECT,  // New water gun selection state
    GAME_OVER    // New game over state
}

// Difficulty enumeration
enum Difficulty {
        EASY,
        NORMAL,
        HARD,
    COMPLETE
}

// Screen mode enumeration
enum ScreenMode {
    WINDOWED,
    FULLSCREEN
}

// Monster type enumeration
enum MonsterType {
    NORMAL(0xFF0000),    // Red
    FAST(0x00FF00),      // Green
    TOUGH(0x0000FF);     // Blue

    private final int color;
    
    MonsterType(int color) {
        this.color = color;
    }
    
    public int getColor() {
        return color;
    }
}

// Difficulty settings class
class DifficultySettings {
    float duckSpeed;         // Duck movement speed
    float duckVerticalSpeed; // Duck vertical movement speed
    int duckCount;          // Number of ducks
    float shootInterval;     // Shooting interval
    float waterPower;       // Water gun power
    
    public DifficultySettings(float speed, float vSpeed, int count, float interval, float power) {
        this.duckSpeed = speed;
        this.duckVerticalSpeed = vSpeed;
        this.duckCount = count;
        this.shootInterval = interval;
        this.waterPower = power;
    }
}

// Monster class
class Monster {
    float x;
    float y;
    boolean active;
    float shootTimer;
    float shootInterval;
    MonsterType type;
    int health = 3;  // Add health attribute, initial value is 3
    
    public Monster(float x, float y, MonsterType type) {
        this.x = x;
        this.y = y;
        this.active = true;
        this.shootTimer = (float)(Math.random() * 2.0f);  // Random initial shooting time
        this.shootInterval = 1.5f + (float)(Math.random() * 1.5f);  // Random shooting interval 1.5-3 seconds
        this.type = type;
        this.health = 3;  // Initialize health
    }
}

// Bullet class
class Bullet {
    float x;
    float y;
    float velocityX;
    float velocityY;
    boolean isEnemy;
    boolean canBlockEnemyBullets;
    
    public Bullet(float x, float y, boolean isEnemy) {
        this.x = x;
        this.y = y;
        this.isEnemy = isEnemy;
        this.canBlockEnemyBullets = false;
        // Default speed
        this.velocityX = isEnemy ? -400 : 400;
        this.velocityY = 0;
    }
}

// Star class
class Star {
    float x;
    float y;
    float size;
    float brightness;
    
    public Star() {
        this.x = (float) Math.random() * 800;
        this.y = (float) Math.random() * 600;
        this.size = 1 + (float) Math.random() * 2;
        this.brightness = 0.5f + (float) Math.random() * 0.5f;
    }
}

// Water gun type enumeration
enum WaterGunType {
    BASIC(0, "Basic Water Gun", 0),
    WATER_GUN_2(1, "Super Soaker", 100),    // Changed from 1000 to 100
    WATER_GUN_3(2, "Hydro Cannon", 200),    // Changed from 2000 to 200
    WATER_GUN_4(3, "Ultimate Blaster", 300); // Changed from 3000 to 300

    private final int id;
    private final String name;
    private final int price;
    private int level = 1;
    private static final int MAX_LEVEL = 3;

    WaterGunType(int id, String name, int price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getPrice() { return price; }
    public int getLevel() { return level; }
    
    // Modify upgrade price calculation method
    public int getUpgradePrice() {
        // First upgrade costs 10 coins
        // Second upgrade costs 20 coins
        // Third upgrade costs 30 coins
        return level * 10;
    }
    
    public boolean canUpgrade() {
        return level < MAX_LEVEL;
    }
    
    public void upgrade() {
        if (level < MAX_LEVEL) {
            level++;
        }
    }
}

// Coin class
class Coin {
    float x;
    float y;
    float velocityY;
    boolean active;
    
    public Coin(float x, float y) {
        this.x = x;
        this.y = y;
        this.velocityY = 0; // Default no falling
        this.active = true;
    }
}

public class Main extends ApplicationAdapter {
    // World size constants
    private static final float MIN_WORLD_WIDTH = 1280f;  // Increased game window width
    private static final float MIN_WORLD_HEIGHT = 720f;  // Increased game window height
    
    // Screen mode related variables
    private ScreenMode currentScreenMode = ScreenMode.WINDOWED;
    private int[] windowedSizes = {
        800, 600,    // Small window
        1024, 768,   // Medium window
        1280, 720,   // Large window
        1920, 1080   // Extra large window
    };
    private int currentWindowSizeIndex = 2; // Default use 1280x720
    
    // Button size constants
    private static final float BUTTON_WIDTH = 200f;
    private static final float BUTTON_HEIGHT = 50f;
    private static final float BUTTON_SPACING = 20f;
    
    // Game related constants
    private static final String PREF_HIGH_SCORE = "highScore";
    private static final float PLANE_SPEED = 500f;  // Increased from 300f to 500f
    private static final float BULLET_SPEED = 300f;
    private static float MONSTER_SPEED = 150f;
    private static float MONSTER_VERTICAL_SPEED = 100f;
    private static int MONSTER_COUNT = 6;
    private static float SHOOT_INTERVAL = 0.5f;
    private static final int STAR_COUNT = 100;
    private static final float BOSS_SHOOT_INTERVAL = 1.5f;
    private static final float RESTART_DELAY = 2.0f;
    private static final float COMPLETE_DISPLAY_TIME = 3.0f;
    private static final int POINTS_PER_KILL = 100;
    private static final float SPAWN_INTERVAL = 3.0f;
    
    // Set plane display size
    private static final float PLANE_WIDTH = 48f;
    private static final float PLANE_HEIGHT = 48f;
    
    // Set bullet size
    private static final float BULLET_WIDTH = 24f;  // Decreased bullet width
    private static final float BULLET_HEIGHT = 12f;  // Decreased bullet height
    
    // Set enemy display size
    private static final float MONSTER_WIDTH = 48f;
    private static final float MONSTER_HEIGHT = 48f;
    
    // Set death animation size
    private static final float DEAD_WIDTH = 48f;
    private static final float DEAD_HEIGHT = 48f;

    // Set Boss size
    private static final float BOSS_WIDTH = 96f;
    private static final float BOSS_HEIGHT = 96f;
    
    // Boss bullet size
    private static final float BOSS_BULLET_WIDTH = 24f;
    private static final float BOSS_BULLET_HEIGHT = 24f;

    // Add game menu buttons
    private Rectangle continueButton;
    private Rectangle backToMainButton;
    private Rectangle inGameQuitButton;

    // Health system
    private int lives = 3;  // Initial 3 lives
    private static final int MAX_LIVES = 5;  // Maximum health limit

    // Add new variables to control invincibility time and blinking effect
    private float invincibleTimer = 0;
    private static final float INVINCIBLE_DURATION = 2f; // Invincibility time 2 seconds
    private boolean isInvincible = false;
    
    // Hit effect variables
    private float hitEffectTimer = 0;
    private static final float HIT_EFFECT_DURATION = 0.3f; // Flash effect duration
    private boolean showHitEffect = false;
    private float shakeTimer = 0;
    private static final float SHAKE_DURATION = 0.3f;
    private static final float SHAKE_INTENSITY = 5.0f;
    private float shakeOffsetX = 0;
    private float shakeOffsetY = 0;

    // Keep these declarations
    private Texture bulletTexture;    // Plane bullet texture
    private Texture bullet2Texture;   // Enemy bullet texture

    // Game related variables
    private Random random;
    private Viewport viewport;
    private TextureRegion deadRegion;
    private Rectangle planeRect;
    private Rectangle monsterRect;
    private Rectangle bulletRect;
    private BitmapFont font;
    private BitmapFont titleFont;  // Font for title in large size

    // New variable
    private SpriteBatch batch;
    private Texture backgroundTexture;
    private Texture planeTexture;
    private Texture monsterTexture;
    private Texture deadTexture;
    private Texture bossTexture;
    private Texture bossShootTexture;
    private float planeX;
    private float planeY;
    private boolean isGameOver;
    private float gameOverTimer;
    private boolean monstersMovingRight;
    private boolean isBossActive;
    private int bossHealth;
    private int bossShootPattern;
    private float bossX;
    private float bossY;
    private Array<Monster> monsters;
    private Array<Bullet> bullets;
    private Array<Star> stars;
    private int currentScore;
    private int highScore;
    private Preferences prefs;
    private Sound shootSound;
    private float shootTimer;
    private float spawnTimer;
    private float completeTimer;
    private Rectangle startGameButton;
    private Rectangle difficultyButton;
    private Rectangle quitButton;
    private Difficulty currentDifficulty;
    private String currentDifficultyText;
    private GameState gameState;
    private DifficultySettings easySettings;
    private DifficultySettings normalSettings;
    private DifficultySettings hardSettings;
    private Texture menuBackground;
    private Texture level1Background;
    private Texture level2Background;
    private Texture level3Background;
    private Texture bossBackground;
    private Music menuMusic;
    private Music gameMusic;
    private Music bossMusic;
    private Sound waterShootSound;
    private Sound duckHitSound;
    private Sound powerupSound;
    private float gameTime = 0f;

    // Add level complete interface buttons
    private Rectangle retryButton;
    private Rectangle nextLevelButton;
    private Rectangle exitToMenuButton;

    // Ensure these texture variables are declared at the beginning of the class
    private Texture poolBackground;
    private Texture beachBackground;
    private Texture parkBackground;
    private Texture stormBackground;

    // Add GlyphLayout to member variables
    private GlyphLayout layout;

    // Shop related constants
    private static final float COIN_WIDTH = 24f;  // Changed coin width from 48 to 24
    private static final float COIN_HEIGHT = 24f; // Changed coin height from 48 to 24
    private static final float COIN_DROP_CHANCE = 1.0f; // 100% drop probability
    
    // Shop related variables
    private int totalCoins;
    private int currentLevelCoins;  // Added current level coin count
    private Array<Coin> coins;
    private Texture coinTexture;
    private Array<WaterGunType> unlockedGuns;
    private WaterGunType currentGun;
    private Rectangle shopButton;
    private Rectangle[] gunButtons;
    private Texture[] waterGunTextures;
    private Rectangle backFromShopButton;

    // Add new constants
    private static final float BASE_BULLET_SPEED = 300f;  // Decreased base bullet speed
    private static final float SPEED_UPGRADE_MULTIPLIER = 0.15f;  // Increase speed by 15% per level
    private static final float BOSS_BULLET_SPEED = 400f; // Increased bullet speed
    private static final float BOSS_BULLET_SPREAD = 15f; // Bullet spread angle
    private float bossShootTimer = 0;
    private int bossAttackPattern = 0; // Used to switch attack patterns

    // Add Boss movement related variables
    private float bossTargetY;  // Boss's target Y coordinate
    private float bossMoveCooldown;  // Boss movement cooldown time
    private static final float BOSS_MOVE_INTERVAL = 2.0f;  // Interval for Boss to change target position
    private static final float BOSS_SPEED = 200f;  // Boss movement speed

    // Add Boss texture variables
    private Texture boss1Texture;
    private Texture boss2Texture;
    private Texture boss3Texture;

    // Add needed variables
    private float autoAttackTimer = 0;
    private static final float AUTO_ATTACK_INTERVAL = 0.5f;

    // Add to member variables in Main class
    private boolean isStartingGame = false;

    // Add to member variables in the class
    private int ducksKilled = 0;  // New: record number of ducks killed
    private Texture monster2Texture; // New: duck icon texture

    // Add to member variables in the class
    private Texture livesTexture;

    @Override
    public void create() {
        // Set vertical synchronization
        Gdx.graphics.setVSync(true);
        
        // Set fullscreen
        setFullscreen();
        
        // Initialize basic components
        batch = new SpriteBatch();
        random = new Random();
        viewport = new ExtendViewport(MIN_WORLD_WIDTH, MIN_WORLD_HEIGHT);
        
        // Initialize game state and difficulty
        gameState = GameState.MENU;
        currentDifficulty = Difficulty.EASY;
        currentDifficultyText = "EASY";
        
        // Initialize arrays
        monsters = new Array<Monster>();
        bullets = new Array<Bullet>();
        stars = new Array<Star>();
        coins = new Array<Coin>();
        unlockedGuns = new Array<WaterGunType>();
        
        // Initialize shop system
        initializeShopSystem();
        
        // Initialize stars
        for (int i = 0; i < STAR_COUNT; i++) {
            stars.add(new Star());
        }
        
        // Initialize collision detection rectangles
        planeRect = new Rectangle();
        bulletRect = new Rectangle();
        monsterRect = new Rectangle();
        
        // Initialize difficulty settings
        initializeDifficulties();
        
        // Initialize buttons
        initializeButtons();
        
        // Load resources
        loadFonts();
        loadTextures();
        loadAudio();
        loadBackgrounds();
        
        // Initialize GlyphLayout
        layout = new GlyphLayout();
        
        // Initialize preferences for saving high score and coins
        prefs = Gdx.app.getPreferences("SpaceGame");
        highScore = prefs.getInteger(PREF_HIGH_SCORE, 0);
        totalCoins = prefs.getInteger("totalCoins", 1000);
        totalCoins += 1000;
        prefs.putInteger("totalCoins", totalCoins);
        prefs.flush();
        loadUnlockedGuns();
        
        // Initialize game objects
        resetGame();
        
        // Set initial water gun to the first unlocked one
        for (WaterGunType gun : WaterGunType.values()) {
            if (isUnlocked(gun)) {
                currentGun = gun;
                planeTexture = waterGunTextures[gun.getId()];
                break;
            }
        }
    }

    private void initializeShopSystem() {
        // Initialize shop buttons
        float buttonWidth = 150f;
        float buttonHeight = 50f;
        shopButton = new Rectangle(MIN_WORLD_WIDTH - buttonWidth - 20, MIN_WORLD_HEIGHT - buttonHeight - 20, 
                                 buttonWidth, buttonHeight);
        
        // Initialize water gun buttons in shop
        gunButtons = new Rectangle[WaterGunType.values().length];
        float startY = MIN_WORLD_HEIGHT * 0.7f;
        float spacing = 100f;
        for (int i = 0; i < gunButtons.length; i++) {
            gunButtons[i] = new Rectangle(
                MIN_WORLD_WIDTH / 2f - 200f,
                startY - i * spacing,
                400f,
                80f
            );
        }
        
        // Initialize return button
        backFromShopButton = new Rectangle(20, MIN_WORLD_HEIGHT - 70, 150, 50);
        
        // Default unlock basic water gun
        unlockedGuns.add(WaterGunType.BASIC);
        currentGun = WaterGunType.BASIC;
        
        // Initialize water gun texture array
        waterGunTextures = new Texture[4];
    }

    private void loadUnlockedGuns() {
        // Load unlocked water guns from preferences
        String unlockedGunsStr = prefs.getString("unlockedGuns", "0"); // Default only basic water gun
        String[] unlockedIds = unlockedGunsStr.split(",");
        for (String id : unlockedIds) {
            try {
                int gunId = Integer.parseInt(id);
                for (WaterGunType gun : WaterGunType.values()) {
                    if (gun.getId() == gunId && !unlockedGuns.contains(gun, true)) {
                        unlockedGuns.add(gun);
                    }
                }
            } catch (NumberFormatException e) {
                System.err.println("Error parsing unlocked gun ID: " + id);
            }
        }
        
        // Load current selected water gun
        int currentGunId = prefs.getInteger("currentGun", 0);
        for (WaterGunType gun : WaterGunType.values()) {
            if (gun.getId() == currentGunId) {
                currentGun = gun;
                break;
            }
        }
    }
    
    private void resetGame() {
        // Initialize game state
        isGameOver = false;
        gameOverTimer = 0;
        monstersMovingRight = true;  // Reset enemy movement direction
        
        // Reset Boss related state
        isBossActive = false;
        bossHealth = 100;
        bossShootTimer = 0;
        bossShootPattern = 0;
        
        // Initialize plane position on screen left middle
        planeX = PLANE_WIDTH;
        planeY = MIN_WORLD_HEIGHT / 2f - PLANE_HEIGHT / 2f;
        
        // Clear existing enemies and bullets
        monsters.clear();
        bullets.clear();
        coins.clear();  // Clear all coins
        
        // Generate new enemies
        spawnMonsters();
        
        // Fully reset game only when these values are reset
        currentLevelCoins = 0;  // Reset current level coin count
        ducksKilled = 0;  // Reset kill count
        currentScore = 0;  // Reset current score
        lives = 3;  // Reset health
        isInvincible = false;
        invincibleTimer = 0;
        
        // Apply current difficulty settings
        applyDifficultySettings();
    }
    
    private void createBulletTexture() {
        // Load plane bullet texture
        bulletTexture = new Texture("bullet.png");
        bulletTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        
        // Load enemy bullet texture
        bullet2Texture = new Texture("bullet2.png");
        bullet2Texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }
    
    private void spawnMonsters() {
        monsters.clear();
        
        // Add default value handling
        if (currentDifficulty == null) {
            currentDifficulty = Difficulty.EASY;
        }
        
        // Generate enemies based on difficulty settings
        switch (currentDifficulty) {
            case EASY:
                // Easy mode: random position, slow movement
        for (int i = 0; i < MONSTER_COUNT; i++) {
                    float x = viewport.getWorldWidth() + random.nextFloat() * 200;
                    float y = random.nextFloat() * (viewport.getWorldHeight() - MONSTER_HEIGHT);
                    monsters.add(new Monster(x, y, MonsterType.NORMAL));
                }
                break;
                
            case NORMAL:
                // Normal mode: two rows alternate
                for (int i = 0; i < MONSTER_COUNT; i++) {
                    float x = viewport.getWorldWidth() + (i % 2) * 100;
                    float y = random.nextFloat() * (viewport.getWorldHeight() - MONSTER_HEIGHT);
                    monsters.add(new Monster(x, y, MonsterType.FAST));
                }
                break;
                
            case HARD:
                // Hard mode: three rows dense formation
                for (int i = 0; i < MONSTER_COUNT; i++) {
                    float x = viewport.getWorldWidth() + (i % 3) * 80;
                    float y = random.nextFloat() * (viewport.getWorldHeight() - MONSTER_HEIGHT);
                    monsters.add(new Monster(x, y, MonsterType.TOUGH));
                }
                break;
            
            default:
                // Default use simple mode generation method
                for (int i = 0; i < MONSTER_COUNT; i++) {
                    float x = viewport.getWorldWidth() + random.nextFloat() * 200;
                    float y = random.nextFloat() * (viewport.getWorldHeight() - MONSTER_HEIGHT);
                    monsters.add(new Monster(x, y, MonsterType.NORMAL));
                }
                break;
        }
    }

    private void updateBullets(float deltaTime) {
        // Update existing bullets
        for (int i = bullets.size - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            bullet.x += bullet.velocityX * deltaTime;
            bullet.y += bullet.velocityY * deltaTime;
            
            // Remove bullets out of screen
            if (bullet.x > viewport.getWorldWidth() || bullet.x < 0 ||
                bullet.y > viewport.getWorldHeight() || bullet.y < 0) {
                bullets.removeIndex(i);
            }
        }

        // Player shooting
        shootTimer += deltaTime;
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.SPACE) && shootTimer >= SHOOT_INTERVAL) {
            shootTimer = 0;
            // Increase bullet speed based on level, each level adds 20%
            float speedMultiplier = 1.0f + (currentGun.getLevel() - 1) * 0.2f;
            float bulletSpeed = BASE_BULLET_SPEED * speedMultiplier;
            
            switch(currentGun) {
                case BASIC:
                    // Single shot straight line
                    createBullet(planeX + PLANE_WIDTH, planeY + PLANE_HEIGHT/2, 
                           bulletSpeed, 0, false);
                    break;
                    
                case WATER_GUN_2:
                    // Double shot
                    createBullet(planeX + PLANE_WIDTH, planeY + PLANE_HEIGHT/2 + 10, 
                           bulletSpeed, 0, false);
                    createBullet(planeX + PLANE_WIDTH, planeY + PLANE_HEIGHT/2 - 10, 
                           bulletSpeed, 0, false);
                    break;
                    
                case WATER_GUN_3:
                case WATER_GUN_4:
                    // Triple shot, can block enemy bullets
                    createBullet(planeX + PLANE_WIDTH, planeY + PLANE_HEIGHT/2, 
                           bulletSpeed, 0, true);
                    createBullet(planeX + PLANE_WIDTH, planeY + PLANE_HEIGHT/2 + 15, 
                           bulletSpeed, 0, true);
                    createBullet(planeX + PLANE_WIDTH, planeY + PLANE_HEIGHT/2 - 15, 
                           bulletSpeed, 0, true);
                    break;
            }
            
            // Play shooting sound effect
            if (waterShootSound != null) {
                waterShootSound.play(0.5f);
            }
        }

        // Water gun 4's auto-attack also uses the new speed calculation
        if (currentGun == WaterGunType.WATER_GUN_4) {
            autoAttackTimer += deltaTime;
            if (autoAttackTimer >= AUTO_ATTACK_INTERVAL) {
                autoAttackTimer = 0;
                Monster target = findNearestEnemy();
                if (target != null) {
                    float dx = target.x - planeX;
                    float dy = target.y - planeY;
                    float angle = (float)Math.atan2(dy, dx);
                    // Increase bullet speed based on level
                    float speedMultiplier = 1.0f + (currentGun.getLevel() - 1) * 0.2f;
                    float bulletSpeed = BASE_BULLET_SPEED * speedMultiplier;
                    
                    createBullet(planeX + PLANE_WIDTH/2, 
                               planeY + PLANE_HEIGHT/2,
                               bulletSpeed * (float)Math.cos(angle),
                               bulletSpeed * (float)Math.sin(angle),
                               true);
                }
            }
        }
    }

    // Helper method: Create bullet
    private void createBullet(float x, float y, float vx, float vy, boolean canBlock) {
        Bullet bullet = new Bullet(x, y, false);
        bullet.velocityX = vx;
        bullet.velocityY = vy;
        bullet.canBlockEnemyBullets = canBlock;
        bullets.add(bullet);
    }

    // Find nearest enemy
    private Monster findNearestEnemy() {
        Monster nearest = null;
        float minDist = Float.MAX_VALUE;
        
        for (Monster monster : monsters) {
            if (!monster.active) continue;
            float dx = monster.x - planeX;
            float dy = monster.y - planeY;
            float dist = dx * dx + dy * dy;
            if (dist < minDist) {
                minDist = dist;
                nearest = monster;
            }
        }
        
        return nearest;
    }

    private void setFullscreen() {
        // Apply current screen mode
        applyScreenMode();
    }
    
    // Add screen mode switching method
    private void applyScreenMode() {
        if (currentScreenMode == ScreenMode.FULLSCREEN) {
            // Set fullscreen
            DisplayMode displayMode = Gdx.graphics.getDisplayMode();
            Gdx.graphics.setFullscreenMode(displayMode);
        } else {
            // Set windowed mode
            int width = windowedSizes[currentWindowSizeIndex * 2];
            int height = windowedSizes[currentWindowSizeIndex * 2 + 1];
            Gdx.graphics.setWindowedMode(width, height);
        }
    }
    
    // Toggle screen mode
    private void toggleScreenMode() {
        if (currentScreenMode == ScreenMode.FULLSCREEN) {
            currentScreenMode = ScreenMode.WINDOWED;
        } else {
            currentScreenMode = ScreenMode.FULLSCREEN;
        }
        applyScreenMode();
    }
    
    // Cycle window size
    private void cycleWindowSize() {
        if (currentScreenMode == ScreenMode.WINDOWED) {
            currentWindowSizeIndex = (currentWindowSizeIndex + 1) % (windowedSizes.length / 2);
            applyScreenMode();
        }
    }

    @Override
    public void resize(int width, int height) {
        // Save current monster relative positions
        Array<Monster> oldMonsters = new Array<Monster>();
        for (Monster monster : monsters) {
            if (monster.active) {
                float relativeX = monster.x / viewport.getWorldWidth();
                float relativeY = monster.y / viewport.getWorldHeight();
                Monster m = new Monster(0, 0, monster.type);
                m.health = monster.health;
                m.shootTimer = monster.shootTimer;
                m.shootInterval = monster.shootInterval;
                m.x = relativeX;
                m.y = relativeY;
                oldMonsters.add(m);
            }
        }
        
        // Update viewport
        viewport.update(width, height, true);
        
        // Reposition monsters based on new screen size
        monsters.clear();
        for (Monster m : oldMonsters) {
            Monster newMonster = new Monster(
                m.x * viewport.getWorldWidth(),
                m.y * viewport.getWorldHeight(),
                m.type
            );
            newMonster.health = m.health;
            newMonster.shootTimer = m.shootTimer;
            newMonster.shootInterval = m.shootInterval;
            monsters.add(newMonster);
        }
    }

    private void checkCollisions() {
        if (isGameOver) return;
        
        // Update invincibility time
        if (isInvincible) {
            invincibleTimer += Gdx.graphics.getDeltaTime();
            if (invincibleTimer >= INVINCIBLE_DURATION) {
                isInvincible = false;
                invincibleTimer = 0;
            }
        }
        
        // Update plane collision rectangle
        planeRect.set(planeX, planeY, PLANE_WIDTH, PLANE_HEIGHT);
        
        // Check all bullet collisions
        for (int i = bullets.size - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            bulletRect.set(bullet.x, bullet.y, BULLET_WIDTH, BULLET_HEIGHT);
            
            if (bullet.isEnemy) {
                // Check if enemy bullets (including minions and boss) hit the player
                if (!isInvincible && planeRect.overlaps(bulletRect)) {
                    // Remove hit bullet
                    bullets.removeIndex(i);
                    
                    // Player takes damage
                    lives--;
                    if (lives <= 0) {
                        isGameOver = true;
                        gameOverTimer = 0;
                        gameState = GameState.GAME_OVER;  // Switch to game over state
                    } else {
                        isInvincible = true;
                        invincibleTimer = 0;
                        // Add hit effect
                        showHitEffect = true;
                        hitEffectTimer = 0;
                        shakeTimer = 0;
                        // Initialize shake offset
                        float angle = random.nextFloat() * 2 * MathUtils.PI;
                        shakeOffsetX = MathUtils.cos(angle) * SHAKE_INTENSITY;
                        shakeOffsetY = MathUtils.sin(angle) * SHAKE_INTENSITY;
                    }
                    
                    // Play hit sound effect
                    if (duckHitSound != null) {
                        duckHitSound.play(0.8f);
                    }
                    continue;
                }
            } else {
                // Check if player bullets hit enemies
                // Check collision with normal enemies
                for (Monster monster : monsters) {
                    if (!monster.active) continue;
                    
                    monsterRect.set(monster.x, monster.y, MONSTER_WIDTH, MONSTER_HEIGHT);
                    if (bulletRect.overlaps(monsterRect)) {
                        monster.health--;
                        bullets.removeIndex(i);
                        
                        // Play hit sound effect
                        if (duckHitSound != null) {
                            duckHitSound.play(0.8f);
                        }
                        
                        // Generate coins
                        if (monster.health <= 0) {
                            monster.active = false;
                            ducksKilled++; // Increase kill count
                            currentScore += POINTS_PER_KILL;
                            if (currentScore > highScore) {
                                highScore = currentScore;
                                prefs.putInteger(PREF_HIGH_SCORE, highScore);
                                prefs.flush();
                            }
                            
                            // Drop coins
                            Coin coin = new Coin(monster.x + MONSTER_WIDTH/2 - COIN_WIDTH/2, 
                                               monster.y + MONSTER_HEIGHT/2 - COIN_HEIGHT/2);
                            coins.add(coin);
                        }
                        break;
                    }
                }
                
                // Check collision with Boss
                if (isBossActive) {
                    Rectangle bossRect = new Rectangle(bossX, bossY, BOSS_WIDTH, BOSS_HEIGHT);
                    if (bulletRect.overlaps(bossRect)) {
                        bossHealth--;
                        bullets.removeIndex(i);
                        
                        // Play hit sound effect
                        if (duckHitSound != null) {
                            duckHitSound.play(0.8f);
                        }
                        
                        if (bossHealth <= 0) {
                            handleBossDefeated();
                        }
                    }
                }
            }
        }
        
        // Check direct collision with enemies
        if (!isInvincible) {
            for (Monster monster : monsters) {
                if (!monster.active) continue;
                
                monsterRect.set(monster.x, monster.y, MONSTER_WIDTH, MONSTER_HEIGHT);
                if (planeRect.overlaps(monsterRect)) {
                    lives--;
                    if (lives <= 0) {
                        isGameOver = true;
                        gameOverTimer = 0;
                        gameState = GameState.GAME_OVER;  // Switch to game over state
                    } else {
                        isInvincible = true;
                        invincibleTimer = 0;
                        // Add hit effect
                        showHitEffect = true;
                        hitEffectTimer = 0;
                        shakeTimer = 0;
                        // Initialize shake offset
                        float angle = random.nextFloat() * 2 * MathUtils.PI;
                        shakeOffsetX = MathUtils.cos(angle) * SHAKE_INTENSITY;
                        shakeOffsetY = MathUtils.sin(angle) * SHAKE_INTENSITY;
                    }
                    break;
                }
            }
        }
    }

    private void updateMonsters(float deltaTime) {
        // Update spawn timer
        spawnTimer += deltaTime;
        if (spawnTimer >= SPAWN_INTERVAL && !isBossActive) {
            // Spawn new enemies on the right
            float y = random.nextFloat() * (viewport.getWorldHeight() - MONSTER_HEIGHT);
            float x = viewport.getWorldWidth();
            MonsterType type = MonsterType.values()[random.nextInt(MonsterType.values().length)];
            monsters.add(new Monster(x, y, type));
            spawnTimer = 0;
        }

        // Update all enemies
        for (Monster monster : monsters) {
            if (!monster.active) continue;
            
            // Move left
            monster.x -= MONSTER_SPEED * deltaTime;
            
            // Remove if off screen
            if (monster.x + MONSTER_WIDTH < 0) {
                monster.active = false;
            }
            
            // Update shooting
            monster.shootTimer += deltaTime;
            if (monster.shootTimer >= monster.shootInterval) {
                float bulletX = monster.x;
                float bulletY = monster.y + MONSTER_HEIGHT / 2;
                bullets.add(new Bullet(bulletX, bulletY, true));
                monster.shootTimer = 0;
            }
        }

        // Check if Boss should spawn
        if (!isBossActive) {
            // Adjust Boss spawn score requirement based on difficulty
            int bossSpawnScore = 0;
            switch (currentDifficulty) {
                case EASY:
                    bossSpawnScore = 500;  // Spawn Boss earlier in easy mode
                    break;
                case NORMAL:
                    bossSpawnScore = 750;  // Medium score in normal mode
                    break;
                case HARD:
                    bossSpawnScore = 1000; // Keep original score in hard mode
                    break;
                default:
                    bossSpawnScore = 500;
                    break;
            }
            
            if (currentScore >= bossSpawnScore) {
                spawnBoss();
            }
        }

        // Update Boss
        if (isBossActive) {
            updateBoss(deltaTime);
        }
    }

    private void spawnBoss() {
        isBossActive = true;
        bossX = viewport.getWorldWidth() - BOSS_WIDTH;  // Start from right edge
        bossY = viewport.getWorldHeight() / 2 - BOSS_HEIGHT / 2;
        
        // Adjust Boss health based on difficulty
        switch (currentDifficulty) {
            case EASY:
                bossHealth = 10;  // First level needs 10 shots
                break;
            case NORMAL:
                bossHealth = 15;  // Second level needs 15 shots
                break;
            case HARD:
                bossHealth = 20;  // Third level needs 20 shots
                break;
            default:
                bossHealth = 10;
                break;
        }
        
        bossShootPattern = 0;
        bossShootTimer = 0;
        
        // Clear all bullets on screen to give player preparation time
        bullets.clear();
    }

    private void updateBoss(float deltaTime) {
        // Boss on the right moves randomly
        bossX = viewport.getWorldWidth() - BOSS_WIDTH - 100;  // Fixed X position on the right
        
        // Update target position
        bossMoveCooldown += deltaTime;
        if (bossMoveCooldown >= BOSS_MOVE_INTERVAL) {
            bossMoveCooldown = 0;
            // Randomly select new target position
            bossTargetY = random.nextFloat() * (viewport.getWorldHeight() - BOSS_HEIGHT);
        }
        
        // Move towards target position
        if (bossY < bossTargetY) {
            bossY += BOSS_SPEED * deltaTime;
        } else if (bossY > bossTargetY) {
            bossY -= BOSS_SPEED * deltaTime;
        }
        
        // Ensure Boss doesn't move out of screen
        bossY = Math.max(0, Math.min(bossY, viewport.getWorldHeight() - BOSS_HEIGHT));
        
        // Update shooting timer
        bossShootTimer += deltaTime;
        float shootInterval = currentDifficulty == Difficulty.EASY ? 1.5f : 
                             currentDifficulty == Difficulty.NORMAL ? 1.2f : 1.0f;
        
        // Shoot tracking bullets
        if (bossShootTimer >= shootInterval) {
            bossShootTimer = 0;
            
            // Calculate player position
            float targetX = planeX + PLANE_WIDTH/2;
            float targetY = planeY + PLANE_HEIGHT/2;
            
            switch(currentDifficulty) {
                case EASY:
                    // Easy mode: 2 types of trajectories
                    shootTrackingBullet(bossX, bossY + BOSS_HEIGHT/2, targetX, targetY, 300);  // Slow tracking
                    shootDirectBullet(bossX, bossY + BOSS_HEIGHT/2, -400, 0);  // Straight line fast
                    break;
                    
                case NORMAL:
                    // Normal mode: 3 types of trajectories
                    shootTrackingBullet(bossX, bossY + BOSS_HEIGHT/3, targetX, targetY, 350);  // Upper tracking
                    shootTrackingBullet(bossX, bossY + BOSS_HEIGHT*2/3, targetX, targetY, 350);  // Lower tracking
                    shootDirectBullet(bossX, bossY + BOSS_HEIGHT/2, -450, 0);  // Middle straight line
                    break;
                    
                case HARD:
                    // Hard mode: 4 types of trajectories
                    shootTrackingBullet(bossX, bossY + BOSS_HEIGHT/4, targetX, targetY, 400);  // Fast tracking
                    shootTrackingBullet(bossX, bossY + BOSS_HEIGHT*3/4, targetX, targetY, 400);  // Fast tracking
                    shootDirectBullet(bossX, bossY + BOSS_HEIGHT/3, -500, 100);  // Upward diagonal shot
                    shootDirectBullet(bossX, bossY + BOSS_HEIGHT*2/3, -500, -100);  // Downward diagonal shot
                    break;
            }
            
            // Play shooting sound effect
            if (waterShootSound != null) {
                waterShootSound.play(0.5f);
            }
        }
    }

    // Shoot tracking bullets
    private void shootTrackingBullet(float startX, float startY, float targetX, float targetY, float speed) {
        // Calculate direction vector
        float dx = targetX - startX;
        float dy = targetY - startY;
        float length = (float)Math.sqrt(dx * dx + dy * dy);
        
        // Normalize and set speed
        float vx = dx / length * speed;
        float vy = dy / length * speed;
        
        Bullet bullet = new Bullet(startX, startY, true);
        bullet.velocityX = vx;
        bullet.velocityY = vy;
        bullets.add(bullet);
    }

    // Shoot straight bullets
    private void shootDirectBullet(float startX, float startY, float vx, float vy) {
        Bullet bullet = new Bullet(startX, startY, true);
        bullet.velocityX = vx;
        bullet.velocityY = vy;
        bullets.add(bullet);
    }

    @Override
    public void render() {
        // Limit frame rate to 60FPS
        float targetFPS = 60f;
        float targetFrameTime = 1f / targetFPS;
        float deltaTime = Math.min(Gdx.graphics.getDeltaTime(), targetFrameTime);
        gameTime += deltaTime;
        
        // Clear screen
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        // Ensure blend mode is reset at the start of each frame
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        
        // Update stars
        updateStars(deltaTime);
        
        // Render background in all states
        renderBackground();
        
        // Update game music
        updateGameMusic();
        
        switch (gameState) {
            case MENU:
                handleMenuInput();
                renderMenu();
                break;
                
            case SHOP:
                handleShopInput();
                renderShop();
                break;
                
            case PLAYING:
                if (!isGameOver) {
                    handleInput();
                    updateBullets(deltaTime);
                    updateMonsters(deltaTime);
                    updateCoins(deltaTime);
                    checkCollisions();
                }
                renderGame();
                break;
                
            case GAME_OVER:
                    gameOverTimer += deltaTime;
                renderGameOver();
                // Press space key to return to main menu
                if (gameOverTimer >= RESTART_DELAY && 
                    Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE)) {
                        gameState = GameState.MENU;
                    resetGame();
                    }
                break;
                
            case PAUSED:
                handleInput();
                renderGame();
                renderPause();
                break;

            case GAME_COMPLETE:
                completeTimer += deltaTime;
                renderGameComplete();
                if (completeTimer >= COMPLETE_DISPLAY_TIME) {
                    gameState = GameState.MENU;
                    currentDifficulty = Difficulty.EASY;
                }
                break;

            case IN_GAME_MENU:
                handleInGameMenuInput();
                renderGame();
                renderInGameMenu();
                break;

            case LEVEL_COMPLETE:
                renderLevelComplete();
                handleLevelCompleteInput();
                break;

            case GUN_SELECT:
                renderGunSelect();
                handleGunSelect();
                break;
        }
    }

    private void handleMenuInput() {
        // Handle F11 key to toggle fullscreen/window mode
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F11)) {
            toggleScreenMode();
            return;
        }
        
        // Handle F10 key to cycle window size
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F10) && 
            currentScreenMode == ScreenMode.WINDOWED) {
            cycleWindowSize();
            return;
        }
        
        if (Gdx.input.justTouched()) {
            Vector2 touch = new Vector2(Gdx.input.getX(), Gdx.input.getY());
            Vector3 worldCoords = viewport.unproject(new Vector3(touch.x, touch.y, 0));
            
            // Handle shop button click
            if (shopButton.contains(worldCoords.x, worldCoords.y)) {
                gameState = GameState.SHOP;
                return;
            }
            
            if (startGameButton.contains(worldCoords.x, worldCoords.y)) {
                gameState = GameState.GUN_SELECT;  // Enter selection interface instead of shop
            } else if (difficultyButton.contains(worldCoords.x, worldCoords.y)) {
                // Cycle through difficulty
                switch (currentDifficulty) {
                    case EASY:
                        currentDifficultyText = "NORMAL";
                        currentDifficulty = Difficulty.NORMAL;
                        break;
                    case NORMAL:
                        currentDifficultyText = "HARD";
                        currentDifficulty = Difficulty.HARD;
                        break;
                    case HARD:
                        currentDifficultyText = "EASY";
                        currentDifficulty = Difficulty.EASY;
                        break;
                    default:
                        break;
                }
                applyDifficultySettings();  // Add this line to apply new difficulty settings
            } else if (quitButton.contains(worldCoords.x, worldCoords.y)) {
                Gdx.app.exit();
            }
        }
    }

    private void renderMenu() {
        batch.begin();
        // Draw background
        batch.draw(menuBackground, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        
        // Use titleFont to draw game title
        titleFont.setColor(Color.BLACK);
        String title = "DuckSplash!";
        layout.setText(titleFont, title);
        float titleX = (viewport.getWorldWidth() - layout.width) / 2;
        float titleY = viewport.getWorldHeight() * 0.8f;
        titleFont.draw(batch, title, titleX, titleY);
        
        // Draw shop button
        batch.setColor(0.3f, 0.3f, 0.7f, 0.7f);
        batch.draw(bulletTexture, shopButton.x, shopButton.y, shopButton.width, shopButton.height);
        batch.setColor(1, 1, 1, 1);
        font.setColor(Color.BLACK);
        String shopText = "Shop";
        layout.setText(font, shopText);
        font.draw(batch, shopText,
                 shopButton.x + (shopButton.width - layout.width) / 2,
                 shopButton.y + (shopButton.height + layout.height) / 2);
        
        // Draw coins number
        String coinsText = "Coins: " + totalCoins;
        layout.setText(font, coinsText);
        float coinsX = viewport.getWorldWidth() - layout.width - 20;
        float coinsY = viewport.getWorldHeight() - 20;
        font.draw(batch, coinsText, coinsX, coinsY);
        
        // Use normal font to draw menu options
        font.setColor(Color.BLACK);  // Change to black
        String[] menuItems = {"Start Game", "Difficulty: " + currentDifficultyText, "Exit"};
        
        // Draw button text (no background)
        // Start game button
        layout.setText(font, menuItems[0]);
        font.draw(batch, menuItems[0], 
                 startGameButton.x + (startGameButton.width - layout.width) / 2,
                 startGameButton.y + (startGameButton.height + layout.height) / 2);
        
        // Difficulty selection button
        layout.setText(font, menuItems[1]);
        font.draw(batch, menuItems[1], 
                 difficultyButton.x + (difficultyButton.width - layout.width) / 2,
                 difficultyButton.y + (difficultyButton.height + layout.height) / 2);
        
        // Exit button
        layout.setText(font, menuItems[2]);
        font.draw(batch, menuItems[2], 
                 quitButton.x + (quitButton.width - layout.width) / 2,
                 quitButton.y + (quitButton.height + layout.height) / 2);
        
        // Add screen mode hint
        font.getData().setScale(1.0f);
        String screenModeText = "Press F11 to toggle fullscreen";
        layout.setText(font, screenModeText);
        float screenModeX = 20;
        float screenModeY = 40;
        font.draw(batch, screenModeText, screenModeX, screenModeY);
        
        if (currentScreenMode == ScreenMode.WINDOWED) {
            String windowSizeText = "Press F10 to cycle window size: " + 
                windowedSizes[currentWindowSizeIndex * 2] + "x" + 
                windowedSizes[currentWindowSizeIndex * 2 + 1];
            layout.setText(font, windowSizeText);
            font.draw(batch, windowSizeText, screenModeX, screenModeY + 30);
        }
        
        batch.end();
    }

    private void drawButton(SpriteBatch batch, String text, Rectangle button, boolean selected) {
        // Draw button text
        font.setColor(selected ? 0f : 1f, selected ? 1f : 1f, selected ? 0f : 1f, 1f);
        layout.setText(font, text);
        font.draw(batch, text, 
                 button.x + button.width / 2 - layout.width / 2,
                 button.y + button.height / 2 + layout.height / 2);
    }

    private void renderGame() {
        batch.begin();
        // Reset blend mode
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        // Draw stars
        for (Star star : stars) {
            batch.setColor(star.brightness, star.brightness, star.brightness, 1);
            batch.draw(bulletTexture, star.x, star.y, star.size, star.size);
        }
        
        // Reset color to white
        batch.setColor(1, 1, 1, 1);

        // Draw coins
        for (Coin coin : coins) {
            if (coin.active) {
                batch.draw(coinTexture, coin.x, coin.y, COIN_WIDTH, COIN_HEIGHT);
            }
        }
        
        // Draw enemies
        for (Monster monster : monsters) {
            if (monster.active) {
                // Set transparency based on health
                float alpha = monster.health / 3.0f;  // Lower health means more transparent
                batch.setColor(1, 1, 1, alpha);  // Use original color, only change transparency
                batch.draw(monsterTexture, monster.x, monster.y, MONSTER_WIDTH, MONSTER_HEIGHT);
            }
        }
        
        // Draw Boss
        if (isBossActive) {
            batch.setColor(1, 1, 1, 1);
            // Choose different Boss images based on difficulty
            Texture currentBossTexture = currentDifficulty == Difficulty.EASY ? boss1Texture :
                                       currentDifficulty == Difficulty.NORMAL ? boss2Texture :
                                       boss3Texture;
            batch.draw(currentBossTexture, bossX, bossY, BOSS_WIDTH, BOSS_HEIGHT);
            
            // Draw Boss health bar
            float healthBarWidth = BOSS_WIDTH;
            float healthBarHeight = 10;
            float healthPercentage = bossHealth / (float)(currentDifficulty == Difficulty.EASY ? 10 : 
                                                         currentDifficulty == Difficulty.NORMAL ? 15 : 20);
            
            // Blood bar background (gray)
            batch.setColor(0.3f, 0.3f, 0.3f, 1f);
            batch.draw(bulletTexture, bossX, bossY + BOSS_HEIGHT + 10, 
                      healthBarWidth, healthBarHeight);
            
            // Current health (white)
            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(bulletTexture, bossX, bossY + BOSS_HEIGHT + 10, 
                      healthBarWidth * healthPercentage, healthBarHeight);
            
            // Show Boss health text (white)
            font.setColor(1, 1, 1, 1);
            font.draw(batch, "Boss HP: " + bossHealth, bossX, bossY + BOSS_HEIGHT + 30);
        }
        
        // Draw bullets
        for (Bullet bullet : bullets) {
            if (bullet.isEnemy) {
                if (isBossActive) {
                    batch.setColor(1f, 0.5f, 0f, 1f);  // Boss bullet is orange
                    batch.draw(bullet2Texture, bullet.x, bullet.y, 
                             BOSS_BULLET_HEIGHT/2, BOSS_BULLET_WIDTH/2,  // Swap width and height center points
                             BOSS_BULLET_HEIGHT, BOSS_BULLET_WIDTH,      // Swap width and height
                             1, 1,
                             90,
                             0, 0,
                             bullet2Texture.getWidth(), bullet2Texture.getHeight(),
                             false, false);
                } else {
                    batch.setColor(1f, 1f, 1f, 1f);  // Enemy bullet uses original color
                    batch.draw(bullet2Texture, bullet.x, bullet.y, 
                             BULLET_WIDTH/2, BULLET_HEIGHT/2,  // Swap width and height center points
                             BULLET_WIDTH, BULLET_HEIGHT,      // Swap width and height
                             1, 1,
                             90,
                             0, 0,
                             bullet2Texture.getWidth(), bullet2Texture.getHeight(),
                             false, false);
                }
            } else {
                batch.setColor(1f, 1f, 1f, 1f);  // Player bullet uses original color
                batch.draw(bulletTexture, bullet.x, bullet.y, 
                         BULLET_WIDTH/2, BULLET_HEIGHT/2,  // Swap width and height center points
                         BULLET_WIDTH, BULLET_HEIGHT,      // Swap width and height
                         1, 1,
                         90,
                         0, 0,
                         bulletTexture.getWidth(), bulletTexture.getHeight(),
                         false, false);
            }
        }
        
        if (!isGameOver) {
            // Update hit effect
            if (showHitEffect) {
                hitEffectTimer += Gdx.graphics.getDeltaTime();
                if (hitEffectTimer >= HIT_EFFECT_DURATION) {
                    showHitEffect = false;
                    hitEffectTimer = 0;
                }
            }
            
            // Update shake effect
            if (shakeTimer < SHAKE_DURATION) {
                shakeTimer += Gdx.graphics.getDeltaTime();
                if (shakeTimer >= SHAKE_DURATION) {
                    shakeOffsetX = 0;
                    shakeOffsetY = 0;
                } else {
                    float progress = shakeTimer / SHAKE_DURATION;
                    float damping = 1.0f - progress; // Gradually reduce shake intensity
                    float angle = random.nextFloat() * 2 * MathUtils.PI;
                    shakeOffsetX = MathUtils.cos(angle) * SHAKE_INTENSITY * damping;
                    shakeOffsetY = MathUtils.sin(angle) * SHAKE_INTENSITY * damping;
                }
            }
            
            // Draw plane - in invincible state transparency changes
            float alpha = 1.0f;
            if (isInvincible) {
                // Lower flashing frequency, use smoother transition
                alpha = 0.5f + (float)Math.abs(Math.sin(invincibleTimer * 2)) * 0.5f;
            }
            
            // Apply hit effect color
            if (showHitEffect) {
                float hitProgress = hitEffectTimer / HIT_EFFECT_DURATION;
                float flash = 1.0f - hitProgress; // Flash intensity decreases over time
                batch.setColor(1, 1 - flash * 0.5f, 1 - flash * 0.5f, alpha); // Red tint
            } else {
                batch.setColor(1, 1, 1, alpha);
            }
            
            // Draw with shake offset
            batch.draw(planeTexture, 
                      planeX + shakeOffsetX, 
                      planeY + shakeOffsetY, 
                      PLANE_WIDTH, PLANE_HEIGHT);
        } else {
            // Use smoother fade out effect
            float fadeOut = 1.0f - (gameOverTimer / RESTART_DELAY);
            fadeOut = Math.max(0, Math.min(1, fadeOut)); // Limit between 0 and 1
            
            batch.setColor(1, 1, 1, fadeOut);
            
            // Calculate explosion animation position (centered on plane position)
            float explosionX = planeX + PLANE_WIDTH/2 - DEAD_WIDTH/2;
            float explosionY = planeY + PLANE_HEIGHT/2 - DEAD_HEIGHT/2;
            
            // Draw death animation, use smoother scaling effect
            float scale = 1.0f + gameOverTimer * 0.3f; // Lower scaling speed
            float width = DEAD_WIDTH * scale;
            float height = DEAD_HEIGHT * scale;
            
            // Keep center point不变
            explosionX = explosionX - (width - DEAD_WIDTH)/2;
            explosionY = explosionY - (height - DEAD_HEIGHT)/2;
            
            batch.draw(deadRegion, explosionX, explosionY, width, height);
            batch.setColor(1, 1, 1, 1);
        }
        
        // Draw score and coins
        batch.setColor(1, 1, 1, 1);
        
        // Draw kill count and icon - top left corner
        batch.draw(monster2Texture, 
                  20, 
                  viewport.getWorldHeight() - 40,
                  30, 30);
        
        font.draw(batch, "x " + ducksKilled, 
                 60, 
                 viewport.getWorldHeight() - 20);
        
        // Draw coin icon and number - below kill count
        batch.draw(coinTexture, 
                  20, 
                  viewport.getWorldHeight() - 80, 
                  30, 
                  30);
        
        font.draw(batch, "x " + currentLevelCoins, 
                 60, 
                 viewport.getWorldHeight() - 60);
        
        // Draw lives icon - below coins
        float livesIconSize = 30;
        float livesY = viewport.getWorldHeight() - 120; // Position below coins
        
        // Draw lives icon - from left to right
        for (int i = 0; i < lives; i++) {
            batch.draw(livesTexture, 
                      20 + (i * (livesIconSize + 5)), 
                      livesY, 
                      livesIconSize, 
                      livesIconSize);
        }
        
        batch.end();
    }

    private void renderGameComplete() {
        batch.begin();
        // Draw background
        batch.draw(backgroundTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        
        // Draw通关信息
        font.setColor(1, 1, 1, 1);  // Use white
        font.getData().setScale(4);  // Set larger font size
        
        String completeText = "GAME COMPLETE!";
        layout.setText(font, completeText);
        float completeX = (viewport.getWorldWidth() - layout.width) / 2;
        float completeY = viewport.getWorldHeight() * 0.7f;
        font.draw(batch, completeText, completeX, completeY);
        
        // Set smaller font size to display kill count and coin count
        font.getData().setScale(2);
        
        // Draw duck icon and kill count
        batch.draw(monster2Texture, 
                  viewport.getWorldWidth() / 2 - 100,  // Center left
                  viewport.getWorldHeight() * 0.5f - 15,  // Vertical position adjustment
                  30, 30);  // Icon size
        
        String ducksText = "x " + ducksKilled;
        layout.setText(font, ducksText);
        font.draw(batch, ducksText,
                 viewport.getWorldWidth() / 2 - 60,  // Right of icon
                 viewport.getWorldHeight() * 0.5f + 10);
        
        // Draw coin icon and number
        batch.draw(coinTexture, 
                  viewport.getWorldWidth() / 2 - 100,  // Center left
                  viewport.getWorldHeight() * 0.4f - 15,  // Vertical position adjustment
                  30, 30);  // Icon size
        
        String coinsText = "x " + currentLevelCoins;
        layout.setText(font, coinsText);
        font.draw(batch, coinsText,
                 viewport.getWorldWidth() / 2 - 60,  // Right of icon
                 viewport.getWorldHeight() * 0.4f + 10);
        
        // Add tip information
        String tipText = "PRESS ESC TO RETURN";
        layout.setText(font, tipText);
        float tipX = (viewport.getWorldWidth() - layout.width) / 2;
        float tipY = viewport.getWorldHeight() * 0.2f;
        font.draw(batch, tipText, tipX, tipY);
        
        font.getData().setScale(1);  // Restore default font size
        batch.end();
    }

    private void applyDifficultySettings() {
        DifficultySettings settings;
        switch (currentDifficulty) {
            case EASY:
                settings = easySettings;
                break;
            case HARD:
                settings = hardSettings;
                break;
            default:
                settings = normalSettings;
                break;
        }
        
        MONSTER_SPEED = settings.duckSpeed;
        MONSTER_VERTICAL_SPEED = settings.duckVerticalSpeed;
        MONSTER_COUNT = settings.duckCount;
        SHOOT_INTERVAL = settings.shootInterval;
        
        // Force re-render background
        renderBackground();
    }

    private void handleInput() {
        float deltaTime = Gdx.graphics.getDeltaTime();
        
        // Handle F11 key to toggle fullscreen/window mode
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F11)) {
            toggleScreenMode();
            return;
        }
        
        // Handle F10 key to cycle window size
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F10) && 
            currentScreenMode == ScreenMode.WINDOWED) {
            cycleWindowSize();
            return;
        }
        
        // Press ESC to open in-game menu
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            if (gameState == GameState.PLAYING) {
                gameState = GameState.IN_GAME_MENU;
            } else if (gameState == GameState.IN_GAME_MENU) {
                gameState = GameState.PLAYING;
            }
            return;
        }
        
        // Press P key to pause game
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.P)) {
            if (gameState == GameState.PLAYING) {
                gameState = GameState.PAUSED;
            } else if (gameState == GameState.PAUSED) {
                gameState = GameState.PLAYING;
            }
            return;
        }
        
        // 自由移动
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.UP)) {
            planeY += PLANE_SPEED * deltaTime;
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.DOWN)) {
            planeY -= PLANE_SPEED * deltaTime;
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.LEFT)) {
            planeX -= PLANE_SPEED * deltaTime;
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.RIGHT)) {
            planeX += PLANE_SPEED * deltaTime;
        }
        
        // Ensure plane doesn't move out of screen range
        planeX = Math.max(0, Math.min(planeX, viewport.getWorldWidth() - PLANE_WIDTH));
        planeY = Math.max(0, Math.min(planeY, viewport.getWorldHeight() - PLANE_HEIGHT));
    }

    private void updateStars(float deltaTime) {
        if (stars == null) {
            stars = new Array<Star>();
            for (int i = 0; i < STAR_COUNT; i++) {
                stars.add(new Star());
            }
        }
        
        // let some stars flash
        for (Star star : stars) {
            star.brightness = Math.max(0.5f, Math.min(1f, 
                star.brightness + (random.nextFloat() - 0.5f) * deltaTime));
        }
    }

    private void renderPause() {
        batch.begin();
        // Draw semi-transparent black background
        batch.setColor(0, 0, 0, 0.5f);
        batch.draw(bulletTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        
        // Draw pause text
        font.setColor(1, 1, 1, 1);
        font.getData().setScale(4);
        
        String pauseText = "GAME PAUSED";
        layout.setText(font, pauseText);
        float pauseX = (viewport.getWorldWidth() - layout.width) / 2;
        float pauseY = viewport.getWorldHeight() * 0.6f;
        font.draw(batch, pauseText, pauseX, pauseY);
        
        // Draw tip text
        font.getData().setScale(2);
        String tipText = "PRESS P TO CONTINUE";
        layout.setText(font, tipText);
        float tipX = (viewport.getWorldWidth() - layout.width) / 2;
        float tipY = viewport.getWorldHeight() * 0.4f;
        font.draw(batch, tipText, tipX, tipY);
        
        font.getData().setScale(1);
        batch.setColor(1, 1, 1, 1);
        batch.end();
    }

    private void handleInGameMenuInput() {
        if (Gdx.input.justTouched()) {
            Vector2 touch = new Vector2(Gdx.input.getX(), Gdx.input.getY());
            Vector3 worldCoords = viewport.unproject(new Vector3(touch.x, touch.y, 0));
            
            if (continueButton.contains(worldCoords.x, worldCoords.y)) {
                gameState = GameState.PLAYING;
            } else if (backToMainButton.contains(worldCoords.x, worldCoords.y)) {
                gameState = GameState.MENU;
                resetGame();
            } else if (inGameQuitButton.contains(worldCoords.x, worldCoords.y)) {
                Gdx.app.exit();
            }
        }
    }

    private void renderInGameMenu() {
        batch.begin();
        // Draw semi-transparent black background
        batch.setColor(0, 0, 0, 0.8f);
        batch.draw(bulletTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        
        // Draw menu title
        batch.setColor(1, 1, 1, 1);
        font.getData().setScale(4);
        String menuText = "GAME MENU";
        layout.setText(font, menuText);
        float menuX = (viewport.getWorldWidth() - layout.width) / 2;
        float menuY = viewport.getWorldHeight() * 0.8f;
        font.draw(batch, menuText, menuX, menuY);
        
        // Draw button text (no background)
        font.getData().setScale(3);
        font.setColor(Color.WHITE);
        
        // Continue game button
        layout.setText(font, "Continue");
        font.draw(batch, "Continue", 
                 continueButton.x + (continueButton.width - layout.width) / 2,
                 continueButton.y + (continueButton.height + layout.height) / 2);
        
        // Back to main menu button
        layout.setText(font, "Main Menu");
        font.draw(batch, "Main Menu", 
                 backToMainButton.x + (backToMainButton.width - layout.width) / 2,
                 backToMainButton.y + (backToMainButton.height + layout.height) / 2);
        
        // Quit game button
        layout.setText(font, "Quit Game");
        font.draw(batch, "Quit Game", 
                 inGameQuitButton.x + (inGameQuitButton.width - layout.width) / 2,
                 inGameQuitButton.y + (inGameQuitButton.height + layout.height) / 2);
        
        font.getData().setScale(1);
        batch.setColor(1, 1, 1, 1);
        batch.end();
    }

    private void resetForNextLevel() {
        // Clear existing enemies and bullets
        monsters.clear();
        bullets.clear();
        
        // Clear all uncollected coins
        coins.clear();
        
        // Reset boss-related states
        isBossActive = false;
        bossHealth = 100;
        bossShootTimer = 0;
        bossShootPattern = 0;
        
        // Reset player position
        planeX = PLANE_WIDTH;
        planeY = MIN_WORLD_HEIGHT / 2f - PLANE_HEIGHT / 2f;
        
        // Reset lives
        lives = 3;  // Reset lives for each level
        isInvincible = false;
        invincibleTimer = 0;
        
        // Reset current score to prevent boss appearing immediately
        currentScore = 0;
        
        // Spawn new enemies
        spawnMonsters();
        
        // Force update background
        renderBackground();
        
        // Note: Don't reset ducksKilled and currentLevelCoins, let them accumulate
        // Game state related
        isGameOver = false;
        gameOverTimer = 0;
        monstersMovingRight = true;
    }

    private void initializeDifficulties() {
        // Easy mode: ducks move slow, fewer number
        easySettings = new DifficultySettings(
            100f,  // slower movement speed
            50f,   // slower vertical speed
            4,     // fewer ducks
            0.6f,  // longer shooting interval
            1.0f   // standard water gun power
        );
        
        // Normal mode: balanced difficulty
        normalSettings = new DifficultySettings(
            150f,  // medium movement speed
            100f,  // medium vertical speed
            6,     // medium number of ducks
            0.4f,  // medium shooting interval
            1.2f   // slightly stronger water gun power
        );
        
        // Hard mode: ducks move fast, more number
        hardSettings = new DifficultySettings(
            200f,  // faster movement speed
            150f,  // faster vertical speed
            8,     // more number of ducks
            0.3f,  // shorter shooting interval
            1.5f   // stronger water gun power
        );
    }

    private void loadAudio() {
        try {
            // Main menu music: lighthearted water park style
            menuMusic = Gdx.audio.newMusic(Gdx.files.internal("menu_music.wav"));
            menuMusic.setLooping(true);
            menuMusic.setVolume(1.0f);
            
            // Game background music: lively summer theme
            gameMusic = Gdx.audio.newMusic(Gdx.files.internal("game_music.mp3"));
            gameMusic.setLooping(true);
            gameMusic.setVolume(0.5f);
            
            // Boss battle music: tense and exciting music
            bossMusic = Gdx.audio.newMusic(Gdx.files.internal("boss_music.mp3"));
            bossMusic.setLooping(true);
            bossMusic.setVolume(0.5f);
            
            // Load sound effects
            waterShootSound = Gdx.audio.newSound(Gdx.files.internal("water_shoot.mp3"));
            duckHitSound = Gdx.audio.newSound(Gdx.files.internal("duck_hit.mp3"));
            powerupSound = Gdx.audio.newSound(Gdx.files.internal("powerup.mp3"));
            
            // Test if sound effects load successfully
            if (waterShootSound != null) {
                System.out.println("Water gun shooting sound effect loaded successfully");
            }
            if (duckHitSound != null) {
                System.out.println("Duck hit sound effect loaded successfully");
            }
            if (powerupSound != null) {
                System.out.println("Powerup sound effect loaded successfully");
            }
            
        } catch (Exception e) {
            System.err.println("Audio loading error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadTextures() {
        try {
            System.out.println("Loading textures...");
            
            // Load and set background texture
            backgroundTexture = new Texture(Gdx.files.internal("background.png"));
            backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            System.out.println("Loaded background.png");
            
            // Load water gun textures
            waterGunTextures[0] = new Texture(Gdx.files.internal("plane.png"));
            waterGunTextures[1] = new Texture(Gdx.files.internal("water_gun2.png"));
            waterGunTextures[2] = new Texture(Gdx.files.internal("water_gun3.png"));
            waterGunTextures[3] = new Texture(Gdx.files.internal("water_gun4.png"));
            System.out.println("Loaded water gun textures");
            
            // Set current used water gun texture
            planeTexture = waterGunTextures[currentGun.getId()];
            
            // Load coin texture
            coinTexture = new Texture(Gdx.files.internal("coin.png"));
            System.out.println("Loaded coin.png");
            
            // Load and set monster texture (duck)
            monsterTexture = new Texture(Gdx.files.internal("monster.png"));
            monsterTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            System.out.println("Loaded monster.png");
            
            // Set dead texture
            deadTexture = new Texture(Gdx.files.internal("dead.png"));
            deadTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            deadRegion = new TextureRegion(deadTexture);
            System.out.println("Loaded dead.png");
            
            // Load bullet texture
            bulletTexture = new Texture(Gdx.files.internal("bullet.png"));
            bulletTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            System.out.println("Loaded bullet.png");
            
            bullet2Texture = new Texture(Gdx.files.internal("bullet2.png"));
            bullet2Texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            System.out.println("Loaded bullet2.png");
            
            // Load Boss related textures
            bossTexture = new Texture(Gdx.files.internal("boss.png"));
            bossShootTexture = new Texture(Gdx.files.internal("boss_shoot.png"));
            bossTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            bossShootTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            System.out.println("Loaded Boss related textures");
            
            // Load three Boss textures
            boss1Texture = new Texture(Gdx.files.internal("boss.png"));
            boss2Texture = new Texture(Gdx.files.internal("boss2.png"));
            boss3Texture = new Texture(Gdx.files.internal("boss3.png"));
            System.out.println("All textures loaded successfully");
            
            // Load monster2 texture
            monster2Texture = new Texture(Gdx.files.internal("monster2.png"));
            monster2Texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            System.out.println("Loaded monster2.png");
            
            // Load lives texture
            livesTexture = new Texture(Gdx.files.internal("Lives.png"));
            livesTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            System.out.println("Loaded Lives.png");
            
        } catch (Exception e) {
            System.err.println("Error loading textures: " + e.getMessage());
            e.printStackTrace();
            // Create a default white texture as a fallback
            bulletTexture = new Texture(1, 1, Pixmap.Format.RGBA8888);
            bullet2Texture = new Texture(1, 1, Pixmap.Format.RGBA8888);
        }
    }

    private void renderBackground() {
        try {
            batch.begin();
            switch (gameState) {
                case MENU:
                    if (menuBackground != null) {
                        batch.draw(menuBackground, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
                    }
                    break;
                case PLAYING:
                case PAUSED:
                case IN_GAME_MENU:
                    if (isBossActive && bossBackground != null) {
                        batch.draw(bossBackground, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
                    } else {
                        Texture currentBackground = null;
                        switch (currentDifficulty) {
                            case EASY:
                                currentBackground = level1Background;
                                break;
                            case NORMAL:
                                currentBackground = level2Background;
                                break;
                            case HARD:
                                currentBackground = level3Background;
                                break;
                            default:
                                currentBackground = level1Background;
                                break;
                        }
                        if (currentBackground != null) {
                            batch.draw(currentBackground, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
                        }
                    }
                    break;
                case GAME_COMPLETE:
                    if (menuBackground != null) {
                        batch.draw(menuBackground, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
                    }
                    break;
            }
            batch.end();
        } catch (Exception e) {
            System.err.println("Error rendering background: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateGameMusic() {
        switch (gameState) {
            case MENU:
                if (!menuMusic.isPlaying()) {
                    gameMusic.stop();
                    bossMusic.stop();
                    menuMusic.play();
                }
                break;
            case PLAYING:
                if (isBossActive) {
                    if (!bossMusic.isPlaying()) {
                        menuMusic.stop();
                        gameMusic.stop();
                        bossMusic.play();
                    }
                } else {
                    if (!gameMusic.isPlaying()) {
                        menuMusic.stop();
                        bossMusic.stop();
                        gameMusic.play();
                    }
                }
                break;
        }
    }

    private void initializeButtons() {
        float centerX = MIN_WORLD_WIDTH / 2f - BUTTON_WIDTH / 2f;
        float startY = MIN_WORLD_HEIGHT / 2f;
        
        // Main menu buttons
        startGameButton = new Rectangle(centerX, startY + 60, BUTTON_WIDTH, BUTTON_HEIGHT);
        difficultyButton = new Rectangle(centerX, startY, BUTTON_WIDTH, BUTTON_HEIGHT);
        quitButton = new Rectangle(centerX, startY - 60, BUTTON_WIDTH, BUTTON_HEIGHT);
        
        // In-game menu buttons
        continueButton = new Rectangle(centerX, startY + 60, BUTTON_WIDTH, BUTTON_HEIGHT);
        backToMainButton = new Rectangle(centerX, startY, BUTTON_WIDTH, BUTTON_HEIGHT);
        inGameQuitButton = new Rectangle(centerX, startY - 60, BUTTON_WIDTH, BUTTON_HEIGHT);
        
        // Level complete interface buttons
        retryButton = new Rectangle(centerX, startY + 60, BUTTON_WIDTH, BUTTON_HEIGHT);
        nextLevelButton = new Rectangle(centerX, startY, BUTTON_WIDTH, BUTTON_HEIGHT);
        exitToMenuButton = new Rectangle(centerX, startY - 60, BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    private void renderLevelComplete() {
        batch.begin();
        // Draw current background
        switch (currentDifficulty) {
            case EASY:
                batch.draw(level1Background, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
                break;
            case NORMAL:
                batch.draw(level2Background, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
                break;
            case HARD:
                batch.draw(level3Background, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
                break;
        }
        
        // Draw semi-transparent black mask
        batch.setColor(0, 0, 0, 0.7f);
        batch.draw(bulletTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        batch.setColor(1, 1, 1, 1);
        
        // Draw title
        font.getData().setScale(4);
        // Display correct level completion information based on current difficulty
        String levelText = "LEVEL " + (currentDifficulty == Difficulty.EASY ? "EASY" : 
                                     currentDifficulty == Difficulty.NORMAL ? "NORMAL" : "HARD") + " COMPLETE!";
        layout.setText(font, levelText);
        float levelX = (viewport.getWorldWidth() - layout.width) / 2;
        float levelY = viewport.getWorldHeight() * 0.8f;
        font.draw(batch, levelText, levelX, levelY);
        
       
        
        // Draw buttons
        font.getData().setScale(3);
        
        // Retry button
        batch.setColor(0.3f, 0.3f, 0.7f, 0.7f);
        batch.draw(bulletTexture, retryButton.x, retryButton.y, retryButton.width, retryButton.height);
        batch.setColor(1, 1, 1, 1);
        layout.setText(font, "Retry Level");
        font.draw(batch, "Retry Level", 
                 retryButton.x + (retryButton.width - layout.width) / 2,
                 retryButton.y + (retryButton.height + layout.height) / 2);
        
        // Next level button - only show on non-last level
        if (currentDifficulty != Difficulty.HARD) {
            batch.setColor(0.3f, 0.3f, 0.7f, 0.7f);
            batch.draw(bulletTexture, nextLevelButton.x, nextLevelButton.y, nextLevelButton.width, nextLevelButton.height);
            batch.setColor(1, 1, 1, 1);
            layout.setText(font, "Next Level");
            font.draw(batch, "Next Level", 
                     nextLevelButton.x + (nextLevelButton.width - layout.width) / 2,
                     nextLevelButton.y + (nextLevelButton.height + layout.height) / 2);
        }
        
        // Back to main menu button
        batch.setColor(0.3f, 0.3f, 0.7f, 0.7f);
        batch.draw(bulletTexture, exitToMenuButton.x, exitToMenuButton.y, exitToMenuButton.width, exitToMenuButton.height);
        batch.setColor(1, 1, 1, 1);
        layout.setText(font, "Back to Menu");
        font.draw(batch, "Back to Menu", 
                 exitToMenuButton.x + (exitToMenuButton.width - layout.width) / 2,
                 exitToMenuButton.y + (exitToMenuButton.height + layout.height) / 2);
        
        font.getData().setScale(1);
        batch.end();
    }

    private void handleLevelCompleteInput() {
        if (Gdx.input.justTouched()) {
            Vector2 touch = new Vector2(Gdx.input.getX(), Gdx.input.getY());
            Vector3 worldCoords = viewport.unproject(new Vector3(touch.x, touch.y, 0));
            
            if (retryButton.contains(worldCoords.x, worldCoords.y)) {
                // Restart current difficulty
                resetGame();
                gameState = GameState.PLAYING;
            } else if (nextLevelButton.contains(worldCoords.x, worldCoords.y) && 
                       currentDifficulty != Difficulty.HARD) {
                // Enter next difficulty
                switch (currentDifficulty) {
                    case EASY:
                        currentDifficulty = Difficulty.NORMAL;
                        currentDifficultyText = "NORMAL";
                        break;
                    case NORMAL:
                        currentDifficulty = Difficulty.HARD;
                        currentDifficultyText = "HARD";
                        break;
                }
                applyDifficultySettings();
                resetForNextLevel();  // Use resetForNextLevel instead of resetGame
                gameState = GameState.PLAYING;
            } else if (exitToMenuButton.contains(worldCoords.x, worldCoords.y)) {
                // Back to main menu
                gameState = GameState.MENU;
                currentDifficulty = Difficulty.EASY;
                currentDifficultyText = "EASY";
                resetGame();
            }
        }
    }

    private void handleBossDefeated() {
        isBossActive = false;
        
        // Add reward score
        currentScore += 500;  // Boss defeated reward 500 points
        
        // Add life reward
        if (lives < MAX_LIVES) {
            lives++;
        }
        
        // Play victory sound
        if (powerupSound != null) {
            powerupSound.play(1.0f);
        }
        
        // Switch to level complete state, but keep current difficulty unchanged
        gameState = GameState.LEVEL_COMPLETE;
        
        // Save high score
        if (currentScore > highScore) {
            highScore = currentScore;
            prefs.putInteger(PREF_HIGH_SCORE, highScore);
            prefs.flush();
        }
    }

    private void loadBackgrounds() {
        try {
            System.out.println("Loading background textures...");
            
            // Load all background textures
            menuBackground = new Texture(Gdx.files.internal("menu_background.png"));
            System.out.println("Loaded menu_background.png");
            
            poolBackground = new Texture(Gdx.files.internal("pool_background.png"));
            System.out.println("Loaded pool_background.png");
            
            beachBackground = new Texture(Gdx.files.internal("beach_background.png"));
            System.out.println("Loaded beach_background.png");
            
            parkBackground = new Texture(Gdx.files.internal("park_background.png"));
            System.out.println("Loaded park_background.png");
            
            stormBackground = new Texture(Gdx.files.internal("storm_background.png"));
            System.out.println("Loaded storm_background.png");
            
            // Set background texture filtering
            menuBackground.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            poolBackground.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            beachBackground.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            parkBackground.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            stormBackground.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            
            // Set level background mapping
            level1Background = poolBackground;    // First level uses pool background
            level2Background = beachBackground;   // Second level uses beach background
            level3Background = parkBackground;    // Third level uses park background
            bossBackground = stormBackground;     // Boss battle uses storm background
            
            System.out.println("All background textures loaded successfully");
        } catch (Exception e) {
            System.err.println("Error loading background textures: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadFonts() {
        try {
            // Use default font
            font = new BitmapFont();
            font.getData().setScale(1.5f);  // Normal text size
            font.setColor(Color.WHITE);
            
            // Create title font (also use default font, but larger size)
            titleFont = new BitmapFont();
            titleFont.getData().setScale(2.5f);  // Title text size
            titleFont.setColor(Color.YELLOW);
            
        } catch (Exception e) {
            System.err.println("Error creating fonts: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void renderShop() {
        batch.begin();
        // Draw background
        batch.draw(menuBackground, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        
        // Draw title
        titleFont.setColor(Color.BLACK);
        String shopTitle = "Water Gun Shop";
        layout.setText(titleFont, shopTitle);
        float titleX = (viewport.getWorldWidth() - layout.width) / 2;
        float titleY = viewport.getWorldHeight() * 0.9f;
        titleFont.draw(batch, shopTitle, titleX, titleY);
        
        // Draw total coins number
        font.setColor(Color.BLACK);
        String coinsText = "Your Coins: " + totalCoins;
        layout.setText(font, coinsText);
        float coinsX = viewport.getWorldWidth() - layout.width - 20;
        float coinsY = viewport.getWorldHeight() - 20;
        font.draw(batch, coinsText, coinsX, coinsY);
        
        // Draw back button
        font.setColor(Color.BLACK);
        String backText = "Back";
        layout.setText(font, backText);
        font.draw(batch, backText, 
                 backFromShopButton.x + (backFromShopButton.width - layout.width) / 2,
                 backFromShopButton.y + (backFromShopButton.height + layout.height) / 2);
        
        // Draw each water gun option
        for (int i = 0; i < WaterGunType.values().length; i++) {
            WaterGunType gun = WaterGunType.values()[i];
            Rectangle button = gunButtons[i];
            
            // Draw water gun icon
            batch.setColor(1, 1, 1, 1);
            batch.draw(waterGunTextures[i], 
                      button.x + 10, 
                      button.y + (button.height - PLANE_HEIGHT) / 2,
                      PLANE_WIDTH, PLANE_HEIGHT);
            
            // Draw water gun name and price
            font.setColor(Color.BLACK);
            String gunInfo = gun.getName();
            if (!isUnlocked(gun)) {
                gunInfo += " - Price: " + gun.getPrice() + " coins";
            }
            layout.setText(font, gunInfo);
            font.draw(batch, gunInfo, 
                     button.x + PLANE_WIDTH + 30,
                     button.y + button.height - (button.height - layout.height) / 2);

            // If water gun is unlocked, draw level box and upgrade cost
            if (isUnlocked(gun)) {
                float boxWidth = 30;
                float boxHeight = 10;
                float boxSpacing = 5;
                float startX = button.x + PLANE_WIDTH + 30;
                float startY = button.y + 20;

                // Draw three level boxes
                for (int level = 1; level <= 3; level++) {
                    if (level <= gun.getLevel()) {
                        // Reached level use orange
                        batch.setColor(1f, 0.5f, 0f, 1f);
                    } else {
                        // Not reached level use gray
                        batch.setColor(0.7f, 0.7f, 0.7f, 1f);
                    }
                    batch.draw(bulletTexture, 
                             startX + (level-1) * (boxWidth + boxSpacing), 
                             startY, 
                             boxWidth, boxHeight);
                }

                // If can upgrade, show upgrade cost
                if (gun.canUpgrade()) {
                    font.setColor(Color.BLACK);
                    String upgradeInfo = "Upgrade: " + gun.getUpgradePrice() + " coins";
                    layout.setText(font, upgradeInfo);
                    font.draw(batch, upgradeInfo,
                             startX + 3 * (boxWidth + boxSpacing) + 10,
                             startY + boxHeight);
                }
            }
        }
        
        batch.setColor(1, 1, 1, 1); // Reset color
        batch.end();
    }

    private boolean isUnlocked(WaterGunType gun) {
        return unlockedGuns.contains(gun, true);
    }

    private void handleShopInput() {
        if (Gdx.input.justTouched()) {
            Vector2 touch = new Vector2(Gdx.input.getX(), Gdx.input.getY());
            Vector3 worldCoords = viewport.unproject(new Vector3(touch.x, touch.y, 0));
            
            // Handle back button
            if (backFromShopButton.contains(worldCoords.x, worldCoords.y)) {
                if (isStartingGame) {
                    // If from start game to shop, back to start game directly
                    isStartingGame = false;
                    applyDifficultySettings();
                    gameState = GameState.PLAYING;
                    resetGame();
                } else {
                    // Normal shop return
                    gameState = GameState.MENU;
                }
                return;
            }
            
            // Handle water gun selection/purchase/upgrade
            for (int i = 0; i < WaterGunType.values().length; i++) {
                if (gunButtons[i].contains(worldCoords.x, worldCoords.y)) {
                    WaterGunType gun = WaterGunType.values()[i];
                    
                    if (unlockedGuns.contains(gun, true)) {
                        // If unlocked, check if can upgrade
                        if (gun.canUpgrade()) {
                            int upgradePrice = gun.getUpgradePrice();
                            if (totalCoins >= upgradePrice) {
                                totalCoins -= upgradePrice;
                                gun.upgrade();
                                // Save upgrade and coin data
                                prefs.putInteger("gun_level_" + gun.getId(), gun.getLevel());
                                prefs.putInteger("totalCoins", totalCoins);
                                prefs.flush();
                                
                                // Play upgrade sound
                                if (powerupSound != null) {
                                    powerupSound.play(0.5f);
                                }
                            }
                        }
                        // Select this water gun
                        currentGun = gun;
                        planeTexture = waterGunTextures[gun.getId()];
                        prefs.putInteger("currentGun", gun.getId());
                        prefs.flush();
                    } else if (totalCoins >= gun.getPrice()) {
                        // Purchase new water gun
                        totalCoins -= gun.getPrice();
                        unlockedGuns.add(gun);
                        currentGun = gun;
                        planeTexture = waterGunTextures[gun.getId()];
                        
                        // Save purchase data
                        StringBuilder unlockedGunsStr = new StringBuilder();
                        for (WaterGunType unlockedGun : unlockedGuns) {
                            if (unlockedGunsStr.length() > 0) {
                                unlockedGunsStr.append(",");
                            }
                            unlockedGunsStr.append(unlockedGun.getId());
                        }
                        prefs.putString("unlockedGuns", unlockedGunsStr.toString());
                        prefs.putInteger("currentGun", gun.getId());
                        prefs.putInteger("totalCoins", totalCoins);
                        prefs.flush();
                        
                        // 播放购买音效
                        if (powerupSound != null) {
                            powerupSound.play(0.5f);
                        }
                    }
                    break;
                }
            }
        }
    }

    private void updateCoins(float deltaTime) {
        // Update existing coins
        for (int i = coins.size - 1; i >= 0; i--) {
            Coin coin = coins.get(i);
            if (coin.active) {
                coin.y += coin.velocityY * deltaTime;
                
                // Check if collected
                Rectangle coinRect = new Rectangle(coin.x, coin.y, COIN_WIDTH, COIN_HEIGHT);
                if (planeRect.overlaps(coinRect)) {
                    coin.active = false;
                    totalCoins++;
                    currentLevelCoins++;  // Increase current level coin count
                    prefs.putInteger("totalCoins", totalCoins);
                    prefs.flush();
                    
                    // Play coin collection sound, reduce volume
                    if (powerupSound != null) {
                        long soundId = powerupSound.play(0.1f);  // Reduce volume to 30%
                        powerupSound.setPitch(soundId, 1.0f);
                        powerupSound.setVolume(soundId, 0.1f);
                    }
                }
                
                // Check if out of screen
                if (coin.y < -COIN_HEIGHT) {
                    coin.active = false;
                }
            }
        }
        
        // Remove inactive coins
        for (int i = coins.size - 1; i >= 0; i--) {
            if (!coins.get(i).active) {
                coins.removeIndex(i);
            }
        }
    }

    private void renderGunSelect() {
        batch.begin();
        // Draw background
        batch.draw(poolBackground, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        
        // Draw top water drop indicator
        float indicatorSize = 40;
        float startX = viewport.getWorldWidth() / 2 - (3 * indicatorSize);
        float topY = viewport.getWorldHeight() - 50;
        for (int i = 0; i < 6; i++) {
            batch.draw(bulletTexture, startX + (i * indicatorSize), topY, indicatorSize, indicatorSize);
        }
        
        // Arrange four water guns horizontally
        float gunY = viewport.getWorldHeight() / 2;
        float spacing = viewport.getWorldWidth() / 5;
        float gunSize = 120; // Large size water gun
        
        for (int i = 0; i < WaterGunType.values().length; i++) {
            WaterGunType gun = WaterGunType.values()[i];
            float gunX = spacing + (i * spacing);
            
            // If current selected water gun and unlocked, draw water drop background
            if (currentGun == gun && isUnlocked(gun)) {
                float dropSize = 140;
                batch.draw(bulletTexture, 
                          gunX - dropSize/2,
                          gunY - dropSize/2,
                          dropSize, dropSize);
            }
            
            // Draw water gun, unlocked show as gray
            if (!isUnlocked(gun)) {
                batch.setColor(0.5f, 0.5f, 0.5f, 0.5f); // Gray semi-transparent
            }
            batch.draw(waterGunTextures[i], 
                      gunX - gunSize/2, 
                      gunY - gunSize/2, 
                      gunSize, gunSize);
            batch.setColor(1, 1, 1, 1); // Reset color
            
            // Draw water gun name
            font.setColor(Color.BLACK);
            String name = gun.getName();
            layout.setText(font, name);
            font.draw(batch, name, 
                     gunX - layout.width/2,
                     gunY - gunSize/2 - 20);
            
            // Show shop related information
            if (!isUnlocked(gun)) {
                // Unlocked show price
                String priceText = "Price: " + gun.getPrice() + " coins";
                layout.setText(font, priceText);
                font.draw(batch, priceText,
                         gunX - layout.width/2,
                         gunY - gunSize/2 - 50);
            } else {
                // Unlocked show level
                float barX = gunX - 50;
                float barY = gunY - gunSize/2 - 50;
                float barWidth = 30;
                float barHeight = 10;
                
                // Draw level bar
                for (int level = 1; level <= 3; level++) {
                    if (level <= gun.getLevel()) {
                        // Reached level use water drop texture
                        batch.draw(bulletTexture, 
                                 barX + (level-1) * (barWidth + 5),
                                 barY, 
                                 barWidth, barHeight);
                    }
                }
                
                // If can upgrade, show upgrade cost
                if (gun.canUpgrade()) {
                    String upgradeText = "Upgrade: " + gun.getUpgradePrice() + " coins";
                    layout.setText(font, upgradeText);
                    font.draw(batch, upgradeText,
                             gunX - layout.width/2,
                             barY - 20);
                }
            }
        }
        
        // Draw select button
        float buttonY = viewport.getWorldHeight() * 0.3f;
        String selectText = "Select";
        layout.setText(font, selectText);
        float buttonX = (viewport.getWorldWidth() - layout.width) / 2;
        
        batch.draw(bulletTexture, 
                  buttonX - 20, buttonY - 10,
                  layout.width + 40, layout.height + 20);
                  
        font.setColor(Color.BLACK);
        font.draw(batch, selectText, buttonX, buttonY + layout.height);
        
        // Draw description text
        String description = "Standard Water Gun: Balanced speed and power";
        layout.setText(font, description);
        font.draw(batch, description,
                 (viewport.getWorldWidth() - layout.width) / 2,
                 buttonY - 40);
        
        batch.end();
    }

    private void handleGunSelect() {
        if (Gdx.input.justTouched()) {
            Vector2 touch = new Vector2(Gdx.input.getX(), Gdx.input.getY());
            Vector3 worldCoords = viewport.unproject(new Vector3(touch.x, touch.y, 0));
            
            // Check which water gun is clicked
            float spacing = viewport.getWorldWidth() / 5;
            float gunY = viewport.getWorldHeight() / 2;
            float gunSize = 120;
            
            for (int i = 0; i < WaterGunType.values().length; i++) {
                float gunX = spacing + (i * spacing);
                Rectangle gunRect = new Rectangle(
                    gunX - gunSize/2, gunY - gunSize/2,
                    gunSize, gunSize
                );
                
                if (gunRect.contains(worldCoords.x, worldCoords.y)) {
                    WaterGunType gun = WaterGunType.values()[i];
                    // Only unlocked water guns can be selected
                    if (isUnlocked(gun)) {
                        currentGun = gun;
                        planeTexture = waterGunTextures[i];
                    }
                }
            }
            
            // Check if select button is clicked
            float buttonY = viewport.getWorldHeight() * 0.3f;
            String selectText = "Select";
            layout.setText(font, selectText);
            float buttonX = (viewport.getWorldWidth() - layout.width) / 2;
            Rectangle selectButton = new Rectangle(
                buttonX - 20, buttonY - 10,
                layout.width + 40, layout.height + 20
            );
            
            if (selectButton.contains(worldCoords.x, worldCoords.y)) {
                // Start game, but keep current difficulty settings
                gameState = GameState.PLAYING;
                resetForNextLevel(); // Use resetForNextLevel instead of resetGame
                applyDifficultySettings(); // Ensure current difficulty settings are applied
            }
        }
    }

    private void renderGameOver() {
        batch.begin();
        // Draw current background
        switch (currentDifficulty) {
            case EASY:
                batch.draw(level1Background, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
                break;
            case NORMAL:
                batch.draw(level2Background, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
                break;
            case HARD:
                batch.draw(level3Background, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
                break;
        }
        
        // Draw semi-transparent black mask
        batch.setColor(0, 0, 0, 0.7f);
        batch.draw(bulletTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        batch.setColor(1, 1, 1, 1);
        
        // Draw GAME OVER text
        font.setColor(1, 0, 0, 1);  // Red
        font.getData().setScale(4);
        String gameOverText = "GAME OVER";
        layout.setText(font, gameOverText);
        float gameOverX = (viewport.getWorldWidth() - layout.width) / 2;
        float gameOverY = viewport.getWorldHeight() * 0.7f;
        font.draw(batch, gameOverText, gameOverX, gameOverY);
        
        // Show score
        font.getData().setScale(2);
        font.setColor(1, 1, 1, 1);  // White
        
        // Show kills
        String killsText = "Ducks Shot: " + ducksKilled;
        layout.setText(font, killsText);
        float killsX = (viewport.getWorldWidth() - layout.width) / 2;
        float killsY = viewport.getWorldHeight() * 0.5f;
        font.draw(batch, killsText, killsX, killsY);
        
        // Show coins number
        String coinsText = "Coins Collected: " + currentLevelCoins;
        layout.setText(font, coinsText);
        float coinsX = (viewport.getWorldWidth() - layout.width) / 2;
        float coinsY = viewport.getWorldHeight() * 0.4f;
        font.draw(batch, coinsText, coinsX, coinsY);
        
        // Show tip text
        String tipText = "Press SPACE to return to menu";
        layout.setText(font, tipText);
        float tipX = (viewport.getWorldWidth() - layout.width) / 2;
        float tipY = viewport.getWorldHeight() * 0.2f;
        font.draw(batch, tipText, tipX, tipY);
        
        font.getData().setScale(1);
        batch.end();
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (backgroundTexture != null) backgroundTexture.dispose();
        if (planeTexture != null) planeTexture.dispose();
        if (monsterTexture != null) monsterTexture.dispose();
        if (deadTexture != null) deadTexture.dispose();
        if (bulletTexture != null) bulletTexture.dispose();
        if (bullet2Texture != null) bullet2Texture.dispose();
        if (bossTexture != null) bossTexture.dispose();
        if (bossShootTexture != null) bossShootTexture.dispose();
        
        // Release music resources
        if (menuMusic != null) menuMusic.dispose();
        if (gameMusic != null) gameMusic.dispose();
        if (bossMusic != null) bossMusic.dispose();
        
        // Release sound resources
        if (waterShootSound != null) waterShootSound.dispose();
        if (duckHitSound != null) duckHitSound.dispose();
        if (powerupSound != null) powerupSound.dispose();
        
        // Release background textures
        if (menuBackground != null) menuBackground.dispose();
        if (poolBackground != null) poolBackground.dispose();
        if (beachBackground != null) beachBackground.dispose();
        if (parkBackground != null) parkBackground.dispose();
        if (stormBackground != null) stormBackground.dispose();
        
        // Release font resources
        if (font != null) font.dispose();
        if (titleFont != null) titleFont.dispose();
        if (monster2Texture != null) monster2Texture.dispose();
        if (livesTexture != null) livesTexture.dispose();
    }
}
