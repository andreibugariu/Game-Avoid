package com.first.game.screen.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;
import com.common.GameManager;
import com.first.game.config.DifficultyLevel;
import com.first.game.config.GameConfig;
import com.first.game.entity.Background;
import com.first.game.entity.Obstacle;
import com.first.game.entity.Player;



public class GameController {

    // == constants ==
    private static final Logger log = new Logger(GameController.class.getName(), Logger.DEBUG);

    // == attributes ==
    private Player player;
    private Array<Obstacle> obstacles = new Array<Obstacle>();
    private Background background;
    private float obstacleTimer;
    private float scoreTimer;
    private int lives = GameConfig.LIVES_START;
    private int score;
    private int displayScore;

    private Pool<Obstacle> obstaclePool;
    private final float startPlayerX = (GameConfig.WORLD_WIDTH - GameConfig.PLAYER_SIZE) / 2f;
    private final float startPlayerY = 1 - GameConfig.PLAYER_SIZE / 2f;


    // == constructors ==
    public GameController() {
        init();
    }

    // == init ==
    private void init() {
        // create player
        player = new Player();

        // position player
        player.setPosition(startPlayerX, startPlayerY);

        // create obstacle pool
        obstaclePool = Pools.get(Obstacle.class, 40);

        // create background
        background = new Background();
        background.setPosition(0, 0);
        background.setSize(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT);
    }

    // == public methods ==
    public void update(float delta) {
        if (isGameOver()) {
            return;
        }

        updatePlayer();
        updateObstacles(delta);
        updateScore(delta);
        updateDisplayScore(delta);

        if (isPlayerCollidingWithObstacle()) {
            log.debug("Collision detected.");
            lives--;

            if (isGameOver()) {
                log.debug("Game Over!!!");
                GameManager.INSTANCE.updateHighScore(score);
            } else {
                restart();
            }
        }
    }

    public Player getPlayer() {
        return player;
    }

    public Array<Obstacle> getObstacles() {
        return obstacles;
    }

    public Background getBackground() {
        return background;
    }

    public int getLives() {
        return lives;
    }

    public int getDisplayScore() {
        return displayScore;
    }


    public boolean isGameOver() {
        return lives <= 0;
    }

    // == private methods ==
    private void restart() {
        obstaclePool.freeAll(obstacles);
        obstacles.clear();
        player.setPosition(startPlayerX, startPlayerY);
    }

    private boolean isPlayerCollidingWithObstacle() {
        for (Obstacle obstacle : obstacles) {
            if (obstacle.isNotHit() && obstacle.isPlayerColliding(player)) {
                return true;
            }
        }

        return false;
    }

    private void updatePlayer() {
        float xSpeed = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            xSpeed = GameConfig.MAX_PLAYER_X_SPEED;
        } else if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            xSpeed = -GameConfig.MAX_PLAYER_X_SPEED;
        }

        player.setX(player.getX() + xSpeed);

        blockPlayerFromLeavingTheWorld();
    }

    private void blockPlayerFromLeavingTheWorld() {
        float playerX = MathUtils.clamp(
                player.getX(), // value
                0, // min
                GameConfig.WORLD_WIDTH - player.getWidth() // max
        );

        player.setPosition(playerX, player.getY());
    }

    private void updateObstacles(float delta) {
        for (Obstacle obstacle : obstacles) {
            obstacle.update();
        }

        createNewObstacle(delta);
        removePassedObstacles();
    }

    private void createNewObstacle(float delta) {
        obstacleTimer += delta;

        if (obstacleTimer >= GameConfig.OBSTACLE_SPAWN_TIME) {
            float min = 0;
            float max = GameConfig.WORLD_WIDTH - GameConfig.OBSTACLE_SIZE;

            float obstacleX = MathUtils.random(min, max);
            float obstacleY = GameConfig.WORLD_HEIGHT;

            Obstacle obstacle = obstaclePool.obtain();
            DifficultyLevel difficultyLevel=GameManager.INSTANCE.getDifficultyLevel();
            obstacle.setYSpeed(difficultyLevel.getObstacleSpeed());
            obstacle.setPosition(obstacleX, obstacleY);

            obstacles.add(obstacle);
            obstacleTimer = 0f;
        }
    }

    private void removePassedObstacles() {
        if (obstacles.size > 0) {
            Obstacle first = obstacles.first();

            float minObstacleY = -GameConfig.OBSTACLE_SIZE;

            if (first.getY() < minObstacleY) {
                obstacles.removeValue(first, true);
                obstaclePool.free(first);
            }
        }
    }

    private void updateScore(float delta) {
        scoreTimer += delta;

        if (scoreTimer >= GameConfig.SCORE_MAX_TIME) {
            score += 1;
            scoreTimer = 0.0f;
        }
    }

    private void updateDisplayScore(float delta) {
        if (displayScore < score) {
            displayScore = Math.min(
                    score,
                    displayScore + (int) (40 * delta)
            );
        }
    }

}