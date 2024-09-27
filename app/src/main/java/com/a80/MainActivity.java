package com.a80;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_MODEL_REQUEST = 1;
    private GLSurfaceView glSurfaceView;
    private CubeRenderer cubeRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        glSurfaceView = findViewById(R.id.gl_surface_view);
        glSurfaceView.setEGLContextClientVersion(2);
        cubeRenderer = new CubeRenderer(new TouchHandler(this));
        glSurfaceView.setRenderer(cubeRenderer);

        Button loadModelButton = findViewById(R.id.load_model_button);
        loadModelButton.setOnClickListener(v -> openFileChooser());

        Button unloadModelButton = findViewById(R.id.unload_model_button);
        unloadModelButton.setOnClickListener(v -> cubeRenderer.unloadModel());
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/octet-stream"); // Выбираем все типы файлов или только .obj
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select .obj file"), PICK_MODEL_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_MODEL_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                loadModelFromUri(uri);
            }
        }
    }

    private void loadModelFromUri(Uri uri) {
        try {
            InputStream objInputStream = getContentResolver().openInputStream(uri);
            if (objInputStream != null) {
                glSurfaceView.queueEvent(() -> {
                    // Загружаем модель в потоке рендера OpenGL
                    cubeRenderer.loadModel(this, objInputStream);
                    try {
                        objInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
    }

    private static class Cube {
        private FloatBuffer vertexBuffer;
        private FloatBuffer colorBuffer;
        private int shaderProgram;
        private int outlineShaderProgram;
        private int positionHandle, colorHandle, mvpMatrixHandle;
        private int outlinePositionHandle, outlineMvpMatrixHandle;

        // Cube vertices
        private final float[] vertices = {
                -1, -1, 1,  1, -1, 1,  1, 1, 1,  -1, 1, 1,  // Front face
                -1, -1, -1,  -1, 1, -1,  1, 1, -1,  1, -1, -1, // Back face
                -1, -1, -1,  -1, -1, 1,  -1, 1, 1,  -1, 1, -1, // Left face
                1, -1, -1,  1, -1, 1,  1, 1, 1,  1, 1, -1, // Right face
                -1, 1, -1,  -1, 1, 1,  1, 1, 1,  1, 1, -1, // Top face
                -1, -1, -1,  1, -1, -1,  1, -1, 1,  -1, -1, 1 // Bottom face
        };

        // Gradient colors for cube
        private final float[] colors = {
                0.831f, 0.714f, 0.035f, 1.0f,  0.867f, 0.463f, 0.157f, 1.0f,  // Front
                0.867f, 0.463f, 0.157f, 1.0f,  0.831f, 0.714f, 0.035f, 1.0f,
                0.867f, 0.463f, 0.157f, 1.0f,  0.765f, 0.239f, 0.278f, 1.0f,  // Back
                0.765f, 0.239f, 0.278f, 1.0f,  0.867f, 0.463f, 0.157f, 1.0f,
                0.765f, 0.239f, 0.278f, 1.0f,  0.545f, 0.114f, 0.353f, 1.0f,  // Left
                0.545f, 0.114f, 0.353f, 1.0f,  0.765f, 0.239f, 0.278f, 1.0f,
                0.545f, 0.114f, 0.353f, 1.0f,  0.255f, 0.098f, 0.341f, 1.0f,  // Right
                0.255f, 0.098f, 0.341f, 1.0f,  0.545f, 0.114f, 0.353f, 1.0f,
                0.255f, 0.098f, 0.341f, 1.0f,  0.831f, 0.714f, 0.035f, 1.0f,  // Top
                0.831f, 0.714f, 0.035f, 1.0f,  0.255f, 0.098f, 0.341f, 1.0f,
                0.831f, 0.714f, 0.035f, 1.0f,  0.765f, 0.239f, 0.278f, 1.0f,  // Bottom
                0.765f, 0.239f, 0.278f, 1.0f,  0.831f, 0.714f, 0.035f, 1.0f
        };

        public Cube() {
            initBuffers();
            initShaders();
            initOutlineShaders(); // Outline shaders
        }

        private void initBuffers() {
            // Vertex buffer
            ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
            bb.order(ByteOrder.nativeOrder());
            vertexBuffer = bb.asFloatBuffer();
            vertexBuffer.put(vertices);
            vertexBuffer.position(0);

            // Color buffer
            ByteBuffer cb = ByteBuffer.allocateDirect(colors.length * 4);
            cb.order(ByteOrder.nativeOrder());
            colorBuffer = cb.asFloatBuffer();
            colorBuffer.put(colors);
            colorBuffer.position(0);
        }

        private void initShaders() {
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

            // Compile shaders
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

            shaderProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(shaderProgram, vertexShader);
            GLES20.glAttachShader(shaderProgram, fragmentShader);
            GLES20.glLinkProgram(shaderProgram);

            // Get attribute locations
            positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
            colorHandle = GLES20.glGetAttribLocation(shaderProgram, "vColor");
            mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");
        }

        private void initOutlineShaders() {
            String vertexShaderCode =
                    "uniform mat4 uMVPMatrix;" +
                            "attribute vec4 vPosition;" +
                            "void main() {" +
                            "  gl_Position = uMVPMatrix * vPosition;" +
                            "}";

            String fragmentShaderCode =
                    "precision mediump float;" +
                            "void main() {" +
                            "  gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);" + // White outline
                            "}";

            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

            outlineShaderProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(outlineShaderProgram, vertexShader);
            GLES20.glAttachShader(outlineShaderProgram, fragmentShader);
            GLES20.glLinkProgram(outlineShaderProgram);

            outlinePositionHandle = GLES20.glGetAttribLocation(outlineShaderProgram, "vPosition");
            outlineMvpMatrixHandle = GLES20.glGetUniformLocation(outlineShaderProgram, "uMVPMatrix");
        }

        public void draw(float angleX, float angleY, float[] projectionMatrix, float scale) {
            float[] rotationMatrixX = new float[16];
            float[] rotationMatrixY = new float[16];
            float[] modelMatrix = new float[16];
            float[] mvpMatrix = new float[16];

            // Apply rotations
            Matrix.setRotateM(rotationMatrixX, 0, angleX, 1, 0, 0);
            Matrix.setRotateM(rotationMatrixY, 0, angleY, 0, 1, 0);

            Matrix.multiplyMM(modelMatrix, 0, rotationMatrixX, 0, rotationMatrixY, 0);
            Matrix.scaleM(modelMatrix, 0, scale, scale, scale);
            Matrix.translateM(modelMatrix, 0, 1, 1, 0);  // Translate model

            // Multiply projection and model matrices
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelMatrix, 0);

            // Draw cube
            GLES20.glUseProgram(shaderProgram);
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

            GLES20.glEnableVertexAttribArray(colorHandle);
            GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);

            for (int i = 0; i < 6; i++) {
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, i * 4, 4);
            }

            GLES20.glDisableVertexAttribArray(positionHandle);
            GLES20.glDisableVertexAttribArray(colorHandle);

            // Draw outlines
            drawOutline(mvpMatrix);
        }

        private void drawOutline(float[] mvpMatrix) {
            GLES20.glUseProgram(outlineShaderProgram);
            GLES20.glUniformMatrix4fv(outlineMvpMatrixHandle, 1, false, mvpMatrix, 0);

            GLES20.glEnableVertexAttribArray(outlinePositionHandle);
            GLES20.glVertexAttribPointer(outlinePositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

            GLES20.glLineWidth(5.0f);  // Set line width for outlines

            // Draw outlines using GL_LINE_LOOP
            for (int i = 0; i < 6; i++) {
                GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, i * 4, 4);
            }

            GLES20.glDisableVertexAttribArray(outlinePositionHandle);
        }

        private int loadShader(int type, String shaderCode) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);

            // Check for compile errors
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e("Cube", "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                throw new RuntimeException("Error compiling shader");
            }

            return shader;
        }
    }

    private static class CubeRenderer implements GLSurfaceView.Renderer {
        private Cube mCube;
        private ObjModel objModel;
        private boolean isModelLoaded = false; // Флаг для проверки, загружена ли модель
        private final float[] projectionMatrix = new float[16];
        private float angleX = 0;
        private float angleY = 0;
        private final TouchHandler touchHandler;

        public CubeRenderer(TouchHandler touchHandler) {
            this.touchHandler = touchHandler;
        }

        public void loadModel(Context context, InputStream objInputStream) {
            objModel = new ObjModel(objInputStream);
            isModelLoaded = true; // Установить флаг в true
        }

        public void unloadModel() {
            isModelLoaded = false; // Сбросить флаг в false
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

            // Инициализация куба
            mCube = new Cube();

            // Инициализация шейдеров модели
            if (isModelLoaded && objModel != null) {
                objModel.initShaders();
            }
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            angleX -= 0.13f;
            angleY += 0.35f;

            float scale = touchHandler.getScale();

            if (isModelLoaded && objModel != null) {
                objModel.draw(projectionMatrix); // Отрисовка загруженной модели
            } else {
                mCube.draw(angleX, angleY, projectionMatrix, scale); // Отрисовка куба
            }
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            float aspectRatio = (float) width / height;
            Matrix.orthoM(projectionMatrix, 0, -5f * aspectRatio, 5f * aspectRatio, -5f, 5f, -10f, 10f);
        }
    }

    private static class TouchHandler {
        private final ScaleGestureDetector scaleGestureDetector;
        private float scale = 1.0f;

        public TouchHandler(Context context) {
            scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        }

        public void onTouchEvent(MotionEvent event) {
            scaleGestureDetector.onTouchEvent(event);
        }

        public float getScale() {
            return scale;
        }

        private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
                scale *= detector.getScaleFactor();
                scale = Math.max(0.1f, Math.min(scale, 5.0f));  // Ограничиваем масштаб
                return true;
            }
        }
    }
}

