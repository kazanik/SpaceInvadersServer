/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.kazanik.spaceinvaders.server.main;

import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.kazanik.spaceinvaders.entity.AbstractEntity;
import pl.kazanik.spaceinvaders.entity.AbstractSpaceCraft;
import pl.kazanik.spaceinvaders.entity.EntityManager;
import pl.kazanik.spaceinvaders.entity.PlayerEntity;
import pl.kazanik.spaceinvaders.generator.EnemyGenerator;
import pl.kazanik.spaceinvaders.main.GameCanvas;
import pl.kazanik.spaceinvaders.settings.GameConditions;
import pl.kazanik.spaceinvaders.settings.GameSettings;
import pl.kazanik.spaceinvaders.sound.SoundPlayer;
import pl.kazanik.spaceinvaders.thread.ClientGameLoop;

/**
 *
 * @author user
 */
public class ServerGameLoop implements Runnable {
    
    private List<PlayerEntity> players;
    private EntityManager em = EntityManager.getInstance();
    private GameSettings settings = GameSettings.getInstance();
    private EnemyGenerator eg = new EnemyGenerator();
    private SoundPlayer sp = new SoundPlayer();
    private boolean running = true;
    private int frames = 0;
    private int enemiesCreated = 0;
    
    public ServerGameLoop() {
        
    }
    
    public ServerGameLoop(List<PlayerEntity> players) {
        this.players = players;
    }
    
    @Override
    public void run() {
        System.out.println("Update runnable");
        long lastFrameTime = 0l;
        long lastCreateTime = System.currentTimeMillis();
        Random rnd = new Random();
        gameloop:
        while(running) {
            // Scene
            try {
                Thread.sleep(GameConditions.SCENE_REFRESH_RATE);
                frames++;
                lastFrameTime = System.currentTimeMillis();
            } catch (InterruptedException ex) {
                Logger.getLogger(ClientGameLoop.class.getName()).log(Level.SEVERE, null, ex);
                running = false;
            }
            //      Enemy Missles move
            for(AbstractEntity missle : em.getEnemyMissles()) {
                missle.move();
                missle.setLastMoveFrame(frames);
            }
            
            // Create enemies
            if(enemiesCreated < (settings.getDifficulty().getEnemyWaves() *
                    settings.getDifficulty().getEnemiesInWave())) {
                if(System.currentTimeMillis()-lastCreateTime > 
                        settings.getDifficulty().getEnemyWaveIntervalMilis()) {
                    for (int i = 0; i < rnd.nextInt(4)+1; i++) {
                        em.addEnemy(eg.generateEnemy());
                        enemiesCreated++;
                    }
                    lastCreateTime = System.currentTimeMillis();
                }
            }
            //      Enemies move
            for(AbstractSpaceCraft enemy : em.getEnemies()) {
                enemy.move();
                enemy.setLastMoveFrame(frames);
            }
            //      ??? Player actions kolejka
            for(PlayerEntity player : players) {
                player.doAction();
                player.setLastMoveFrame(frames);
            }
            frames = (frames >= 2000000000) ? 0 : frames;
            //      Collisions
            for(AbstractSpaceCraft enemy : em.getEnemies()) {
                for(PlayerEntity player : players) {
                if(enemy.getSprite().collisionRect().intersects(
                        player.getSprite().collisionRect())) {
//                    gameLoop.abort();
                    break gameloop;
                }
                for(AbstractEntity missle : em.getEnemyMissles()) {
                    if(player.getSprite().collisionRect().intersects(
                            missle.getSprite().collisionRect())) {
//                        gameLoop.abort();
                        break gameloop;
                    }
                }
                for(AbstractEntity missle : em.getPlayerMissles()) {
                    if(enemy.getSprite().collisionRect().intersects(
                            missle.getSprite().collisionRect())) {
                        enemy.collision(missle);
                        missle.collision(enemy);
                        //
                        sp.play(GameConditions.EXPLOSION_SOUND_PATH);
                    }
                }
                }
            }
            em.checkDestroyedCrafts();
            em.checkDestroyedMissles();
        }
    }
}
