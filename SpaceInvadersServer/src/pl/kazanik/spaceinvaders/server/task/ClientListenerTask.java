/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.kazanik.spaceinvaders.server.task;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReadWriteLock;
import pl.kazanik.spaceinvaders.server.connection.ServerManager;
import pl.kazanik.spaceinvaders.client.exception.ExceptionUtils;
import pl.kazanik.spaceinvaders.settings.GameConditions;

/**
 *
 * @author user
 */
public class ClientListenerTask extends AbstractClientTask {

    private boolean runn;
    private int frames;
    
    public ClientListenerTask(String clientToken, ServerManager serverManager,
            ReadWriteLock clientTaskLock) {
        super(clientToken, serverManager, GameConditions.LISTENER_TASK, clientTaskLock);
        runn = true;
    }
    
    @Override
    protected void execute() throws IOException {
        int max_frames = 2_000_000_000;
        while(serverManager.checkClientAlive(clientToken)) {
            try {
                frames++;
                Thread.sleep(GameConditions.SERVER_SYNCH_DELAY2);
//                Thread.sleep(1000);
                //if(frames%2 == 0) {
                if(!serverManager.isHeartbeatRunning()) {
                    serverManager.setHeartbeatRunning(true);
                    serverManager.submitClientTask(new ClientHeartbeatTask(
                            clientToken, serverManager, clientTaskLock));
                }
                if(!serverManager.isUpdateRunning()) {
                    serverManager.setUpdateRunning(true);
                    serverManager.submitClientTask(new ClientUpdateTask(
                            clientToken, serverManager, clientTaskLock));
                }
                if(!serverManager.isInputRunning()) {
                    serverManager.setInputRunning(true);
                    serverManager.submitClientTask(new ClientInputListenerTask(
                            clientToken, serverManager, clientTaskLock));
                }
                if(!serverManager.isOutputRunning()) {
                    serverManager.setOutputRunning(true);
                    serverManager.submitClientTask(new ClientOutputPrinterTask(
                            clientToken, serverManager, clientTaskLock));
                }
                serverManager.checkClientAlive(clientToken);
                if(frames >= max_frames)
                    frames = 0;
                //System.out.println("blabllab");
            } catch(InterruptedException ex) {
//            catch(TimeoutException te) {}
//            catch (ExecutionException ex) {
                if(ExceptionUtils.isCausedByIOEx(ex)) {
                    System.out.println("@@@@@client listener execute: "
                        + "listen/update task exception catched, "
                        + "now try stop thread and close resources");
                    runn = false;
                } else {
                    System.out.println("so timeout");
                }
            }
        }
        serverManager.disconnectClient(clientToken);
    }

    @Override
    public Boolean call() throws Exception {
        try {
            execute();
            return true;
        } catch(IOException e) {
            System.out.println("listener run catch io");
            error = e;
            //throw new RuntimeException("ex updating clients", e);
            throw e;
        }
    }
    
}
