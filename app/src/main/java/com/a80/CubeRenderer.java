package com.a80;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CubeRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "CubeRenderer";
    private final List<Model> models = new ArrayList<>();
    private final TouchHandler touchHandler;

    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private boolean wireframeMode = false;

    public CubeRenderer(TouchHandler touchHandler) {
        this.touchHandler = touchHandler;
        models.add(createCube());
    }

    public TouchHandler getTouchHandler() {
        return touchHandler;
    }

    public void setWireframeMode(boolean wireframeMode) {
        this.wireframeMode = wireframeMode;
    }

    public void loadModel(InputStream objInputStream) {
        try {
            Model model = parseObj(objInputStream);
            models.clear();
            models.add(model);
            Log.d(TAG, "Model loaded successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error loading model: " + e.getMessage(), e);
        }
    }

    public void unloadModels() {
        models.clear();
        models.add(createCube());
        Log.d(TAG, "All models unloaded.");
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, 20, 0, 0, 0, 0, 1, 0);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, 0, 0, -10);
        Matrix.rotateM(modelMatrix, 0, touchHandler.getRotationX(), 1, 0, 0);
        Matrix.rotateM(modelMatrix, 0, touchHandler.getRotationY(), 0, 1, 0);
        Matrix.scaleM(modelMatrix, 0, touchHandler.getScale(), touchHandler.getScale(), touchHandler.getScale());

        float[] mvpMatrix = new float[16];
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, modelMatrix, 0);

        for (Model model : models) {
            model.draw(mvpMatrix, wireframeMode);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float aspectRatio = (float) width / height;
        Matrix.frustumM(projectionMatrix, 0, -aspectRatio, aspectRatio, -1, 1, 2, 100);
    }

    private Model createCube() {
        float[] vertices = {
                -1, -1, 1,  1, -1, 1,  1, 1, 1,  -1, 1, 1,
                -1, -1, -1, -1, 1, -1, 1, 1, -1,  1, -1, -1,
                -1, -1, -1, -1, -1, 1, -1, 1, 1, -1, 1, -1,
                1, -1, -1, 1, -1, 1, 1, 1, 1,  1, 1, -1,
                -1, 1, -1, -1, 1, 1, 1, 1, 1,  1, 1, -1,
                -1, -1, -1, 1, -1, -1, 1, -1, 1, -1, -1, 1
        };

        short[] indices = {
                0, 1, 2, 2, 3, 0, 4, 5, 6, 6, 7, 4,
                8, 9, 10, 10, 11, 8, 12, 13, 14, 14, 15, 12,
                16, 17, 18, 18, 19, 16, 20, 21, 22, 22, 23, 20
        };

        return new Model(vertices, indices);
    }

    private Model parseObj(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        List<Float> vertices = new ArrayList<>();
        List<Short> indices = new ArrayList<>();

        String line;
        while ((line = reader.readLine()) != null) {
            String[] tokens = line.split("\\s+");
            if (tokens[0].equals("v")) {
                vertices.add(Float.parseFloat(tokens[1]));
                vertices.add(Float.parseFloat(tokens[2]));
                vertices.add(Float.parseFloat(tokens[3]));
            } else if (tokens[0].equals("f")) {
                for (int i = 1; i <= 3; i++) {
                    String[] parts = tokens[i].split("/");
                    indices.add((short) (Integer.parseInt(parts[0]) - 1));
                }
            }
        }

        return new Model(toFloatArray(vertices), toShortArray(indices));
    }

    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) array[i] = list.get(i);
        return array;
    }

    private short[] toShortArray(List<Short> list) {
        short[] array = new short[list.size()];
        for (int i = 0; i < list.size(); i++) array[i] = list.get(i);
        return array;
    }
}
