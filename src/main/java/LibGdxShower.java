import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import model.Car;
import model.TileType;

public class LibGdxShower implements ApplicationListener {

    static {

    }

    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;
    private LibGdxDataToPaint libGdxDataToPaint = new LibGdxDataToPaint();
    private OrthographicCamera camera;

    private boolean didDrawPP; //TODO
    private boolean resized;


    @Override
    public void create() {
        // Create a full-screen camera:
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
// Set it to an orthographic projection with "y down" (the first boolean parameter)
        camera.setToOrtho(true, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = new BitmapFont(true);
        applyCamera();
        font.setColor(Color.RED);

    }

    private void applyCamera() {
        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapes.dispose();
        font.dispose();
    }

    @Override
    public void render() {
        LibGdxDataToPaint data = this.libGdxDataToPaint;
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapes.begin(ShapeRenderer.ShapeType.Filled);

        drawShapes(data);

        shapes.end();


        Gdx.gl.glDisable(GL20.GL_BLEND);
        if (!resized && data != null) {
            resized = true;
            float camWidth = (float) (data.game.getWorldWidth() * data.game.getTrackTileSize());
            //You probably want to keep the aspect ration of the window
            float camHeight = camWidth * ((float) Runner.LIBGDX_WIDTH / (float) Runner.LIBGDX_WIDTH);

            camera.viewportWidth = camWidth;
            camera.viewportHeight = camHeight;
            camera.update();
            applyCamera();
        }
        moveCamera(data);
    }

    private void moveCamera(LibGdxDataToPaint data) {
        if (data == null) {
            return;
        }


    }

    private void drawShapes(LibGdxDataToPaint data) {
        if (data == null) {
            return;
        }
        float tileSize = (float) data.game.getTrackTileSize();
        TileType[][] tilesXY = data.world.getTilesXY();
        for (int x = 0, tilesXYLength = tilesXY.length; x < tilesXYLength; x++) {
            TileType[] columns = tilesXY[x];
            for (int y = 0, columnsLength = columns.length; y < columnsLength; y++) {
                TileType tile = columns[y];
                shapes.setColor(tile != TileType.EMPTY ? Color.GRAY : Color.WHITE);
                shapes.rect(tileToReal(data, x), tileToReal(data, y), tileSize, tileSize);
            }
        }

        for (int i = 0; i < Runner.LIBGDX_WIDTH / 100; i++) {
            shapes.setColor(Color.RED);
            shapes.rect(i * 100, 0, 10, 700);
        }


        drawCar(data, data.mainUnit);

    }

    private void drawCar(LibGdxDataToPaint data, Car car) {
        shapes.setColor(Color.GREEN);
        float x = (float) car.x;
        float y = (float) car.y;

        float w = (float) data.game.getCarWidth() / 2;
        float h = (float) data.game.getCarHeight() / 2;

        shapes.identity();
        shapes.translate(x, y, 0);
        shapes.rotate(0, 0, 1, (float) (car.getAngle() * 60.0f));
        shapes.rect(-w, -h, w * 2, h * 2);

        shapes.identity();
    }

    private float tileToReal(LibGdxDataToPaint data, int v) {
        return (float) (v * data.game.getTrackTileSize());
    }


    public static double root(double num, double root) {
        return Math.pow(Math.E, Math.log(num) / root);
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    public void setObjects(LibGdxDataToPaint libGdxObjs) {
        this.libGdxDataToPaint = libGdxObjs;
    }

}