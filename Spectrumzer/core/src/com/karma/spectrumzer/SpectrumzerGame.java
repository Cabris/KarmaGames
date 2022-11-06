package com.karma.spectrumzer;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;

public class SpectrumzerGame extends ApplicationAdapter {
    SpriteBatch batch;
    Texture img;
    private OrthographicCamera camera;
    private OrthographicCamera hudCamera;
    private BitmapFont font;
    private SpriteBatch spriteBatch;

    @Override
    public void create() {
        batch = new SpriteBatch();
        img = new Texture("badlogic.jpg");
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 480);

        hudCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        hudCamera.position.set(hudCamera.viewportWidth / 2.0f, hudCamera.viewportHeight / 2.0f, 1.0f);
        font = new BitmapFont(Gdx.files.internal("Fonts/myFont.fnt"));
        spriteBatch = new SpriteBatch();

    }

    @Override
    public void render() {
        ScreenUtils.clear(1, 0, 0, 1);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        float x = 0;
        float y = 0;

        if (Gdx.input.isTouched()) {
            Vector3 touchPos = new Vector3();
            touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            Vector3 pos = camera.unproject(touchPos);
            x = pos.x - img.getWidth() / 2;
            y = pos.y - img.getHeight() / 2;
        }

        batch.draw(img, x, y);
        batch.end();

        hudCamera.update();
        spriteBatch.setProjectionMatrix(hudCamera.combined);
        spriteBatch.begin();
        font.draw(spriteBatch, "Upper left, FPS=" + Gdx.graphics.getFramesPerSecond(), 0, hudCamera.viewportHeight);
        font.draw(spriteBatch, "Lower left", 0, font.getLineHeight());
        spriteBatch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        img.dispose();
    }
}
