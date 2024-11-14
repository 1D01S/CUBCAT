package com.a80;

import android.opengl.GLES20;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

public class ObjModel {
    private static final String TAG = "ObjModel";

    private FloatBuffer vertexBuffer;
    private ShortBuffer indexBuffer;
    private int indexCount;
    private int shaderProgram;
    private int positionHandle, mvpMatrixHandle;

    private static final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "}";

    private static final String fragmentShaderCode =
            "precision mediump float;" +
                    "void main() {" +
                    "  gl_FragColor = vec4(0.5, 0.5, 0.5, 1.0);" +
                    "}";

    public ObjModel(InputStream objInputStream, int shaderProgram) {
        this.shaderProgram = shaderProgram;
        loadObjModel(objInputStream);
        initShaderHandles();
    }

    private void initShaderHandles() {
        GLES20.glUseProgram(shaderProgram);
        positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");

        if (positionHandle == -1 || mvpMatrixHandle == -1) {
            Log.e(TAG, "Error locating shader attributes or uniforms.");
        }
    }

    private void loadObjModel(InputStream objInputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(objInputStream))) {
            List<Float> vertices = new ArrayList<>();
            List<Short> indices = new ArrayList<>();

            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\\s+");
                switch (tokens[0]) {
                    case "v": // Vertex positions
                        vertices.add(Float.parseFloat(tokens[1]));
                        vertices.add(Float.parseFloat(tokens[2]));
                        vertices.add(Float.parseFloat(tokens[3]));
                        break;
                    case "f": // Faces
                        // Note: Parsing faces with the format v/vt/vn
                        for (int i = 1; i <= 3; i++) {
                            String[] parts = tokens[i].split("/");
                            int vertexIndex = Integer.parseInt(parts[0]) - 1;
                            indices.add((short) vertexIndex);
                        }
                        break;
                }
            }

            indexCount = indices.size();

            // Vertex buffer
            vertexBuffer = ByteBuffer.allocateDirect(vertices.size() * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            vertexBuffer.put(toFloatArray(vertices));
            vertexBuffer.position(0);

            // Index buffer
            indexBuffer = ByteBuffer.allocateDirect(indices.size() * 2)
                    .order(ByteOrder.nativeOrder())
                    .asShortBuffer();
            indexBuffer.put(toShortArray(indices));
            indexBuffer.position(0);

            // Отладочный вывод для проверки вершин и индексов
            Log.d(TAG, "Loaded vertices: " + vertices.size() / 3);
            Log.d(TAG, "Loaded indices: " + indices.size() / 3);

        } catch (Exception e) {
            Log.e(TAG, "Error loading OBJ model", e);
        }
    }

    public void draw(float[] mvpMatrix) {
        if (shaderProgram == 0) {
            Log.e(TAG, "Shader program is not initialized.");
            return;
        }

        GLES20.glUseProgram(shaderProgram);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    public void destroy() {
        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
            Log.d(TAG, "Shader program deleted.");
        }
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
