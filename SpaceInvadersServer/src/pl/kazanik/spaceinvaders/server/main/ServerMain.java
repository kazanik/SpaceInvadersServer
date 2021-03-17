/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.kazanik.spaceinvaders.server.main;

import java.util.ArrayList;
import java.util.List;
import pl.kazanik.spaceinvaders.difficulty.Difficulties;
import pl.kazanik.spaceinvaders.server.connection.ServerManager;
import pl.kazanik.spaceinvaders.server.connection.ServerMainRun;
import pl.kazanik.spaceinvaders.server.session.SessionManager;

/**
 *
 * @author user
 */
public class ServerMain {

    private List<Thread> serverInstances;
    private Thread mainThread;

    public ServerMain() {
        serverInstances = new ArrayList<>();
    }
    
    public void checkServerInstances() {
        mainThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(serverInstances.size() > 0) {
                    try{
                        Thread.sleep(1000*10);
                        int i = 0;
                        for(Thread t : serverInstances) {
                            if(t == null || !t.isAlive() /*|| t.isInterrupted()*/) {
                                t = null;
                                serverInstances.remove(i);
                            }
                            i++;
                        }
                    } catch(InterruptedException ex) {}
                }
            }
        });
    }
    
    public void runServer(int PLAYER_SLOTS, Difficulties gameDifficulty) {
        Object serverLock = new Object();
        SessionManager sessionManager = new SessionManager();
        ServerManager serverManager = new ServerManager(
            PLAYER_SLOTS, gameDifficulty, serverLock, sessionManager);
        ServerMainRun serverRunn = new ServerMainRun(
            serverManager, serverLock, sessionManager);
        serverManager.initServerThreadPool();
        serverManager.launchServer(serverRunn);
//        Thread serverThread = new Thread(serverRunn);
//        serverThread.start();
//        serverInstances.add(serverThread);
//        checkServerInstances();
    }
    
}
