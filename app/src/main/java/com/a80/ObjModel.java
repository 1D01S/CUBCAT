package com.a80;

import android.opengl.GLES20;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class ObjModel {
    private FloatBuffer vertexBuffer;
    private int vertexCount;
    private int shaderProgram;
    private int positionHandle, mvpMatrixHandle;

    public ObjModel(InputStream objInputStream) {
        loadObjModel(objInputStream);
    }

    // Отдельная инициализация шейдеров, которую мы будем вызывать только когда контекст OpenGL доступен
    public void initShaders() {
        String vertexShaderCode =
                "uniform mat4 uMVPMatrix;" +
                        "attribute vec4 vPosition;" +
                        "void main() {" +
                        "  gl_Position = uMVPMatrix * vPosition;" +
                        "}";

        String fragmentShaderCode =
                "precision mediump float;" +
                        "void main() {" +
                        "  gl_FragColor = vec4(0.5, 0.5, 0.5, 1.0);" + // Цвет серый для модели
                        "}";

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShader);
        GLES20.glAttachShader(shaderProgram, fragmentShader);
        GLES20.glLinkProgram(shaderProgram);

        positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");
    }

    private void loadObjModel(InputStream objInputStream) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(objInputStream));
            List<Float> vertices = new ArrayList<>();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("v ")) {
                    String[] tokens = line.split("\\s+");
                    vertices.add(Float.parseFloat(tokens[1]));
                    vertices.add(Float.parseFloat(tokens[2]));
                    vertices.add(Float.parseFloat(tokens[3]));
                }
            }

            vertexCount = vertices.size() / 3;

            // Создаем прямой буфер вершин с нативным порядком байтов
            ByteBuffer buffer = ByteBuffer.allocateDirect(vertices.size() * 4);
            buffer.order(ByteOrder.nativeOrder());
            vertexBuffer = buffer.asFloatBuffer();
            for (Float v : vertices) {
                vertexBuffer.put(v);
            }
            vertexBuffer.position(0);

        } catch (Exception e) {
            Log.e("ObjModel", "Error loading OBJ model", e);
        }
    }

    public void draw(float[] mvpMatrix) {
        GLES20.glUseProgram(shaderProgram);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e("ObjModel", "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            throw new RuntimeException("Error compiling shader.");
        }
        return shader;
    }
}
