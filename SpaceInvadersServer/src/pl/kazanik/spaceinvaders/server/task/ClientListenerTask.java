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
        while(runn) {
            try {
                frames++;
                Thread.sleep(GameConditions.SERVER_SYNCH_DELAY2);
                //Thread.sleep(100);
                AbstractClientTask heartbeatTask = 
                    new ClientHeartbeatTask(clientToken, serverManager, clientTaskLock);
                AbstractClientTask updateTask = 
                    new ClientUpdateTask(clientToken, serverManager, clientTaskLock);
                AbstractClientTask inputTask = 
                    new ClientInputListenerTask(clientToken, serverManager, clientTaskLock);
                AbstractClientTask outputTask = 
                    new ClientOutputPrinterTask(clientToken, serverManager, clientTaskLock);
                //if(frames%2 == 0) {
                    Future<?> futureHeartbeat = serverManager.submitClientHeartbeatTask(heartbeatTask);
                    futureHeartbeat.get(100, TimeUnit.MICROSECONDS);
                //}
                Future<?> futureUpdate = serverManager.submitClientUpdateTask(updateTask);
                futureUpdate.get(100, TimeUnit.MICROSECONDS);
                Future<?> futureInput = serverManager.submitClientInputTask(inputTask);
                futureInput.get(100, TimeUnit.MICROSECONDS);
                Future<?> futureOutput = serverManager.submitClientOutputTask(outputTask);
                futureOutput.get(100, TimeUnit.MICROSECONDS);
                if(frames >= max_frames)
                    frames = 0;
                //System.out.println("blabllab");
            } catch(InterruptedException e) {}
            catch(TimeoutException te) {}
            catch (ExecutionException ex) {
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
