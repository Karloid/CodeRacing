import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import model.Car;
import model.Game;
import model.Move;
import model.PlayerContext;

import java.io.IOException;

public final class Runner {
    public static final int LIBGDX_WIDTH = 700;
    private final RemoteProcessClient remoteProcessClient;
    private final String token;

    public static void main(String[] args) throws IOException {
        try {
            Thread.sleep(2300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (args.length == 3) {
            new Runner(args).run();
        } else {
            new Runner(new String[]{"127.0.0.1", "31001", "0000000000000000"}).run();
        }
    }

    private Runner(String[] args) throws IOException {
        remoteProcessClient = new RemoteProcessClient(args[0], Integer.parseInt(args[1]));
        token = args[2];
    }

    public void run() throws IOException {
        try {
            remoteProcessClient.writeToken(token);
            int teamSize = remoteProcessClient.readTeamSize();
            remoteProcessClient.writeProtocolVersion();
            Game game = remoteProcessClient.readGameContext();

            Strategy[] strategies = new Strategy[teamSize];

            for (int strategyIndex = 0; strategyIndex < teamSize; ++strategyIndex) {
                MyKStrategy strategy = new MyKStrategy();

                strategies[strategyIndex] = strategy;
                addShower(strategy);
            }

            PlayerContext playerContext;

            while ((playerContext = remoteProcessClient.readPlayerContext()) != null) {
                Car[] playerCars = playerContext.getCars();
                if (playerCars == null || playerCars.length != teamSize) {
                    break;
                }

                Move[] moves = new Move[teamSize];

                for (int carIndex = 0; carIndex < teamSize; ++carIndex) {
                    Car playerCar = playerCars[carIndex];

                    Move move = new Move();
                    moves[carIndex] = move;
                    strategies[playerCar.getTeammateIndex()].move(
                            playerCar, playerContext.getWorld(), game, move
                    );
                }

                remoteProcessClient.writeMoves(moves);
            }
        } finally {
            remoteProcessClient.close();
        }
    }

    private void addShower(MyKStrategy strategy) {
        LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
        cfg.foregroundFPS = 60;
        cfg.title = "CodeRacing";
        cfg.useGL30 = false;
        cfg.width = LIBGDX_WIDTH;
        cfg.height = LIBGDX_WIDTH;
        cfg.x = 1920 + 600;
        cfg.y = -100;

        LibGdxShower shower = new LibGdxShower();
        new LwjglApplication(shower, cfg);
        strategy.painter = new LibGdxPainter(shower);
        strategy.painter.setMYS(strategy);
    }
}
