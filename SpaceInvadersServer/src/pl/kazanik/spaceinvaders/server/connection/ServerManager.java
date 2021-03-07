/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.kazanik.spaceinvaders.server.connection;

import pl.kazanik.spaceinvaders.client.exception.ClientDisconnectedException;
import pl.kazanik.spaceinvaders.server.task.AbstractClientTask;
import pl.kazanik.spaceinvaders.server.task.ClientListenerTask;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.kazanik.spaceinvaders.difficulty.Difficulties;
import pl.kazanik.spaceinvaders.entity.AbstractSpaceCraft;
import pl.kazanik.spaceinvaders.generator.PlayerGenerator;
import pl.kazanik.spaceinvaders.generator.SceneGenerator;
import pl.kazanik.spaceinvaders.scene.Scene;
import pl.kazanik.spaceinvaders.client.Client;
import pl.kazanik.spaceinvaders.entity.PlayerEntity;
import pl.kazanik.spaceinvaders.generator.EnemyGenerator;
import pl.kazanik.spaceinvaders.server.main.ServerGameLoop;
import pl.kazanik.spaceinvaders.server.session.SessionManager;
import pl.kazanik.spaceinvaders.settings.GameSettings;



/**
 *
 * @author user
 */
public class ServerManager {
    
    //private volatile ConcurrentMap<String, Client> clients;
    //private static final int IN_STREAM_BUFF_LEN = 4096;
    private volatile List<Client> clients;
    private volatile char[] inStreamBuff;
    private final int MAX_PLAYERS;
    private final Difficulties gameDifficulty;
    private final Object serverLock;
    private final SessionManager session;
    private volatile ExecutorService clientListenerPool, clientHeartbeatPool,
            clientUpdatePool, clientInputPool, clientOutputPool;
    private final Lock clientInLock, clientOutLock;
    private final ReadWriteLock clientTaskLock;
    private boolean gameStarted, gameInited;
    private List<PlayerEntity> players;
    private Thread gameThread;
    private Runnable gameRunnable;

    public ServerManager(int MAX_PLAYERS, Difficulties gameDifficulty, 
            Object serverLock, SessionManager session) {
        this.MAX_PLAYERS = MAX_PLAYERS;
        this.gameDifficulty = gameDifficulty;
        this.serverLock = serverLock;
        this.session = session;
        //clients = new ConcurrentHashMap<>();
        //clients = new CopyOnWriteArrayList<>();
        this.clients = new ArrayList<>();
        this.clientInLock = new ReentrantLock();
        this.clientOutLock = new ReentrantLock();
        this.clientTaskLock = new ReentrantReadWriteLock();
    }
    
    private int calculatePoolMaxThreads() {
        return (int) Math.ceil(MAX_PLAYERS / 8.0);
    }
    
    private Client createNullClient(String token) {
        return new Client(token, null, 0l, null, null, null, null);
    }
    
    public void initClientThreadPools() {
        int maxThreads = calculatePoolMaxThreads();
        //clientListenerPool = Executors.newWorkStealingPool(maxThreads);
        //clientWorkerPool = Executors.newWorkStealingPool(maxThreads);
        clientListenerPool = Executors.newCachedThreadPool();
        clientHeartbeatPool = Executors.newCachedThreadPool();
        clientUpdatePool = Executors.newCachedThreadPool();
        clientInputPool = Executors.newCachedThreadPool();
        clientOutputPool = Executors.newCachedThreadPool();
    }
    
    public synchronized void submitClientListenerTask(String clientToken, Socket clientSocket) {
        clientListenerPool.submit(new ClientListenerTask(clientToken, this, clientTaskLock));
    }
    
    public synchronized Future<?> submitClientHeartbeatTask(AbstractClientTask task) {
        return clientHeartbeatPool.submit(task);
    }
    
    public synchronized Future<?> submitClientUpdateTask(AbstractClientTask task) {
        return clientUpdatePool.submit(task);
    }
    
    public synchronized Future<?> submitClientInputTask(AbstractClientTask task) {
        return clientInputPool.submit(task);
    }
    
    public synchronized Future<?> submitClientOutputTask(AbstractClientTask task) {
        return clientOutputPool.submit(task);
    }
    
    public synchronized void disconnectClient(String token) throws IOException {
        int clientIndex = clients.indexOf(createNullClient(token));
        if(clientIndex != -1) {
            Client c = clients.remove(clientIndex);
            System.out.println("client "+token+" disconnected");
            c.closeSocket();
            System.out.println("client "+token+" socket closed");
            //session.decrementTokenCounter();
        }
        // check if abort game if no clients left
    }
    
    public void disconnectAllClients() {
        for(int i = 0; i < clients.size(); i++) {
            Client client = clients.get(i);
            try {
                client.closeSocket();
            } catch (IOException ex) {
                Logger.getLogger(ServerManager.class.getName()).log(
                    Level.SEVERE, "ex closing client socket", ex);
            }
        }
        clients = new CopyOnWriteArrayList<>();
        session.resetTokenCounter();
    }
    
    public synchronized boolean connectClient(Socket socket, String token) {
        System.out.println("client: " + socket.getPort() + ", token: " + token
            + ", time: " + System.currentTimeMillis());
        if(clients.size() < MAX_PLAYERS) {
            try {
                if(!clients.contains(createNullClient(token))) {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
                    Client c = new Client(token, socket, System.currentTimeMillis(), 
                            in, out, clientInLock, clientOutLock);
                    clients.add(c);
                }
                return clients.contains(createNullClient(token));
            } catch(IOException ioe) {
                Logger.getLogger(ServerManager.class.getName()).log(
                    Level.SEVERE, "ex connecting client socket", ioe);
                return false;
            }
        }
        System.out.println("client refused: " + socket.getPort());
        System.out.println("clients size: " + clients.size());
        return false;
    }
    
    public synchronized Client getClient(String token, String location) throws IOException {
        int index = clients.indexOf(createNullClient(token));
        if(index == -1)
            throw new ClientDisconnectedException(token, location, null, null);
        return clients.get(index);
    }
    
    public synchronized void updateClient(String clientToken, String location) throws IOException {
        int clientIndex = clients.indexOf(createNullClient(clientToken));
        if(clientIndex != -1) {
            Client client = clients.get(clientIndex);
            client.setLastHeartBeat(System.currentTimeMillis());
            clients.set(clientIndex, client);
        } else {
            throw new ClientDisconnectedException(clientToken, location, null, null);
        }
    }
    
    public void closeServer() {
        disconnectAllClients();
        clientListenerPool.shutdown();
        clientHeartbeatPool.shutdown();
        clientUpdatePool.shutdown();
        clientInputPool.shutdown();
        clientOutputPool.shutdown();
        //serverThread.getServerRunnable().setRunning(false);
        if(gameThread != null)
            gameThread.interrupt();
        gameThread = null;
    }
    
    public void prepareGame() {
        GameSettings settings = GameSettings.getInstance();
        settings.setDifficulty(gameDifficulty);
        SceneGenerator sg = new SceneGenerator();
        PlayerGenerator pg = new PlayerGenerator();
        Scene gameScene = sg.generate();
        settings.setGameScene(gameScene);
//        List<AbstractSpaceCraft> players = new ArrayList<>();
        players = new ArrayList<>();
//        for (int i = 0; i < MAX_PLAYERS; i++) {
//            AbstractSpaceCraft player = pg.generate();
//            players.add((PlayerEntity) player);
//        }
        EnemyGenerator eg = new EnemyGenerator();
        eg.generate();
        gameInited = true;
    }
    
    public void startGame() {
        gameRunnable = new ServerGameLoop(players);
        gameThread = new Thread(gameRunnable);
        gameThread.start();
        gameStarted = true;
    }
    
    public synchronized int getClientsCount() {
        return clients.size();
    }

    public synchronized boolean isServerFull() {
        return clients.size() == MAX_PLAYERS;
    }

    public int getMAX_PLAYERS() {
        return MAX_PLAYERS;
    }
    
    public synchronized boolean isClientConnected(String clientToken) {
        return clients.contains(createNullClient(clientToken));
    }

    public synchronized List<Client> getClients() {
        return clients;
    }
    
    public synchronized boolean areAtleastTwoConnected() {
        return clients.size() > 1;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public boolean isGameInited() {
        return gameInited;
    }

    public List<PlayerEntity> getPlayers() {
        return players;
    }
    
}
