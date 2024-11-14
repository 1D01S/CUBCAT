package com.a80;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Cube {
    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private int shaderProgram; // Шейдерная программа только для куба
    private int positionHandle, colorHandle, mvpMatrixHandle;

    private final float[] vertices = {
            -1, -1, 1,  1, -1, 1,  1, 1, 1,  -1, 1, 1,
            -1, -1, -1,  -1, 1, -1,  1, 1, -1,  1, -1, -1,
            -1, -1, -1,  -1, -1, 1,  -1, 1, 1,  -1, 1, -1,
            1, -1, -1,  1, -1, 1,  1, 1, 1,  1, 1, -1,
            -1, 1, -1,  -1, 1, 1,  1, 1, 1,  1, 1, -1,
            -1, -1, -1,  1, -1, -1,  1, -1, 1,  -1, -1, 1
    };

    private final float[] colors = {
            0.831f, 0.714f, 0.035f, 1.0f,  0.867f, 0.463f, 0.157f, 1.0f,
            0.867f, 0.463f, 0.157f, 1.0f,  0.831f, 0.714f, 0.035f, 1.0f,
            0.765f, 0.239f, 0.278f, 1.0f,  0.545f, 0.114f, 0.353f, 1.0f,
            0.255f, 0.098f, 0.341f, 1.0f,  0.831f, 0.714f, 0.035f, 1.0f,
            0.765f, 0.239f, 0.278f, 1.0f,  0.831f, 0.714f, 0.035f, 1.0f
    };

    public Cube(int shaderProgram) {
        this.shaderProgram = shaderProgram;
        initBuffers();
        initShaderHandles();
    }

    private void initBuffers() {
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        ByteBuffer cb = ByteBuffer.allocateDirect(colors.length * 4);
        cb.order(ByteOrder.nativeOrder());
        colorBuffer = cb.asFloatBuffer();
        colorBuffer.put(colors);
        colorBuffer.position(0);
    }

    private void initShaderHandles() {
        positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        colorHandle = GLES20.glGetAttribLocation(shaderProgram, "vColor");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");
    }

    public void draw(float[] mvpMatrix) {
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
    }
}