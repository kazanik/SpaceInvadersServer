/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.kazanik.spaceinvaders.server.connection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.kazanik.spaceinvaders.server.main.ServerGameLoop;
import pl.kazanik.spaceinvaders.server.session.SessionManager;
import pl.kazanik.spaceinvaders.server.task.ClientListenerTask;
import pl.kazanik.spaceinvaders.settings.GameConditions;
import pl.kazanik.spaceinvaders.thread.ClientGameLoop;

/**
 *
 * @author user
 */
public class ServerMainRun implements Runnable {

    private InetAddress serverAddress;
    private ServerSocket serverSocket;
    private boolean running;
    private final ServerManager serverManager;
    private final Object serverLock;
    private final SessionManager sessionManager;

    public ServerMainRun(ServerManager serverManager, Object serverLock,
            SessionManager sessionManager) {
        this.serverManager = serverManager;
        this.serverLock = serverLock;
        this.sessionManager = sessionManager;
    }
    
    private boolean initServer() {
        running = false;
        try {
            serverAddress = InetAddress.getLocalHost();
            //serverSocket = new ServerSocket(GameConditions.SERVER_PORT);
            serverSocket = new ServerSocket(GameConditions.SERVER_PORT, 
                    serverManager.getMAX_PLAYERS(), serverAddress);
//            serverManager.initClientThreadPools();
            serverSocket.setSoTimeout(20);
            running = true;
            return true;
        } catch (UnknownHostException ex) {
            Logger.getLogger(ServerMainRun.class.getName()).log(Level.SEVERE, 
                    "cannot get local host address", ex);
        } catch (IOException e) {
            Logger.getLogger(ServerMainRun.class.getName()).log(Level.SEVERE,
                "Exception caught when trying to listen on port "
                + GameConditions.SERVER_PORT + " or listening for a connection", e);
        }
        return false;
    }
    
    private void closeClientSocket(String clientTokenGenerated, Socket clientSocket) {
        try {
            clientSocket.close();
        } catch(IOException e) {
            System.err.println("ex closing client socket");
        } finally {
            //sessionManager.decrementTokenCounter();
            try {
                serverManager.disconnectClient(clientTokenGenerated);
            } catch(IOException ioe) {
                System.err.println("ioex disconnecting client in closeClientSocket()");
            }
        }
    }
    
    private void printClientInfo(Socket clientSocket) {
        System.out.println("client remote adres: " 
            + clientSocket.getRemoteSocketAddress()
            + "\nclient inet adres: " + clientSocket.getInetAddress().getHostName()
                + " , " + clientSocket.getInetAddress().getCanonicalHostName()
                + " , " + clientSocket.getInetAddress().getHostAddress()
            + "\nclient local adres: " + clientSocket.getLocalAddress().getHostName()
                + " , " + clientSocket.getLocalAddress().getCanonicalHostName()
                + " , " + clientSocket.getLocalAddress().getHostAddress()
            + "\nclient local socket adres: " + clientSocket.getLocalSocketAddress()
            + "\nclient ports: " + clientSocket.getLocalPort() 
            + " , " + clientSocket.getPort()
        );
    }
    
    @Override
    public void run() {
        boolean initCompleted = initServer();
        System.out.println("init completed: " + initCompleted);
        System.out.println("serwer lokal adres: "+serverSocket.getLocalSocketAddress()
            +"\nserwer port: "+serverSocket.getLocalPort()
            +"\nserwer inet adres: "+serverSocket.getInetAddress());
        if(initCompleted) {
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.setSoTimeout(20);
                    String clientToken = sessionManager.generateClientToken();
                    if(serverManager.connectClient(clientSocket, clientToken)) {
                        printClientInfo(clientSocket);
                        serverManager.submitClientTask(
                            new ClientListenerTask(clientToken, serverManager, 
                                new ReentrantReadWriteLock(true)));
                        if(serverManager.isGameStarted()) {
                            // connect client to game
                        } else {
                            if(serverManager.isServerFull()) {
                                serverManager.prepareGame();
                                serverManager.startGame();
                            }
                        }
                    } else {
                        closeClientSocket(clientToken, clientSocket);
                    }
                } catch(SocketTimeoutException stex) {
                    //System.out.println("socket ex in accept");
                } catch(IOException e) {
                    System.out.println("Exception caught when trying to listen on port "
                        + GameConditions.SERVER_PORT + " or listening for a connection");
                    System.out.println(e.getMessage());
                    running = false;
                } //catch (InterruptedException ex) {}
            }
        }
        try {
            if(serverSocket != null)
                serverSocket.close();
        } catch (IOException ex) {
            System.err.println("ex closing server socket");
        }
        serverManager.closeServer();
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public ServerManager getServerManager() {
        return serverManager;
    }
    
}
