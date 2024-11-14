package com.a80;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CubeRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "CubeRenderer";
    private Cube mCube;
    private final List<ObjModel> objModels = new ArrayList<>();
    private boolean isCubeVisible = true;
    private int cubeShaderProgram;  // Шейдерная программа только для куба
    private int modelShaderProgram; // Шейдерная программа для загружаемых моделей
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final TouchHandler touchHandler;

    public CubeRenderer(TouchHandler touchHandler) {
        this.touchHandler = touchHandler;
    }

    public TouchHandler getTouchHandler() {
        return touchHandler;
    }

    public void loadModel(Context context, InputStream objInputStream) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int bytesRead;
            byte[] data = new byte[1024];
            while ((bytesRead = objInputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            byte[] modelData = buffer.toByteArray();
            InputStream modelStream = new ByteArrayInputStream(modelData);

            ObjModel objModel = new ObjModel(modelStream, modelShaderProgram);
            objModels.add(objModel);

            isCubeVisible = false; // Скрыть куб при загрузке модели
            Log.d(TAG, "Model loaded successfully.");
        } catch (IOException e) {
            Log.e(TAG, "Error loading model: " + e.getMessage());
        }
    }


    public void unloadModels() {
        for (ObjModel objModel : objModels) {
            objModel.destroy();
        }
        objModels.clear();
        isCubeVisible = true; // Показываем куб после разгрузки
        Log.d(TAG, "All models unloaded.");
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        cubeShaderProgram = createCubeShaderProgram();  // Создаем шейдеры для куба
        modelShaderProgram = createModelShaderProgram(); // Создаем шейдеры для моделей

        mCube = new Cube(cubeShaderProgram); // Инициализируем куб с его шейдерной программой
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 20f, 0f, 0f, 0f, 0f, 1f, 0f);
        Log.d(TAG, "Surface created.");
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, 0, 0, -10f);
        Matrix.rotateM(modelMatrix, 0, touchHandler.getRotationX(), 1, 0, 0);
        Matrix.rotateM(modelMatrix, 0, touchHandler.getRotationY(), 0, 1, 0);
        Matrix.scaleM(modelMatrix, 0, touchHandler.getScale(), touchHandler.getScale(), touchHandler.getScale());

        float[] mvpMatrix = new float[16];
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, modelMatrix, 0);

        if (isCubeVisible) {
            mCube.draw(mvpMatrix); // Рисуем куб с его шейдерами
        }

        for (ObjModel objModel : objModels) {
            objModel.draw(mvpMatrix); // Рисуем загружаемые модели с отдельной шейдерной программой
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float aspectRatio = (float) width / height;
        Matrix.frustumM(projectionMatrix, 0, -aspectRatio, aspectRatio, -1, 1, 2, 100);
        Log.d(TAG, "Surface changed.");
    }

    // Метод для создания шейдерной программы куба без использования ShaderProgram
    private int createCubeShaderProgram() {
        String vertexShaderCode =
                "uniform mat4 uMVPMatrix;" +
                        "attribute vec4 vPosition;" +
                        "attribute vec4 vColor;" +
                        "varying vec4 vColorOut;" +
                        "void main() {" +
                        "  gl_Position = uMVPMatrix * vPosition;" +
                        "  vColorOut = vColor;" +
                        "}";

        String fragmentShaderCode =
                "precision mediump float;" +
                        "varying vec4 vColorOut;" +
                        "void main() {" +
                        "  gl_FragColor = vColorOut;" +
                        "}";

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            GLES20.glDeleteProgram(program);
            throw new RuntimeException("Error linking cube shader program.");
        }

        return program;
    }

    // Метод для создания шейдерной программы для загружаемых моделей
    private int createModelShaderProgram() {
        String vertexShaderCode =
                "uniform mat4 uMVPMatrix;" +
                        "attribute vec4 vPosition;" +
                        "attribute vec3 vNormal;" +
                        "varying vec3 vNormalInterp;" +
                        "void main() {" +
                        "  gl_Position = uMVPMatrix * vPosition;" +
                        "  vNormalInterp = normalize(vNormal);" +
                        "}";

        String fragmentShaderCode =
                "precision mediump float;" +
                        "varying vec3 vNormalInterp;" +
                        "void main() {" +
                        "  vec3 lightDir = normalize(vec3(0.0, 0.0, 1.0));" +
                        "  float diff = max(dot(vNormalInterp, lightDir), 0.2);" +
                        "  gl_FragColor = vec4(diff, diff, diff, 1.0);" +
                        "}";

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            GLES20.glDeleteProgram(program);
            throw new RuntimeException("Error linking model shader program.");
        }

        return program;
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }
}