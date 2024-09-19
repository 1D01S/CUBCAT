package com.a80;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class MainActivity extends AppCompatActivity {
    private GLSurfaceView glSurfaceView;
    private TouchHandler touchHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(new CubeRenderer(this));

        touchHandler = new TouchHandler(this);
        glSurfaceView.setOnTouchListener((v, event) -> {
            touchHandler.onTouchEvent(event);
            return true;
        });

        setContentView(glSurfaceView);
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

    private class CubeRenderer implements GLSurfaceView.Renderer {
        private Cube mCube;
        private float[] projectionMatrix = new float[16]; // Матрица проекции
        private float angleX = 0; // Угол вращения по оси X
        private float angleY = 0; // Угол вращения по оси Y
        private float angleSpeedX = -0.13f; // Скорость вращения по оси X
        private float angleSpeedY = 0.35f; // Скорость вращения по оси Y

        public CubeRenderer(Context context) {
            mCube = new Cube();
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            angleX += angleSpeedX; // Увеличиваем угол поворота по оси X
            angleY += angleSpeedY; // Увеличиваем угол поворота по оси Y

            // Передаем углы и матрицу проекции для отрисовки
            mCube.draw(angleX, angleY, projectionMatrix);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            float aspectRatio = (float) width / (float) height;
            Matrix.orthoM(projectionMatrix, 0, -5f * aspectRatio, 5f * aspectRatio, -5f, 5f, -10f, 10f);
        }
    }

    private class Cube {
        private float[] vertices;
        private FloatBuffer vertexBuffer;

        public Cube() {
            setSize(1.0f); // Установка размера куба
        }

        public void setSize(float size) {
            vertices = new float[]{
                    // Передняя грань
                    -1, -1,  1,  // Вершина 0
                    1, -1,  1,   // Вершина 1
                    1,  1,  1,   // Вершина 2
                    -1,  1,  1,  // Вершина 3
                    // Задняя грань
                    -1, -1, -1,   // Вершина 4
                    -1,  1, -1,   // Вершина 5
                    1,  1, -1,    // Вершина 6
                    1, -1, -1,     // Вершина 7
                    // Левые грани
                    -1, -1, -1,
                    -1, -1,  1,
                    -1,  1,  1,
                    -1,  1, -1,
                    // Правые грани
                    1, -1, -1,
                    1, -1,  1,
                    1,  1,  1,
                    1,  1, -1,
                    // Верхняя грань
                    -1,  1, -1,
                    -1,  1,  1,
                    1,  1,  1,
                    1,  1, -1,
                    // Нижняя грань
                    -1, -1, -1,
                    1, -1, -1,
                    1, -1,  1,
                    -1, -1,  1
            };

            ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
            bb.order(ByteOrder.nativeOrder());
            vertexBuffer = bb.asFloatBuffer();
            vertexBuffer.put(vertices);
            vertexBuffer.position(0);
        }

        public void draw(float angleX, float angleY, float[] projectionMatrix) {
            float[] rotationMatrixX = new float[16];
            float[] rotationMatrixY = new float[16];
            float[] mvpMatrix = new float[16];

            Matrix.setIdentityM(rotationMatrixX, 0);
            Matrix.setIdentityM(rotationMatrixY, 0);
            Matrix.setRotateM(rotationMatrixX, 0, angleX, 1, 0, 0);
            Matrix.setRotateM(rotationMatrixY, 0, angleY, 0, 1, 0);

            float[] tempMatrix = new float[16];
            Matrix.multiplyMM(tempMatrix, 0, rotationMatrixY, 0, rotationMatrixX, 0);
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);

            // Рисуем грани куба
            drawCube(mvpMatrix);

            // Отключаем тест глубины, чтобы точка была видна
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);

            // Рисуем центровую точку
            drawCenterPoint(mvpMatrix);

            // Включаем тест глубины обратно
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);

            // Отключаем тест глубины, чтобы линии были видны поверх
            drawEdges(mvpMatrix);
        }


        private void drawCenterPoint(float[] mvpMatrix) {
            String vertexShaderCode =
                    "uniform mat4 uMVPMatrix;" +
                            "attribute vec4 vPosition;" +
                            "void main() { " +
                            "   gl_Position = uMVPMatrix * vPosition;" +
                            "   gl_PointSize = 15.0; " + // Устанавливаем размер точки здесь
                            "}";

            String fragmentShaderCode =
                    "precision mediump float;" +
                            "void main() { gl_FragColor = vec4(1.0, 0.3529, 0.2784, 1.0); }"; // Цвет #FF5A47


            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

            int program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, fragmentShader);
            GLES20.glLinkProgram(program);

            GLES20.glUseProgram(program);
            int positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
            int mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");

            // Создание буфера для рисования точки
            float[] pointVertices = {
                    0, 0, 0, // Центр куба
            };

            FloatBuffer pointBuffer = ByteBuffer.allocateDirect(pointVertices.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(pointVertices);
            pointBuffer.position(0);

            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, pointBuffer);
            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1); // Рисуем точку

            GLES20.glDisableVertexAttribArray(positionHandle);
        }


        private void drawCube(float[] mvpMatrix) {
            String vertexShaderCode =
                    "uniform mat4 uMVPMatrix;" +
                            "attribute vec4 vPosition;" +
                            "attribute vec4 vColor;" +
                            "varying vec4 vColorOut;" +
                            "void main() { " +
                            "   gl_Position = uMVPMatrix * vPosition; " +
                            "   vColorOut = vColor; " +
                            "}";

            String fragmentShaderCode =
                    "precision mediump float;" +
                            "varying vec4 vColorOut;" +
                            "void main() {" +
                            "   gl_FragColor = vColorOut;" +
                            "}";

            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

            int program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, fragmentShader);
            GLES20.glLinkProgram(program);

            GLES20.glUseProgram(program);
            int positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
            int colorHandle = GLES20.glGetAttribLocation(program, "vColor");
            int mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");

            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

            // Определяем цвета для каждой грани с градиентом
            float[] colors = {
                    // Передняя грань (градиент от белого к серому)
                    1.0f, 1.0f, 1.0f, // Вершина 0
                    1.0f, 0.75f, 0.75f, // Вершина 1
                    0.75f, 0.75f, 1.0f, // Вершина 2
                    1.0f, 1.0f, 1.0f, // Вершина 3
                    // Задняя грань (градиент от темного к серому)
                    0.25f, 0.25f, 0.25f, // Вершина 4
                    0.5f, 0.5f, 0.5f, // Вершина 5
                    0.5f, 0.5f, 0.5f, // Вершина 6
                    0.25f, 0.25f, 0.25f, // Вершина 7
                    // Левые грани (темный градиент)
                    0.5f, 0.5f, 0.5f,
                    0.75f, 0.75f, 0.75f,
                    0.5f, 0.5f, 0.5f,
                    0.25f, 0.25f, 0.25f,
                    // Правые грани (светлый градиент)
                    1.0f, 1.0f, 1.0f,
                    0.75f, 0.5f, 0.5f,
                    1.0f, 1.0f, 1.0f,
                    0.75f, 0.75f, 0.75f,
                    // Верхняя грань (градиент от светлого к темному)
                    1.0f, 1.0f, 1.0f,
                    0.75f, 0.75f, 0.75f,
                    0.5f, 0.5f, 0.5f,
                    0.25f, 0.25f, 0.25f,
                    // Нижняя грань (градиент от темного к светлому)
                    0.25f, 0.25f, 0.25f,
                    0.5f, 0.5f, 0.5f,
                    0.75f, 0.75f, 0.75f,
                    1.0f, 1.0f, 1.0f
            };

            // Установка цвета
            ByteBuffer colorBuffer = ByteBuffer.allocateDirect(colors.length * 4);
            colorBuffer.order(ByteOrder.nativeOrder());
            colorBuffer.asFloatBuffer().put(colors);
            colorBuffer.position(0);

            GLES20.glVertexAttribPointer(colorHandle, 3, GLES20.GL_FLOAT, false, 0, colorBuffer);
            GLES20.glEnableVertexAttribArray(colorHandle);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4); // Передняя грань
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 4, 4); // Задняя грань
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 8, 4); // Левые грани
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 12, 4); // Правые грани
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 16, 4); // Верхняя грань
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 20, 4); // Нижняя грань

            GLES20.glDisableVertexAttribArray(positionHandle);
            GLES20.glDisableVertexAttribArray(colorHandle);
        }

        private void drawEdges(float[] mvpMatrix) {
            String vertexShaderCode =
                    "uniform mat4 uMVPMatrix;" +
                            "attribute vec4 vPosition;" +
                            "void main() { gl_Position = uMVPMatrix * vPosition; }";

            String fragmentShaderCode =
                    "precision mediump float;" +
                            "void main() { gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0); }"; // Ярко-белый цвет для углов

            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

            int program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, fragmentShader);
            GLES20.glLinkProgram(program);

            GLES20.glUseProgram(program);
            int positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
            int mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");

            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

            GLES20.glLineWidth(10.0f); // Установка ширины линии

            // Рисуем углы куба
            GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, 4); // Передняя грань
            GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 4, 4); // Задняя грань
            GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 8, 4); // Левые грани
            GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 12, 4); // Правые грани
            GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 16, 4); // Верхняя грань
            GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 20, 4); // Нижняя грань

            GLES20.glDisableVertexAttribArray(positionHandle);
        }

        private int loadShader(int type, String shaderCode) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);
            return shader;
        }
    }

    private class TouchHandler {
        private ScaleGestureDetector scaleGestureDetector;

        public TouchHandler(Context context) {
            scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        }

        public void onTouchEvent(MotionEvent event) {
            scaleGestureDetector.onTouchEvent(event);
        }

        private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                return true;
            }
        }
    }
}




